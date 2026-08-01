CREATE INDEX idx_scrape_run_started_at ON scrape_run (started_at DESC);
CREATE INDEX idx_scrape_run_store_started_at ON scrape_run (store_id, started_at DESC);
