const admin = require("firebase-admin");
const { logger } = require("firebase-functions");
const { defineSecret } = require("firebase-functions/params");
const { onSchedule } = require("firebase-functions/v2/scheduler");

admin.initializeApp();

const koreaEximApiKey = defineSecret("KOREA_EXIM_API_KEY");

const db = admin.firestore();
const latestExchangeRateRef = db.collection("exchangeRates").doc("latest");

const KOREA_EXIM_EXCHANGE_URL =
  "https://oapi.koreaexim.go.kr/site/program/financial/exchangeJSON";
const KOREA_TIME_ZONE = "Asia/Seoul";
const MAX_FALLBACK_DAYS = 7;

exports.syncExchangeRates = onSchedule(
  {
    region: "asia-northeast3",
    schedule: "30 9,11 * * *",
    timeZone: KOREA_TIME_ZONE,
    secrets: [koreaEximApiKey],
  },
  async () => {
    const fetchedAt = Date.now();

    try {
      const { rateDate, rates } = await fetchValidExchangeRates(koreaEximApiKey.value());

      await latestExchangeRateRef.set({
        rateDate,
        fetchedAt,
        source: "KOREA_EXIM",
        sourceName: "한국수출입은행 환율 정보 Open API",
        status: "FRESH",
        message: null,
        rates,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      logger.info("Exchange rates synced", {
        rateDate,
        rateCount: rates.length,
      });
    } catch (error) {
      await keepPreviousRatesAsStale(fetchedAt, error);
    }
  },
);

async function fetchValidExchangeRates(authKey) {
  for (const searchDate of exchangeRateSearchDates()) {
    const rows = await fetchExchangeRates(authKey, searchDate);
    const resultCode = rows[0]?.result;

    if ([2, 3, 4].includes(resultCode)) {
      throw new Error(apiErrorMessage(resultCode));
    }

    const rates = rows.map(normalizeExchangeRate).filter(Boolean);
    if (rates.length > 0) {
      return {
        rateDate: searchDate || todayInKorea(),
        rates,
      };
    }
  }

  throw new Error("오늘 환율이 아직 고시되지 않았습니다.");
}

async function fetchExchangeRates(authKey, searchDate) {
  const params = new URLSearchParams({
    authkey: authKey,
    searchdate: searchDate,
    data: "AP01",
  });

  const response = await fetch(`${KOREA_EXIM_EXCHANGE_URL}?${params}`);
  if (!response.ok) {
    throw new Error(`Korea Exim API request failed: ${response.status}`);
  }

  const body = await response.json();
  if (!Array.isArray(body)) {
    throw new Error("Korea Exim API returned malformed data.");
  }

  return body;
}

function normalizeExchangeRate(row) {
  if (row.result !== 1 || !row.cur_unit || !row.deal_bas_r) {
    return null;
  }

  const currencyUnit = row.cur_unit.trim();
  const code = currencyUnit.replace("(100)", "").trim();
  const baseRate = Number(row.deal_bas_r.replace(/,/g, ""));
  if (!code || Number.isNaN(baseRate)) {
    return null;
  }

  return {
    code,
    currencyUnit,
    currencyName: row.cur_nm || "Unknown",
    baseRate: currencyUnit.includes("(100)") ? baseRate / 100 : baseRate,
  };
}

async function keepPreviousRatesAsStale(fetchedAt, error) {
  logger.error("Exchange rate sync failed", error);

  const latest = await latestExchangeRateRef.get();
  const previousRates = latest.get("rates");
  if (latest.exists && Array.isArray(previousRates) && previousRates.length > 0) {
    await latestExchangeRateRef.set(
      {
        fetchedAt,
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
      fetchedAt,
      source: "KOREA_EXIM",
      sourceName: "한국수출입은행 환율 정보 Open API",
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

function apiErrorMessage(resultCode) {
  switch (resultCode) {
    case 2:
      return "Invalid exchange rate API request. (result=2)";
    case 3:
      return "Invalid exchange rate API authKey. (result=3)";
    case 4:
      return "Exchange rate API daily request limit exceeded. (result=4)";
    default:
      return "Exchange rate API returned no data.";
  }
}

function exchangeRateSearchDates() {
  const previousBusinessDates = [];
  const cursor = koreaLocalDate();
  cursor.setUTCDate(cursor.getUTCDate() - 1);

  while (previousBusinessDates.length < MAX_FALLBACK_DAYS) {
    const day = cursor.getUTCDay();
    if (day !== 0 && day !== 6) {
      previousBusinessDates.push(formatBasicDate(cursor));
    }
    cursor.setUTCDate(cursor.getUTCDate() - 1);
  }

  return ["", ...previousBusinessDates];
}

function todayInKorea() {
  return formatBasicDate(koreaLocalDate());
}

function koreaLocalDate() {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: KOREA_TIME_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(new Date());

  const year = Number(parts.find((part) => part.type === "year").value);
  const month = Number(parts.find((part) => part.type === "month").value);
  const day = Number(parts.find((part) => part.type === "day").value);

  return new Date(Date.UTC(year, month - 1, day));
}

function formatBasicDate(date) {
  const year = date.getUTCFullYear();
  const month = String(date.getUTCMonth() + 1).padStart(2, "0");
  const day = String(date.getUTCDate()).padStart(2, "0");

  return `${year}${month}${day}`;
}
