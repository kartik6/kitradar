INSERT INTO store (slug, name, base_url, platform)
VALUES
  ('goal-thread', 'GOAL THREAD', 'https://www.goalthread.co.in', 'DOTPE'),
  ('full-time-store', 'FULL TIME STORE', 'https://fulltimestore.in', 'SHOPIFY'),
  ('the-football-frenzy', 'THE FOOTBALL FRENZY', 'https://www.thefootballfrenzy.com', 'SHOPIFY')
ON CONFLICT (slug) DO NOTHING;
