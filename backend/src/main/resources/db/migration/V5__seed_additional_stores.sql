INSERT INTO store (slug, name, base_url, platform)
VALUES
  ('shop-ezzy', 'SHOP EZZY', 'https://theshopezzy.com', 'SHOPIFY'),
  ('store-1914', 'STORE 1914', 'https://store1914.in', 'SHOPIFY'),
  ('gully-sports', 'GULLY SPORTS', 'https://gullysports.in', 'SHOPIFY'),
  ('campeon-sports', 'CAMPEON SPORTS', 'https://campeonsports.com', 'SHOPIFY'),
  ('jersey-hut', 'JERSEY HUT', 'https://jerseyhut.in', 'SHOPIFY'),
  ('jc-club', 'JC CLUB', 'https://jcclub.in', 'WOOCOMMERCE'),
  ('outhouse-athleisure', 'OUTHOUSE ATHLEISURE', 'https://outhouseathleisure.com', 'WOOCOMMERCE'),
  ('kitkrew', 'KITKREW', 'https://kitkrew.in', 'SHOPIFY')
ON CONFLICT (slug) DO NOTHING;
