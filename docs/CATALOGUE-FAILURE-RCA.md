# Catalogue refresh failures: RCA and safe recovery

Reviewed on 2026-10-24: the latest 20 GitHub Actions runs, all four failed runs in that window, and the latest successful run. Dates below are UTC run-start dates.

## Evidence

| Run | Failed source | Observed failure | Other stores imported | Products upserted by other stores |
|---|---|---|---:|---:|
| [Oct 13](https://github.com/kartik6/kitradar/actions/runs/38337911224) | full-time-store | Retailer HTTP 500 | 10 | 3,878 |
| [Oct 17](https://github.com/kartik6/kitradar/actions/runs/39078465920) | goal-thread | Retailer HTTP 502 after the 225/517 progress checkpoint | 10 | 4,070 |
| [Oct 20](https://github.com/kartik6/kitradar/actions/runs/39347971892) | outhouse-athleisure | Retailer HTTP 500; response HTML title was `Database Error` | 10 | 4,157 |
| [Oct 22](https://github.com/kartik6/kitradar/actions/runs/39697546324) | outhouse-athleisure | 20-second timeout probing the WooCommerce endpoint | 10 | 4,187 |

The [latest successful run](https://github.com/kartik6/kitradar/actions/runs/40042208525), started Oct 23 and finished Oct 24 UTC, imported all 11 stores and upserted 4,590 products in approximately 676 seconds. The successful imports in the four failed workflows were already committed; the final nonzero worker exit did not roll them back. No backend import failure or job timeout was observed in those four runs.

The logs establish intermittent retailer-side HTTP failures and a request deadline expiry. They do not establish the underlying merchant infrastructure failure; the timeout could involve the retailer or network path. Outhouse's HTML suggests a merchant database problem, but merchant logs would be needed to confirm its cause.

## Why isolated outages became failed workflows

The worker continued to other stores, then threw if any source failed. Within a source, however, a failed request discarded previously fetched pages/products. The request helper retried only `TypeError: fetch failed`, excluding HTTP 500/502 and `TimeoutError`. Response-body decoding was returned without awaiting inside the retry block, so interrupted bodies also bypassed that catch.

Simply uploading the products collected before an error would have been unsafe: the backend only suppressed delisting when the incoming count was below 50% of the active catalogue. A known-incomplete 80% catalogue could deactivate the unseen 20%. WooCommerce needs an additional safeguard because applying a product replaces its variants: incomplete variation pagination must not remove known sizes.

## Implemented behavior

- GET requests receive at most three attempts for network interruption, timeout, HTTP 408/429/500/502/503/504. Backoff is bounded; `Retry-After` is respected up to 30 seconds, and longer waits abandon the request rather than retry early. Import POSTs are never retried automatically because a timeout can leave their commit outcome unknown.
- Shopify and WooCommerce retain earlier pages when later pages fail. Pagination caps and malformed response shapes mark the result incomplete. DotPe continues past individual failed product routes, stopping after three consecutive non-404 failures or its ten-minute route-processing budget.
- WooCommerce variable products are omitted when the variation catalogue is incomplete or their variations are missing. Their existing product and size data remain untouched. Usable simple products can still sync.
- Incomplete catalogues use `POST /api/v1/admin/imports/raw/partial`. That endpoint forces `PARTIAL` and zero delists independently of the count heuristic. Empty payloads never delist. Complete catalogues retain normal delisting and the existing shrink guard. Each backend import remains transactional.
- A separate endpoint makes rollout fail closed: an old backend returns 404 rather than silently ignoring a new completeness flag and delisting products. The worker does not fall back to a complete import.
- Mixed source outcomes succeed with explicit GitHub warning annotations and a per-store job summary when at least one catalogue imports. All-source failure, source-registry/configuration errors, and any backend import failure still fail the workflow. Individual failed fetches continue to be recorded in backend run history; partial imports record `PARTIAL`, while detailed source errors are in the workflow summary.
- GitHub Actions uses Node 22 and runs the worker tests before ingestion. A concurrency group prevents overlapping refresh workflows and does not cancel an active import.

## Tradeoffs and limits

Unresolved DotPe 404s after refreshing the build ID now produce partial imports instead of silently treating missing routes as confirmed deletions. The sampled successful runs had fewer imported products than sitemap routes. If those routes remain unresolved, future runs can remain partial and stale listings will persist until a complete fetch or an audited cleanup. This is a deliberate preservation policy; the warnings must remain visible to operators.

Partial sync applies complete product records, not arbitrary fragments of a product. Database/normalization failures still roll back that store's import and fail the job; this change does not attempt unsafe per-row recovery from transaction failures. The existing 25-minute job timeout still applies, and the in-process backend scrapers are outside this worker-specific recovery change.

## Additional security finding

The previous worker reused an `Authorization: Bearer ADMIN_TOKEN` header on public retailer requests. Public-source requests now use only the bot User-Agent; authorization is restricted to backend API requests. Historical runs sent the credential to third-party retailers. Rotate the existing admin token in both the backend and GitHub Actions after the corrected worker is active. This review found the exposure path, not evidence of misuse.

## Rollout and validation

1. Deploy the backend containing `/imports/raw/partial` first. No schema migration is needed; `PARTIAL` already exists.
2. Publish the worker/workflow changes, then rotate the shared admin token in backend and Actions configuration together.
3. Run a manual refresh and inspect the per-store summary and `/api/v1/admin/runs`. Partial imports must show `PARTIAL` and `delistedCount=0`; healthy complete imports retain ordinary delisting.
4. Investigate repeated degraded outcomes even when the overall workflow is green. Re-run a specific store once its upstream recovers.

Local verification: 12 Node worker tests and 28 Java backend tests passed, including four synchronizer regression tests. No production deployment, workflow dispatch, secret rotation, or live catalogue write was performed during this review.
