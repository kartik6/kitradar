const apiBase = required("KITRADAR_API_URL").replace(/\/$/, "") + "/api/v1/admin";
const adminToken = required("ADMIN_TOKEN");
const requestedStore = process.env.STORE?.trim();

const commonHeaders = {
  Authorization: `Bearer ${adminToken}`,
  "User-Agent": "KitPagluBot/1.0 (+https://kitpaglu.vercel.app)",
};

const sources = await request(`${apiBase}/sources`, { headers: commonHeaders });
const selectedSources = requestedStore
  ? sources.filter((source) => source.slug === requestedStore)
  : sources;

if (requestedStore && selectedSources.length === 0) {
  throw new Error(`No enabled store found for slug ${requestedStore}`);
}

const failures = [];
const workflowStartedAt = Date.now();
console.log(`Catalogue refresh starting: ${selectedSources.length} enabled store(s)`);
for (const source of selectedSources) {
  let catalogueFetched = false;
  const sourceStartedAt = Date.now();
  try {
    console.log(`▶ START ${sourceLabel(source)} — ${source.platform}`);
    const products = await fetchProducts(source);
    catalogueFetched = true;
    const result = await request(`${apiBase}/imports/raw?store=${encodeURIComponent(source.slug)}`, {
      method: "POST",
      headers: { ...commonHeaders, "Content-Type": "application/json" },
      body: JSON.stringify({ products }),
    });
    console.log(`✓ END ${sourceLabel(source)} — ${result.status}; fetched=${result.fetchedCount}; upserted=${result.upsertedCount}; delisted=${result.delistedCount}; ${elapsed(sourceStartedAt)}`);
  } catch (error) {
    const message = describeError(error);
    failures.push(`${source.slug}: ${message}`);
    console.error(`✗ END ${sourceLabel(source)} — FAILED; ${message}; ${elapsed(sourceStartedAt)}`);
    if (!catalogueFetched) {
      await recordFetchFailure(source.slug, message);
    }
  }
}

if (failures.length) {
  console.error(`Catalogue refresh ended with ${failures.length} failure(s); ${elapsed(workflowStartedAt)}`);
  throw new Error(`${failures.length} catalogue source(s) failed:\n${failures.join("\n")}`);
}
console.log(`Catalogue refresh complete: ${selectedSources.length} store(s) succeeded; ${elapsed(workflowStartedAt)}`);

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

async function fetchWooCommerce(source) {
  const baseUrl = source.baseUrl.replace(/\/$/, "");
  const endpoint = await wooCommerceEndpoint(baseUrl);
  const products = await fetchWooCommerceCollection(source.slug, endpoint, {}, "products");
  const variations = await fetchWooCommerceCollection(source.slug, endpoint, { type: "variation" }, "variations");
  const variationsByParent = new Map();
  for (const variation of variations) {
    if (variation.parent != null) {
      const key = String(variation.parent);
      variationsByParent.set(key, [...(variationsByParent.get(key) ?? []), variation]);
    }
  }
  return products.map((product) => mapWooCommerceProduct(product, variationsByParent.get(String(product.id)) ?? []));
}

async function fetchWooCommerceCollection(storeSlug, endpoint, filters, label) {
  const products = [];
  for (let page = 1; page <= 100; page += 1) {
    const params = new URLSearchParams({ per_page: "100", page: String(page), ...filters });
    const pageProducts = await sourceRequest(`${endpoint}?${params.toString()}`, { headers: commonHeaders });
    if (!Array.isArray(pageProducts)) {
      throw new Error(`Expected an array from WooCommerce at ${endpoint}`);
    }
    products.push(...pageProducts);
    console.log(`${storeSlug}: WooCommerce ${label} page ${page}; fetched ${products.length} records`);
    if (pageProducts.length < 100) break;
    await pause(750);
  }
  return products;
}

async function wooCommerceEndpoint(baseUrl) {
  const modernEndpoint = `${baseUrl}/wp-json/wc/store/v1/products`;
  try {
    await sourceRequest(`${modernEndpoint}?per_page=1`, { headers: commonHeaders });
    return modernEndpoint;
  } catch (error) {
    if (error.status !== 404) throw error;
    return `${baseUrl}/wp-json/wc/store/products`;
  }
}

function mapWooCommerceProduct(product, variations) {
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

async function fetchShopify(source) {
  const products = [];
  for (let page = 1; page <= 40; page += 1) {
    const response = await sourceRequest(`${source.baseUrl.replace(/\/$/, "")}/products.json?limit=250&page=${page}`, {
      headers: commonHeaders,
    });
    const pageProducts = response.products ?? [];
    products.push(...pageProducts.map((product) => mapShopifyProduct(product, source.baseUrl)));
    console.log(`${source.slug}: Shopify page ${page}; fetched ${products.length} products`);
    if (pageProducts.length < 250) break;
    await pause(1000);
  }
  return products;
}

function mapShopifyProduct(product, baseUrl) {
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

async function fetchDotPe(source) {
  const baseUrl = source.baseUrl.replace(/\/$/, "");
  let buildId = await dotPeBuildId(baseUrl);
  const sitemap = await sourceRequest(`${baseUrl}/sitemap_products.xml`, { headers: commonHeaders, responseType: "text" });
  const locations = [...sitemap.matchAll(/<loc>([^<]+)<\/loc>/g)]
    .map((match) => new URL(match[1].trim()))
    .map((url) => url.pathname.split("/").filter(Boolean))
    .filter((parts) => parts.length >= 3 && parts[0] === "product")
    .map((parts) => ({ id: parts[1], slug: parts[2] }));
  console.log(`${source.slug}: DotPe sitemap has ${locations.length} product routes`);

  const products = [];
  for (const [index, location] of locations.entries()) {
    try {
      const product = await dotPeProduct(baseUrl, buildId, location);
      products.push(mapDotPeProduct(product, baseUrl));
    } catch (error) {
      if (error.status !== 404) throw error;
      buildId = await dotPeBuildId(baseUrl);
      try {
        const product = await dotPeProduct(baseUrl, buildId, location);
        products.push(mapDotPeProduct(product, baseUrl));
      } catch (retryError) {
        if (retryError.status !== 404) throw retryError;
      }
    }
    const completed = index + 1;
    if (completed % 25 === 0 || completed === locations.length) {
      console.log(`${source.slug}: DotPe processed ${completed}/${locations.length} routes`);
    }
    await pause(250);
  }
  return products;
}

async function dotPeBuildId(baseUrl) {
  const homepage = await sourceRequest(baseUrl, { headers: commonHeaders, responseType: "text" });
  const match = homepage.match(/"buildId"\s*:\s*"([^"]+)"/);
  if (!match) throw new Error(`Could not find Next.js build ID on ${baseUrl}`);
  return match[1];
}

async function dotPeProduct(baseUrl, buildId, location) {
  const url = `${baseUrl}/_next/data/${encodeURIComponent(buildId)}/product/${encodeURIComponent(location.id)}/${encodeURIComponent(location.slug)}.json`;
  const response = await sourceRequest(url, { headers: commonHeaders });
  return response.pageProps.product;
}

function mapDotPeProduct(product, baseUrl) {
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

async function request(url, options = {}) {
  const { responseType = "json", retryNetwork = false, ...fetchOptions } = options;
  const attempts = retryNetwork ? 2 : 1;
  for (let attempt = 1; attempt <= attempts; attempt += 1) {
    try {
      const response = await fetch(url, { ...fetchOptions, signal: AbortSignal.timeout(20_000) });
      if (!response.ok) {
        const error = new Error(`${response.status} ${response.statusText}: ${(await response.text()).slice(0, 500)}`);
        error.status = response.status;
        throw error;
      }
      return responseType === "text" ? response.text() : response.json();
    } catch (error) {
      if (error?.status || attempt === attempts || !isTransientNetworkError(error)) {
        throw withRequestContext(url, error);
      }
      console.warn(`Transient network failure for ${url}; retrying once in 750ms (${describeError(error)})`);
      await pause(750);
    }
  }
  throw new Error(`Failed to fetch ${url}`);
}

function sourceRequest(url, options = {}) {
  return request(url, { ...options, retryNetwork: true });
}

function isTransientNetworkError(error) {
  return error instanceof TypeError && error.message === "fetch failed";
}

function withRequestContext(url, error) {
  if (error?.status) return error;
  return new Error(`Failed to fetch ${url}: ${describeError(error)}`, { cause: error });
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

function sourceLabel(source) {
  return `${source.slug} (${source.baseUrl})`;
}

function elapsed(startedAt) {
  return `${((Date.now() - startedAt) / 1000).toFixed(1)}s`;
}

function pause(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}
