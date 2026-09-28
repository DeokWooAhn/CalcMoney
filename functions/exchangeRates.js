// ExchangeRate-API 응답을 Firestore `exchangeRates/latest` 문서 형식으로 바꾸는 순수 로직.
// Firebase 의존성이 없어서 로컬에서 node 로 바로 검증할 수 있다.

const EXCHANGE_RATE_API_BASE_URL = "https://v6.exchangerate-api.com/v6";
const BASE_CURRENCY = "KRW";
const KOREA_TIME_ZONE = "Asia/Seoul";

const koreanCurrencyNames = new Intl.DisplayNames(["ko"], { type: "currency", fallback: "none" });

// ISO 4217 에 없는 지역 통화라 Intl 이 이름을 모르는 코드들.
const FALLBACK_KOREAN_CURRENCY_NAMES = {
  FOK: "페로 제도 크로나",
  GGP: "건지 파운드",
  IMP: "맨섬 파운드",
  JEP: "저지 파운드",
  KID: "키리바시 달러",
  TVD: "투발루 달러",
};

function exchangeRateApiUrl(apiKey) {
  return `${EXCHANGE_RATE_API_BASE_URL}/${encodeURIComponent(apiKey)}/latest/${BASE_CURRENCY}`;
}

/**
 * ExchangeRate-API 응답을 검증하고 Firestore 에 쓸 값으로 변환한다.
 *
 * 응답의 `conversion_rates` 는 "1원당 외화" 이므로, 앱이 기대하는
 * "외화 1단위당 원"(기존 수출입은행 매매기준율과 같은 방향)으로 뒤집어 저장한다.
 */
function parseExchangeRateApiResponse(body) {
  if (!body || typeof body !== "object") {
    throw new Error("ExchangeRate-API returned malformed data.");
  }

  if (body.result !== "success") {
    throw new Error(apiErrorMessage(body["error-type"]));
  }

  if (body.base_code !== BASE_CURRENCY) {
    throw new Error(`ExchangeRate-API returned unexpected base: ${safeProviderToken(body.base_code)}`);
  }

  const rates = buildRates(body.conversion_rates);
  if (rates.length === 0) {
    throw new Error("ExchangeRate-API returned no data.");
  }

  const updatedAtUnix = Number(body.time_last_update_unix);
  if (!Number.isFinite(updatedAtUnix) || updatedAtUnix <= 0) {
    throw new Error("ExchangeRate-API returned no update time.");
  }

  return {
    rateDate: formatKoreaBasicDate(new Date(updatedAtUnix * 1000)),
    rates,
  };
}

function buildRates(conversionRates) {
  if (!conversionRates || typeof conversionRates !== "object") {
    return [];
  }

  const rates = Object.entries(conversionRates)
    .map(([code, rate]) => normalizeRate(code, rate))
    .filter(Boolean);

  return sortByCode(rates);
}

function normalizeRate(rawCode, rawRate) {
  const code = String(rawCode).trim().toUpperCase();
  const perKrw = Number(rawRate);

  if (!/^[A-Z]{3}$/.test(code) || code === BASE_CURRENCY) {
    return null;
  }

  // 0 이나 음수로 나누면 Infinity/음수 환율이 앱까지 흘러간다.
  if (!Number.isFinite(perKrw) || perKrw <= 0) {
    return null;
  }

  return {
    code,
    currencyUnit: code,
    currencyName: koreanCurrencyNames.of(code) ?? FALLBACK_KOREAN_CURRENCY_NAMES[code] ?? code,
    baseRate: 1 / perKrw,
  };
}

// 구버전 앱은 서버가 준 순서를 그대로 보여주므로, 예전 수출입은행 응답처럼 통화 코드 알파벳순으로 맞춘다.
// 새 버전 앱은 통화 선택 화면에서 직접 정렬한다.
function sortByCode(rates) {
  // 코드는 대문자 ASCII 세 글자라 로케일 비교 없이 단순 비교로 충분하다.
  return [...rates].sort((left, right) => (left.code < right.code ? -1 : Number(left.code > right.code)));
}

// 오류 메시지는 공개 읽기 문서인 lastError 에 그대로 저장된다.
// API 가 준 값은 우리가 통제할 수 없으므로, 짧은 코드 형식일 때만 남기고 나머지는 고정 문구로 바꾼다.
function safeProviderToken(value) {
  return typeof value === "string" && /^[A-Za-z-]{1,40}$/.test(value) ? value : "unrecognized";
}

function apiErrorMessage(errorType) {
  switch (errorType) {
    case "invalid-key":
      return "Invalid ExchangeRate-API key. (invalid-key)";
    case "inactive-account":
      return "ExchangeRate-API account is inactive. (inactive-account)";
    case "quota-reached":
      return "ExchangeRate-API request quota reached. (quota-reached)";
    case "unsupported-code":
      return "ExchangeRate-API does not support the base currency. (unsupported-code)";
    case "malformed-request":
      return "Malformed ExchangeRate-API request. (malformed-request)";
    default:
      return `ExchangeRate-API request failed. (${safeProviderToken(errorType)})`;
  }
}

function formatKoreaBasicDate(date) {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: KOREA_TIME_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(date);

  const part = (type) => parts.find((item) => item.type === type).value;

  return `${part("year")}${part("month")}${part("day")}`;
}

module.exports = {
  KOREA_TIME_ZONE,
  exchangeRateApiUrl,
  parseExchangeRateApiResponse,
};
