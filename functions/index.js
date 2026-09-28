const admin = require("firebase-admin");
const { logger } = require("firebase-functions");
const { defineSecret } = require("firebase-functions/params");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const {
  KOREA_TIME_ZONE,
  exchangeRateApiUrl,
  parseExchangeRateApiResponse,
} = require("./exchangeRates");

admin.initializeApp();

const exchangeRateApiKey = defineSecret("EXCHANGE_RATE_API_KEY");

const db = admin.firestore();
const latestExchangeRateRef = db.collection("exchangeRates").doc("latest");

const SOURCE = "EXCHANGE_RATE_API";
const SOURCE_NAME = "ExchangeRate-API";

// ExchangeRate-API 무료 플랜은 하루 한 번(00:00 UTC = 09:00 KST 전후) 갱신된다.
// 갱신 직후를 피해 11:10 에 받고, 실패하면 12:30 에 한 번 더 시도한다.
exports.syncExchangeRates = onSchedule(
  {
    region: "asia-northeast3",
    schedule: "10 11 * * *",
    timeZone: KOREA_TIME_ZONE,
    secrets: [exchangeRateApiKey],
  },
  syncExchangeRates,
);

exports.retrySyncExchangeRates = onSchedule(
  {
    region: "asia-northeast3",
    schedule: "30 12 * * *",
    timeZone: KOREA_TIME_ZONE,
    secrets: [exchangeRateApiKey],
  },
  syncExchangeRates,
);

async function syncExchangeRates() {
  const checkedAt = Date.now();

  try {
    const { rateDate, rates } = await fetchExchangeRates(exchangeRateApiKey.value());

    await latestExchangeRateRef.set({
      rateDate,
      fetchedAt: checkedAt,
      rateFetchedAt: checkedAt,
      lastCheckedAt: checkedAt,
      source: SOURCE,
      sourceName: SOURCE_NAME,
      status: "FRESH",
      message: null,
      lastError: null,
      rates,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    logger.info("Exchange rates synced", {
      rateDate,
      rateCount: rates.length,
    });
  } catch (error) {
    await keepPreviousRatesAsStale(checkedAt, error);
  }
}

async function fetchExchangeRates(apiKey) {
  let response;
  try {
    response = await fetch(exchangeRateApiUrl(apiKey));
  } catch (error) {
    // 요청 URL 경로에 API 키가 들어 있다. 원본 오류를 그대로 남기면 키가
    // 공개 읽기 문서인 lastError 로 새어 나갈 수 있어 메시지를 새로 만든다.
    throw new Error(`ExchangeRate-API network error: ${error.name}`);
  }

  // 키가 틀리거나 한도를 넘으면 4xx 와 함께 error-type 이 담긴 JSON 을 준다.
  // 그 사유를 살리기 위해 상태 코드보다 본문을 먼저 해석한다.
  let body;
  try {
    body = await response.json();
  } catch {
    throw new Error(`ExchangeRate-API request failed: ${response.status}`);
  }

  return parseExchangeRateApiResponse(body);
}

async function keepPreviousRatesAsStale(checkedAt, error) {
  logger.error("Exchange rate sync failed", { message: error.message });

  const latest = await latestExchangeRateRef.get();
  const previousRates = latest.get("rates");
  if (latest.exists && Array.isArray(previousRates) && previousRates.length > 0) {
    await latestExchangeRateRef.set(
      {
        lastCheckedAt: checkedAt,
        status: "STALE",
        message: "오늘 환율 갱신에 실패하여 이전 환율을 사용합니다.",
        lastError: error.message,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true },
    );
    return;
  }

  await latestExchangeRateRef.set(
    {
      rateDate: "",
      fetchedAt: 0,
      rateFetchedAt: 0,
      lastCheckedAt: checkedAt,
      source: SOURCE,
      sourceName: SOURCE_NAME,
      status: "ERROR",
      message: "환율 정보를 준비하지 못했습니다.",
      lastError: error.message,
      rates: [],
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    },
    { merge: true },
  );

  throw error;
}
