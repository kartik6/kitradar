const apiBase = required("KITRADAR_API_URL").replace(/\/$/, "") + "/api/v1/admin";
const adminToken = required("ADMIN_TOKEN");
const requestedStore = process.env.STORE?.trim();

const commonHeaders = {
  Authorization: `Bearer ${adminToken}`,
  "User-Agent": "KitPagluBot/1.0 (+https://kitpaglu.vercel.app)",
};

const sources = await request(`${apiBase}/sources`);
const selectedSources = requestedStore
  ? sources.filter((source) => source.slug === requestedStore)
  : sources;

if (requestedStore && selectedSources.length === 0) {
  throw new Error(`No enabled store found for slug ${requestedStore}`);
}

for (const source of selectedSources) {
  const products = await fetchProducts(source);
  const result = await request(`${apiBase}/imports/raw?store=${encodeURIComponent(source.slug)}`, {
    method: "POST",
    headers: { ...commonHeaders, "Content-Type": "application/json" },
    body: JSON.stringify({ products }),
  });
  console.log(`${source.slug}: ${result.status}; fetched=${result.fetchedCount}; upserted=${result.upsertedCount}`);
}

async function fetchProducts(source) {
  switch (source.platform) {
    case "SHOPIFY":
      return fetchShopify(source);
    case "DOTPE":
      return fetchDotPe(source);
    default:
      throw new Error(`${source.slug}: unsupported platform ${source.platform}`);
  }
}

async function fetchShopify(source) {
  const products = [];
  for (let page = 1; page <= 40; page += 1) {
    const response = await request(`${source.baseUrl.replace(/\/$/, "")}/products.json?limit=250&page=${page}`, {
      headers: commonHeaders,
    });
    const pageProducts = response.products ?? [];
    products.push(...pageProducts.map((product) => mapShopifyProduct(product, source.baseUrl)));
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
  const sitemap = await request(`${baseUrl}/sitemap_products.xml`, { headers: commonHeaders, responseType: "text" });
  const locations = [...sitemap.matchAll(/<loc>([^<]+)<\/loc>/g)]
    .map((match) => new URL(match[1].trim()))
    .map((url) => url.pathname.split("/").filter(Boolean))
    .filter((parts) => parts.length >= 3 && parts[0] === "product")
    .map((parts) => ({ id: parts[1], slug: parts[2] }));

  const products = [];
  for (const location of locations) {
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
    await pause(250);
  }
  return products;
}

async function dotPeBuildId(baseUrl) {
  const homepage = await request(baseUrl, { headers: commonHeaders, responseType: "text" });
  const match = homepage.match(/"buildId"\s*:\s*"([^"]+)"/);
  if (!match) throw new Error(`Could not find Next.js build ID on ${baseUrl}`);
  return match[1];
}

async function dotPeProduct(baseUrl, buildId, location) {
  const url = `${baseUrl}/_next/data/${encodeURIComponent(buildId)}/product/${encodeURIComponent(location.id)}/${encodeURIComponent(location.slug)}.json`;
  const response = await request(url, { headers: commonHeaders });
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
  const { responseType = "json", ...fetchOptions } = options;
  const response = await fetch(url, fetchOptions);
  if (!response.ok) {
    const error = new Error(`${response.status} ${response.statusText}: ${(await response.text()).slice(0, 500)}`);
    error.status = response.status;
    throw error;
  }
  return responseType === "text" ? response.text() : response.json();
}

function required(name) {
  if (!process.env[name]) throw new Error(`Set ${name}`);
  return process.env[name];
}

function pause(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}
