# KitRadar — Product Requirements Document (v0)

**Status:** Approved for build
**Last updated:** 2026-08-01
**Engineering plan:** [`../plan.md`](../plan.md)

---

## 1. Problem

Indian football-jersey stores sell heavily overlapping catalogues at different prices, with different sizes in stock, and at different quality tiers ("fan", "player", "master", "retro"). A buyer who wants a Real Madrid home kit in size M today has to open seven browser tabs, search each store separately, learn each store's private vocabulary for quality tiers, and check size availability one product page at a time.

There is no single place to see what is actually buyable, right now, across all of them.

## 2. Product

KitRadar pulls every store's catalogue on a nightly schedule into one normalised database and serves searchable, filterable jersey and clearance grids. Buyers can save kits as browser-local favourites, and clicking a card sends them to the original store to purchase.

KitRadar is a **discovery and comparison surface, not a marketplace.** We never handle carts, checkout, inventory, payments, shipping, or returns. We hold no stock and take no orders.

## 3. Goals and non-goals

### Goals

- Show every jersey available across the covered stores in one grid.
- Let a buyer narrow to exactly what they can buy: their size, in stock, their budget, their preferred version.
- Get them to the right product page on the right store in one click.
- Refresh accurately every night with no manual work.

### Non-goals (v0)

Deliberately excluded to keep v0 shippable. Each is a candidate for v1.

| Not in v0 | Why |
|---|---|
| Price history / price-drop alerts | Requires a time-series table and a notification channel; a whole feature in itself |
| User accounts, login | Nothing in v0 is per-user |
| Synced / cross-device favourites | Requires an account and server-side user data; v0 favourites are deliberately browser-local |
| Cross-store deduplication ("this same kit, 3 stores") | The genuinely hard problem — needs fuzzy product matching. Worth doing properly later, not badly now. |
| Affiliate links / monetisation | No commercial arrangements exist yet |
| Mobile app | The web grid is responsive |
| Admin UI | A token-protected refresh endpoint is enough for one operator |

## 4. Users

**Primary — the buyer.** Knows the club and kit they want. Cares about three things in order: is it available in my size, what does it cost, and is it the quality tier I expect. Arrives on mobile more often than desktop.

**Secondary — the operator (you).** Needs to know the nightly refresh ran, how many products each store returned, and what failed.

## 5. Coverage

### v0 stores

| Store | Domain | Platform | Approx. products |
|---|---|---|---|
| Goal Thread | `www.goalthread.co.in` | DotPe | ~359 |
| Full Time Store | `fulltimestore.in` | Shopify | ~508 |
| The Football Frenzy | `www.thefootballfrenzy.com` | Shopify | ~660 |

**~1,527 products at launch.**

### Planned coverage (post-v0)

Verified as Shopify, so each is a configuration change with no code: **Thayyil Sports** (`thayyilsports.com`), **Store 1914** (`store1914.in`), **Gully Sports** (`gullysports.in`).

**Shopezzy** (`shopezzy.com`) runs on neither platform and needs its own investigation before it can be added.

## 6. User stories

1. As a buyer, I search "real madrid" and see matching kits from every store in one grid.
2. As a buyer, I filter to size **M** and **In stock only**, and every result genuinely has an M I can buy right now.
3. As a buyer, I filter to **Player version** because I do not want a fan replica.
4. As a buyer, I sort by price ascending to find the cheapest listing.
5. As a buyer, I see the store name, sale price, seller-listed MRP, and discount on every genuinely discounted card before I click.
6. As a buyer, I click a card and land on that exact product's page on the original store, in a new tab.
7. As a buyer, I share a filtered URL with a friend and they see the same results.
8. As a buyer, I know how fresh the data is before I trust a price.
9. As a buyer, I save a kit to my favourites and find it again in the same browser.
10. As a buyer, I browse only clearance listings and can clearly compare the seller-listed MRP with the sale price.
11. As the operator, I trigger a refresh manually and see per-store counts and errors.
12. As the operator, I can inspect recent per-store refresh runs, their status, timestamps, counts, and errors.

## 7. Functional requirements

### FR-1 — Catalogue browse

Paginated grid, 24 kits per page. A **Load more** button appends the next page without a full navigation. Header shows `{N} kits found` and `Live listings pulled from {M} stores`.

### FR-1a — Clearance browse

`/clearance` is a dedicated, indexable view of products with a genuine seller-provided discount: the seller-listed reference price is present and strictly greater than the selling price. It uses the same cards, filters, pagination, and outbound links as the catalogue, and defaults to **Biggest discount** sorting. It is not a manually curated or separately scraped catalogue.

### FR-2 — Product card

Each card shows:

- Product image, lazy-loaded, fixed aspect ratio so the grid never reflows
- Product title
- Store name badge (e.g. `GOAL THREAD`)
- Version badge (e.g. `FAN VERSION`), omitted when the version is unknown
- Sale price in ₹, with clear primary emphasis
- Seller-listed **MRP**, struck through, plus the percentage off, only when that price is strictly greater than the sale price
- An `Out of stock` state when no variant is purchasable
- A keyboard-accessible save/remove-favourite control that does not activate the card link

The whole card is a link opening in a new tab with `rel="noopener nofollow sponsored"`.

### FR-2a — Favourites

Buyers can save or remove a kit from any card and view their saved kits at `/favorites`. Favourites are stored only in `localStorage` as product IDs (namespaced and versioned as `kitradar:favorites:v1`), with a 100-kit limit. The favourites page resolves those IDs against the current catalogue after hydration; a saved product that is no longer listed remains saved but is called out as unavailable.

No account, login, server-side profile, analytics, or cross-device sync is involved. Clearing browser storage, changing browsers, or using a different device clears/separates the list.

### FR-3 — Search

Free-text over product title and store tags.

- Multi-token, ANDed: `real madrid away` matches only products containing all three
- Case- and accent-insensitive
- Matches partial words — `madr` matches `Real Madrid`, because that is what people type
- Debounced 300ms in the browser, executed server-side

### FR-4 — Filters

All filters combine, and all are reflected in the URL query string so results are shareable and the back button works.

| Filter | Values | Behaviour |
|---|---|---|
| Size | `S, M, L, XL, XXL, XXXL` | Multi-select, OR within the filter |
| Store | Live list from the API | Multi-select, OR within the filter |
| Version | `FAN, PLAYER, MASTER, RETRO, KIDS, WOMEN, LONG_SLEEVE` | Multi-select, OR within the filter |
| In stock only | Boolean | At least one purchasable variant |
| Price range | Min / max in ₹ | Against the product's lowest price |

**Critical interaction — size + in stock.** When both are set, a product qualifies only if *the same variant* is both that size and available. A product with a sold-out M and an in-stock XL must **not** appear under "M, in stock only". Getting this wrong is the single most damaging correctness bug in the product, because it sends buyers to pages where they cannot buy.

Non-jersey items (shorts, socks, jackets, keychains, match balls — roughly 5% of listings) are excluded from the grid by default.

### FR-5 — Sort

| Option | Ordering |
|---|---|
| Featured *(default)* | In-stock first, then newest, then id |
| Price: low to high | Lowest price ascending |
| Price: high to low | Lowest price descending |
| Biggest discount | Discount percentage descending |
| Newest | Source creation date descending |

### FR-6 — Version classification

Every product is classified into one of `FAN`, `PLAYER`, `MASTER`, `RETRO`, `KIDS`, `WOMEN`, `LONG_SLEEVE`, or `UNKNOWN`.

Stores describe versions inconsistently — one uses a product category, another uses tags, a third only hints in the title. Classification therefore uses an ordered priority chain: the merchant's own category is trusted first, then exact tag matches, then title patterns.

**Where classification is uncertain, the product is `UNKNOWN`, not guessed.** `UNKNOWN` products appear in unfiltered browsing but are excluded from version-filtered results. A buyer who filters to "Player version" must be able to trust every result; showing them a maybe-player is worse than showing them nothing.

### FR-7 — Nightly refresh

Runs at 03:00 IST. For each store: fetch the catalogue, normalise it, insert or update products, and mark products that have disappeared as delisted.

- One store failing must never affect the others
- A partial fetch must never wipe a store's catalogue
- Every run is recorded with per-store counts, duration, and any error

### FR-8 — Freshness indicator

The UI shows when the data was last refreshed ("Updated 6 hours ago"), so a buyer knows how much to trust a price.

### FR-9 — Operator refresh

`POST /api/v1/admin/refresh`, protected by a bearer token, optionally scoped to one store. Returns per-store fetched / upserted / delisted counts and any errors. Concurrent runs are prevented.

`GET /api/v1/admin/runs`, protected by the same bearer token, lists newest refresh runs first and supports `store`, `page`, and `pageSize` filters. It is an operator API, not a public browser endpoint.

## 8. Non-functional requirements

**Performance.** Filtered query under 300ms p95 server-side on a warm cache. Results cached 15 minutes in-process and flushed immediately after a successful refresh, so data is never staler than the last run.

**Politeness to source stores.** This is a hard requirement, not a nicety. Identifying User-Agent (`KitRadarBot/0.1`), at most 4 concurrent requests per host, at least 250ms between requests, 15s timeout, 3 retries with exponential backoff. Only public endpoints are read. `robots.txt` is respected. A full run touches each store for well under a minute.

**Correctness over coverage.** Prefer showing less and being right. `UNKNOWN` beats a wrong guess; excluding an ambiguous product beats sending a buyer to a dead end.

**Accessibility.** Keyboard-navigable filters, visible focus states, alt text on product images, contrast meeting WCAG AA.

**Legal and ethical posture.**

- Footer disclaimer: *"Not affiliated with any of these stores. Prices and availability are indicative — confirm on the seller's site."*
- Outbound links carry `rel="nofollow sponsored"`
- Product images are hot-linked from the stores' own CDNs, never re-hosted
- MRP and discount display use only a reference price supplied by the seller. When no higher reference price is supplied, KitRadar shows only the sale price and makes no discount claim.
- We link to stores, we do not reproduce their storefronts, undercut them, or intercept their checkout
- Any store that asks to be removed is removed, by flipping one `enabled` flag

## 9. Success criteria

| Criterion | Target |
|---|---|
| Catalogue size | ≥1,500 products across 3 stores |
| Freshness | Prices and stock match source within 24 hours |
| Query latency | <300ms p95, warm cache |
| Version classification | `UNKNOWN` is a minority of products |
| Size + in-stock correctness | 100% — every result under "size M, in stock" has a buyable M |
| Extensibility | Adding a fourth Shopify store needs no code change |

## 10. Open questions

1. **Shopezzy's platform** is unidentified. Resolve before committing to a date for full seven-store coverage.
2. **Long sleeve** is an attribute, not a quality tier — it is orthogonal to fan/player/master. v0 treats it as a version value only when no stronger signal matched, mirroring the reference site. If it proves confusing in use, promote it to its own filter in v1.
3. **Cross-store deduplication** is the feature that would make this genuinely better than seven tabs rather than merely more convenient. It is deferred on purpose, but it is the obvious v1 headline.
