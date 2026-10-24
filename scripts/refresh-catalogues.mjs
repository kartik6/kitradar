import { setDefaultResultOrder } from "node:dns";
import { appendFile } from "node:fs/promises";
import { pathToFileURL } from "node:url";

// Some hosted runners have no reliable IPv6 egress while a few source domains
// publish AAAA records. Prefer IPv4 rather than failing before HTTP begins.
setDefaultResultOrder("ipv4first");

const SOURCE_REQUEST_TIMEOUT_MS = 20_000;
const sourceHeaders = { "User-Agent": "KitPagluBot/1.0 (+https://kitpaglu.vercel.app)" };
let apiBase;
let commonHeaders;

export async function main() {
  apiBase = required("KITRADAR_API_URL").replace(/\/$/, "") + "/api/v1/admin";
  commonHeaders = { ...sourceHeaders, Authorization: `Bearer ${required("ADMIN_TOKEN")}` };
  const importTimeoutMs = positiveIntegerEnv("KITRADAR_IMPORT_TIMEOUT_MS", 120_000);
  const requestedStore = process.env.STORE?.trim();
  const sources = await request(`${apiBase}/sources`, { headers: commonHeaders, retryNetwork: true });
  if (!Array.isArray(sources) || sources.length === 0) throw new Error("No enabled catalogue sources returned");
  const selectedSources = requestedStore ? sources.filter((source) => source.slug === requestedStore) : sources;
  if (selectedSources.length === 0) throw new Error(`No enabled store found for slug ${requestedStore}`);

  const outcomes = [];
  let importFailed = false;
  for (const source of selectedSources) {
    let catalogueFetched = false;
    const startedAt = Date.now();
    try {
      console.log(`▶ START ${sourceLabel(source)} — ${source.platform}`);
      const catalogue = await fetchProducts(source);
      if (catalogue.products.length === 0) {
        throw new Error(`No usable products fetched; preserving existing catalogue. ${catalogue.issues.join("; ")}`);
      }
      catalogueFetched = true;
      // A separate endpoint fails closed against older backends that cannot protect partial imports.
      const endpoint = catalogue.complete ? "raw" : "raw/partial";
      console.log(`↥ IMPORT ${sourceLabel(source)} — ${catalogue.products.length} products; complete=${catalogue.complete}`);
      const result = await request(`${apiBase}/imports/${endpoint}?store=${encodeURIComponent(source.slug)}`, {
        method: "POST",
        headers: { ...commonHeaders, "Content-Type": "application/json" },
        body: JSON.stringify({ products: catalogue.products }),
        timeoutMs: importTimeoutMs,
      });
      if (!["SUCCESS", "PARTIAL"].includes(result.status)
          || (!catalogue.complete && (result.status !== "PARTIAL" || result.delistedCount !== 0))) {
        throw new Error(`Unexpected import result: ${JSON.stringify(result)}`);
      }
      outcomes.push({ store: source.slug, status: result.status, fetched: catalogue.products.length,
        upserted: result.upsertedCount, detail: catalogue.issues.slice(0, 10).join("; ")
          + (catalogue.issues.length > 10 ? `; ${catalogue.issues.length - 10} more issue(s)` : "") });
      console.log(`✓ END ${source.slug} — ${result.status}; upserted=${result.upsertedCount}; delisted=${result.delistedCount}; ${elapsed(startedAt)}`);
    } catch (error) {
      const message = describeError(error);
      outcomes.push({ store: source.slug, status: "FAILED", fetched: 0, upserted: 0, detail: message });
      console.error(`✗ END ${source.slug} — ${message}; ${elapsed(startedAt)}`);
      if (!catalogueFetched) await recordFetchFailure(source.slug, message);
      else importFailed = true;
    }
  }
  for (const outcome of outcomes.filter((row) => row.status !== "SUCCESS")) {
    console.warn(`::warning::${annotation(`${outcome.store}: ${outcome.status}; ${outcome.detail || "Catalogue shrink safety guard; no delists"}`)}`);
  }
  if (process.env.GITHUB_STEP_SUMMARY) {
    const cell = (value) => String(value).replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll("|", "&#124;").replace(/[\r\n]/g, " ");
    await appendFile(process.env.GITHUB_STEP_SUMMARY,
      `## Catalogue refresh\n\n| Store | Status | Upserted | Details |\n|---|---|---:|---|\n`
      + outcomes.map((row) => `| ${cell(row.store)} | ${row.status} | ${row.upserted} | ${cell(row.detail)} |`).join("\n") + "\n");
  }
  const imported = outcomes.filter((row) => row.status !== "FAILED").length;
  console.log(`Catalogue refresh: ${imported}/${outcomes.length} stores imported; ${outcomes.filter((row) => row.status !== "SUCCESS").length} degraded`);
  if (importFailed || imported === 0) throw new Error("Catalogue refresh failed: backend import failure or no usable source data; see store results");
  return outcomes;
}

function annotation(value) {
  return value.replaceAll("%", "%25").replaceAll("\r", "%0D").replaceAll("\n", "%0A");
}

function catalogue(products, issues = []) {
  return { products, complete: issues.length === 0, issues };
}

async function recordFetchFailure(store, message) {
  try {
    await request(`${apiBase}/imports/failure?store=${encodeURIComponent(store)}`, {
      method: "POST",
      headers: { ...commonHeaders, "Content-Type": "application/json" },
      body: JSON.stringify({ message }),
      responseType: "text",
    });
  } catch (recordError) {
    const details = recordError instanceof Error ? recordError.message : String(recordError);
    console.error(`${store}: could not record failed run; ${details}`);
  }
}

async function fetchProducts(source) {
  switch (source.platform) {
    case "SHOPIFY":
      return fetchShopify(source);
    case "DOTPE":
      return fetchDotPe(source);
    case "WOOCOMMERCE":
      return fetchWooCommerce(source);
    default:
      throw new Error(`${source.slug}: unsupported platform ${source.platform}`);
  }
}

export async function fetchWooCommerce(source) {
  const baseUrl = source.baseUrl.replace(/\/$/, "");
  const endpoint = await wooCommerceEndpoint(baseUrl);
  const products = await fetchWooCommerceCollection(source.slug, endpoint, {}, "products");
  const variations = await fetchWooCommerceCollection(source.slug, endpoint, { type: "variation" }, "variations");
  const variationsByParent = new Map();
  for (const variation of variations.products) {
    if (variation?.parent != null) {
      const key = String(variation.parent);
      variationsByParent.set(key, [...(variationsByParent.get(key) ?? []), variation]);
    }
  }
  const issues = [...products.issues, ...variations.issues];
  const mapped = [];
  for (const product of products.products) {
    if (!product || product.id == null) {
      issues.push("Skipped product without an ID");
      continue;
    }
    const variants = variationsByParent.get(String(product.id)) ?? [];
    // Never replace a variable product's sizes with an incomplete variation list.
    if (product.type === "variable" && (!variations.complete || variants.length === 0)) {
      issues.push(`Skipped variable product ${product.id}: incomplete or missing variations`);
      continue;
    }
    try { mapped.push(mapWooCommerceProduct(product, variants)); }
    catch (error) { issues.push(`Product ${product.id}: ${describeError(error)}`); }
  }
  return catalogue(mapped, issues);
}

async function fetchWooCommerceCollection(storeSlug, endpoint, filters, label) {
  const products = [];
  for (let page = 1; page <= 100; page += 1) {
    try {
      const params = new URLSearchParams({ per_page: "100", page: String(page), ...filters });
      const pageProducts = await sourceRequest(`${endpoint}?${params.toString()}`, { headers: sourceHeaders });
      if (!Array.isArray(pageProducts)) throw new Error(`Expected an array from WooCommerce at ${endpoint}`);
      if (pageProducts.some((product) => product?.id == null || (label === "variations" && product.parent == null))) {
        throw new Error(`Malformed WooCommerce ${label} page`);
      }
      products.push(...pageProducts);
      console.log(`${storeSlug}: WooCommerce ${label} page ${page}; fetched ${products.length} records`);
      if (pageProducts.length < 100) return catalogue(products);
      await pause(750);
    } catch (error) {
      return catalogue(products, [`${label} page ${page}: ${describeError(error)}`]);
    }
  }
  return catalogue(products, [`${label}: pagination limit reached`]);
}

async function wooCommerceEndpoint(baseUrl) {
  const modernEndpoint = `${baseUrl}/wp-json/wc/store/v1/products`;
  try {
    await sourceRequest(`${modernEndpoint}?per_page=1`, { headers: sourceHeaders });
    return modernEndpoint;
  } catch (error) {
    if (error.status !== 404) throw error;
    return `${baseUrl}/wp-json/wc/store/products`;
  }
}

function mapWooCommerceProduct(product, variations) {
  validateProduct(product, "name");
  const mappedVariants = variations.map((variant, index) => mapWooCommerceVariant(variant, index));

  return {
    externalId: String(product.id),
    title: product.name,
    productUrl: product.permalink,
    imageUrl: product.images?.[0]?.src ?? null,
    tags: [...(product.categories ?? []), ...(product.tags ?? [])].map((entry) => entry.name).filter(Boolean),
    category: product.categories?.[0]?.name ?? null,
    sourceCreatedAt: null,
    variants: mappedVariants.length ? mappedVariants : [{
      ...mapWooCommerceVariant(product, 0), externalId: String(product.id), sizeLabel: null,
    }],
  };
}

function mapWooCommerceVariant(variant, position) {
  const prices = variant.prices ?? {};
  return {
    externalId: String(variant.id),
    sizeLabel: (variant.attributes ?? []).find((attribute) => attribute.name?.trim().toLowerCase() === "size")?.value
      ?? sizeFromVariationLabel(variant.variation),
    price: priceFromMinorUnits(prices.price, prices.currency_minor_unit),
    compareAtPrice: priceFromMinorUnits(prices.regular_price, prices.currency_minor_unit),
    available: Boolean(variant.is_in_stock ?? variant.stock_status !== "outofstock"),
    position,
  };
}

function sizeFromVariationLabel(label) {
  const match = typeof label === "string" && label.match(/(?:^|,\s*)size:\s*([^,]+)/i);
  return match ? match[1].trim() : null;
}

function priceFromMinorUnits(value, minorUnit = 2) {
  if (value == null || value === "") return null;
  const digits = String(value).trim();
  if (!/^-?\d+$/.test(digits)) return digits;
  if (minorUnit === 0) return digits;
  const negative = digits.startsWith("-");
  const unsigned = negative ? digits.slice(1) : digits;
  const padded = unsigned.padStart(minorUnit + 1, "0");
  const splitAt = padded.length - minorUnit;
  return `${negative ? "-" : ""}${padded.slice(0, splitAt)}.${padded.slice(splitAt)}`;
}

export async function fetchShopify(source) {
  const products = [];
  const issues = [];
  for (let page = 1; page <= 40; page += 1) {
    try {
      const response = await sourceRequest(`${source.baseUrl.replace(/\/$/, "")}/products.json?limit=250&page=${page}`, { headers: sourceHeaders });
      if (!Array.isArray(response.products)) throw new Error("Missing Shopify products array");
      for (const product of response.products) {
        try { products.push(mapShopifyProduct(product, source.baseUrl)); }
        catch (error) { issues.push(`Product ${product?.id}: ${describeError(error)}`); }
      }
      console.log(`${source.slug}: Shopify page ${page}; fetched ${products.length} products`);
      if (response.products.length < 250) return catalogue(products, issues);
      await pause(1000);
    } catch (error) {
      return catalogue(products, [...issues, `page ${page}: ${describeError(error)}`]);
    }
  }
  return catalogue(products, [...issues, "Shopify pagination limit reached"]);
}

function mapShopifyProduct(product, baseUrl) {
  validateProduct(product, "title");
  if (!Array.isArray(product.variants)) throw new Error("Missing Shopify variants array");
  const sizePosition = (product.options ?? []).find((option) => option.name?.trim().toLowerCase() === "size")?.position ?? 0;
  return {
    externalId: String(product.id),
    title: product.title,
    productUrl: `${baseUrl.replace(/\/$/, "")}/products/${product.handle}`,
    imageUrl: product.images?.[0]?.src ?? null,
    tags: Array.isArray(product.tags) ? product.tags : String(product.tags ?? "").split(",").map((tag) => tag.trim()).filter(Boolean),
    category: null,
    sourceCreatedAt: product.created_at ?? null,
    variants: (product.variants ?? []).map((variant) => ({
      externalId: String(variant.id),
      sizeLabel: sizePosition === 1 ? variant.option1 : sizePosition === 2 ? variant.option2 : sizePosition === 3 ? variant.option3 : null,
      price: variant.price,
      compareAtPrice: variant.compare_at_price ?? null,
      available: Boolean(variant.available),
      position: variant.position ?? 0,
    })),
  };
}

export async function fetchDotPe(source) {
  const baseUrl = source.baseUrl.replace(/\/$/, "");
  let buildId = await dotPeBuildId(baseUrl);
  const sitemap = await sourceRequest(`${baseUrl}/sitemap_products.xml`, { headers: sourceHeaders, responseType: "text" });
  const locations = [...sitemap.matchAll(/<loc>([^<]+)<\/loc>/g)]
    .map((match) => new URL(match[1].trim()))
    .map((url) => url.pathname.split("/").filter(Boolean))
    .filter((parts) => parts.length >= 3 && parts[0] === "product")
    .map((parts) => ({ id: parts[1], slug: parts[2] }));
  console.log(`${source.slug}: DotPe sitemap has ${locations.length} product routes`);

  const products = [];
  const issues = [];
  const startedAt = Date.now();
  let consecutiveFailures = 0;
  for (const [index, location] of locations.entries()) {
    if (Date.now() - startedAt > 10 * 60_000 || consecutiveFailures >= 3) {
      issues.push(`Stopped after ${index}/${locations.length} routes: source time budget or repeated failures`);
      break;
    }
    try {
      let product;
      try { product = await dotPeProduct(baseUrl, buildId, location); }
      catch (error) {
        if (error.status !== 404) throw error;
        buildId = await dotPeBuildId(baseUrl);
        product = await dotPeProduct(baseUrl, buildId, location);
      }
      products.push(mapDotPeProduct(product, baseUrl));
      consecutiveFailures = 0;
    } catch (error) {
      issues.push(`route ${location.id}: ${describeError(error)}`);
      // Stale sitemap routes are common; preserve their previous data without tripping the outage breaker.
      if (error.status !== 404) consecutiveFailures += 1;
      else consecutiveFailures = 0;
    }
    const completed = index + 1;
    if (completed % 25 === 0 || completed === locations.length) {
      console.log(`${source.slug}: DotPe processed ${completed}/${locations.length} routes`);
    }
    await pause(250);
  }
  return catalogue(products, issues);
}

async function dotPeBuildId(baseUrl) {
  const homepage = await sourceRequest(baseUrl, { headers: sourceHeaders, responseType: "text" });
  const match = homepage.match(/"buildId"\s*:\s*"([^"]+)"/);
  if (!match) throw new Error(`Could not find Next.js build ID on ${baseUrl}`);
  return match[1];
}

async function dotPeProduct(baseUrl, buildId, location) {
  const url = `${baseUrl}/_next/data/${encodeURIComponent(buildId)}/product/${encodeURIComponent(location.id)}/${encodeURIComponent(location.slug)}.json`;
  const response = await sourceRequest(url, { headers: sourceHeaders });
  return response.pageProps.product;
}

function mapDotPeProduct(product, baseUrl) {
  validateProduct(product, "name");
  const variants = product.variants?.map((variant) => ({
    externalId: String(variant.variant_id),
    sizeLabel: (variant.option_variant_names ?? []).find((option) => option.option_name?.toLowerCase() === "size")?.variant_name ?? null,
    price: variant.discounted_price ?? variant.price,
    compareAtPrice: variant.discounted_price != null && Number(variant.price) > Number(variant.discounted_price) ? variant.price : null,
    available: variant.available === 1,
    position: 0,
  })) ?? [];
  return {
    externalId: String(product.id),
    title: product.name,
    productUrl: product.link || `${baseUrl}/product/${product.id}`,
    imageUrl: product.image_url ?? null,
    tags: (product.collections ?? []).map((collection) => collection.name).filter(Boolean),
    category: product.category?.name ?? null,
    sourceCreatedAt: null,
    variants: variants.length ? variants : [{
      externalId: String(product.id), sizeLabel: null, price: product.discounted_price ?? product.price,
      compareAtPrice: product.discounted_price != null && Number(product.price) > Number(product.discounted_price) ? product.price : null,
      available: false, position: 0,
    }],
  };
}

function validateProduct(product, titleField) {
  if (product?.id == null || String(product.id).trim() === "" || typeof product[titleField] !== "string" || !product[titleField].trim()) {
    throw new Error("Product is missing its ID or title");
  }
}

export async function request(url, options = {}) {
  const { responseType = "json", retryNetwork = false, timeoutMs = SOURCE_REQUEST_TIMEOUT_MS, sleep = pause, ...fetchOptions } = options;
  const attempts = retryNetwork && (!fetchOptions.method || fetchOptions.method === "GET") ? 3 : 1;
  for (let attempt = 1; attempt <= attempts; attempt += 1) {
    try {
      const response = await fetch(url, { ...fetchOptions, signal: AbortSignal.timeout(timeoutMs) });
      if (!response.ok) {
        const error = new Error(`${response.status} ${response.statusText}: ${(await response.text()).slice(0, 500)}`);
        error.status = response.status;
        const retryAfter = response.headers.get("retry-after");
        if (retryAfter) error.retryAfterMs = /^\d+$/.test(retryAfter)
          ? Number(retryAfter) * 1000 : Math.max(0, Date.parse(retryAfter) - Date.now());
        throw error;
      }
      return responseType === "text" ? await response.text() : await response.json();
    } catch (error) {
      if (attempt === attempts || !isTransientNetworkError(error) || error.retryAfterMs > 30_000) {
        throw withRequestContext(url, error);
      }
      const delay = Math.max(750 * 2 ** (attempt - 1), error.retryAfterMs || 0);
      console.warn(`Transient failure for ${url}; retry ${attempt}/${attempts - 1} in ${delay}ms (${describeError(error)})`);
      await sleep(delay);
    }
  }
  throw new Error(`Failed to fetch ${url}`);
}

function sourceRequest(url, options = {}) {
  return request(url, { ...options, retryNetwork: true });
}

function isTransientNetworkError(error) {
  return [408, 429, 500, 502, 503, 504].includes(error?.status)
    || error?.name === "TimeoutError"
    || (error instanceof TypeError && (error.message === "fetch failed" || error.message === "terminated"));
}

function withRequestContext(url, error) {
  const contextual = new Error(`Failed to fetch ${url}: ${describeError(error)}`, { cause: error });
  contextual.status = error?.status;
  return contextual;
}

function describeError(error) {
  const message = error instanceof Error ? error.message : String(error);
  const cause = error && typeof error === "object" ? error.cause : null;
  if (!cause) return message;
  const causeMessage = cause.message ?? String(cause);
  if (message.includes(causeMessage)) return message;
  const causeCode = cause.code ? `${cause.code}: ` : "";
  return `${message} (${causeCode}${causeMessage})`;
}

function required(name) {
  if (!process.env[name]) throw new Error(`Set ${name}`);
  return process.env[name];
}

function positiveIntegerEnv(name, fallback) {
  const value = process.env[name];
  if (value == null || value.trim() === "") return fallback;
  if (!/^\d+$/.test(value) || Number(value) < 1) {
    throw new Error(`${name} must be a positive integer number of milliseconds`);
  }
  return Number(value);
}

function sourceLabel(source) {
  return `${source.slug} (${source.baseUrl})`;
}

function elapsed(startedAt) {
  return `${((Date.now() - startedAt) / 1000).toFixed(1)}s`;
}

function pause(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  await main();
}
