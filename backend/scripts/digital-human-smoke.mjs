#!/usr/bin/env node

const baseUrl = normalizeBaseUrl(process.argv[2] || process.env.ANYI_API_BASE_URL || "https://api.anyibj.cn");
const apkPath = process.env.ANYI_APK_PATH || "/downloads/anyi-memorial-latest.apk";
const checks = [];

async function main() {
  const health = await expectJson(`${baseUrl}/health`, "API health");
  expect(health.ok === true, "API health returned ok=true");

  const config = await expectJson(`${baseUrl}/app/config`, "App config");
  const digitalHuman = config.digitalHuman || {};
  expect(digitalHuman.enabled === true, "Digital human is enabled");
  expect(isHttpUrl(digitalHuman.url), `Digital human URL is valid: ${digitalHuman.url || "(missing)"}`);
  expect(isHttpUrl(digitalHuman.statusUrl), `Digital human status URL is valid: ${digitalHuman.statusUrl || "(missing)"}`);

  const status = await expectJson(digitalHuman.statusUrl, "Digital human status");
  expect(status.ok === true, "Digital human status returned ok=true");
  expect(status.checks?.page?.status === 200, "Digital human page check returned HTTP 200");

  await expectHeadOrGet(digitalHuman.url, "Digital human page", {
    status: 200,
    contentTypeIncludes: "text/html"
  });

  await expectHeadOrGet(`${baseUrl}${apkPath}`, "Latest APK download", {
    status: 200,
    contentTypeIncludes: "application/vnd.android.package-archive",
    minContentLength: 1024 * 1024
  });

  checks.forEach((message) => console.log(`PASS ${message}`));
}

async function expectJson(url, label) {
  const response = await fetchWithTimeout(url, { headers: { Accept: "application/json" } });
  expect(response.ok, `${label} HTTP ${response.status}`);
  const contentType = response.headers.get("content-type") || "";
  expect(contentType.includes("application/json"), `${label} returned JSON`);
  return response.json();
}

async function expectHeadOrGet(url, label, options) {
  let response = await fetchWithTimeout(url, { method: "HEAD" });
  if (response.status === 405 || response.status === 501) {
    response = await fetchWithTimeout(url, { method: "GET" });
  }
  expect(response.status === options.status, `${label} HTTP ${options.status}`);
  const contentType = response.headers.get("content-type") || "";
  expect(contentType.includes(options.contentTypeIncludes), `${label} content-type includes ${options.contentTypeIncludes}`);
  if (options.minContentLength) {
    const contentLength = Number(response.headers.get("content-length") || "0");
    expect(contentLength >= options.minContentLength, `${label} content-length >= ${options.minContentLength}`);
  }
}

async function fetchWithTimeout(url, options = {}) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 12000);
  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } finally {
    clearTimeout(timeout);
  }
}

function expect(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
  checks.push(message);
}

function normalizeBaseUrl(value) {
  return value.trim().replace(/\/+$/g, "");
}

function isHttpUrl(value) {
  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:";
  } catch {
    return false;
  }
}

main().catch((error) => {
  console.error(`FAIL ${error instanceof Error ? error.message : String(error)}`);
  process.exit(1);
});
