ALTER TABLE product
  ADD COLUMN source_date_known BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE product
SET source_date_known = TRUE
WHERE source_created_at IS NOT NULL;

CREATE INDEX idx_product_arrivals
  ON product (active, product_kind, in_stock DESC, source_date_known DESC, source_created_at DESC, id DESC);
