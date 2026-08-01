CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE store (
  id              BIGSERIAL PRIMARY KEY,
  slug            TEXT UNIQUE NOT NULL,
  name            TEXT NOT NULL,
  base_url        TEXT NOT NULL,
  platform        TEXT NOT NULL,
  enabled         BOOLEAN NOT NULL DEFAULT TRUE,
  last_scraped_at TIMESTAMPTZ
);

CREATE TABLE product (
  id                BIGSERIAL PRIMARY KEY,
  store_id          BIGINT NOT NULL REFERENCES store(id),
  external_id       TEXT NOT NULL,
  title             TEXT NOT NULL,
  search_text       TEXT NOT NULL,
  product_url       TEXT NOT NULL,
  image_url         TEXT,
  version           TEXT NOT NULL DEFAULT 'UNKNOWN',
  product_kind      TEXT NOT NULL DEFAULT 'JERSEY',
  min_price         NUMERIC(10, 2) NOT NULL,
  compare_at_price  NUMERIC(10, 2),
  discount_percent  INT NOT NULL DEFAULT 0,
  in_stock          BOOLEAN NOT NULL,
  source_created_at TIMESTAMPTZ,
  first_seen_at     TIMESTAMPTZ NOT NULL,
  last_seen_at      TIMESTAMPTZ NOT NULL,
  active            BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uq_product_store_external_id UNIQUE (store_id, external_id)
);

CREATE TABLE product_variant (
  id               BIGSERIAL PRIMARY KEY,
  product_id       BIGINT NOT NULL REFERENCES product(id) ON DELETE CASCADE,
  external_id      TEXT NOT NULL,
  size_label       TEXT,
  size_norm        TEXT,
  price            NUMERIC(10, 2) NOT NULL,
  compare_at_price NUMERIC(10, 2),
  available        BOOLEAN NOT NULL,
  position         INT NOT NULL DEFAULT 0,
  CONSTRAINT uq_product_variant_external_id UNIQUE (product_id, external_id)
);

CREATE TABLE scrape_run (
  id              BIGSERIAL PRIMARY KEY,
  store_id        BIGINT REFERENCES store(id),
  started_at      TIMESTAMPTZ NOT NULL,
  finished_at     TIMESTAMPTZ,
  status          TEXT NOT NULL,
  fetched_count   INT NOT NULL DEFAULT 0,
  upserted_count  INT NOT NULL DEFAULT 0,
  delisted_count  INT NOT NULL DEFAULT 0,
  error_message   TEXT
);

CREATE INDEX idx_product_search ON product USING gin (search_text gin_trgm_ops);
CREATE INDEX idx_product_browse ON product (active, product_kind, in_stock, min_price);
CREATE INDEX idx_product_store ON product (store_id);
CREATE INDEX idx_product_version ON product (version);
CREATE INDEX idx_product_discount ON product (discount_percent DESC);
CREATE INDEX idx_variant_size ON product_variant (product_id, size_norm, available);
