REPLACE
INTO `product` (`id`, `created_at`, `updated_at`, `product_id`, `base_currency`, `quote_currency`, `base_max_size`, `base_min_size`, `base_scale`, `quote_scale`, `quote_max_size`, `quote_min_size`, `quote_increment`, `taker_fee_rate`, `maker_fee_rate`, `display_order`)
VALUES
    (1,NULL,NULL,'BTC-USDT','BTC','USDT',10000.00,0.00,8,2,100000.00,0.00,0.0001,0,0,0);