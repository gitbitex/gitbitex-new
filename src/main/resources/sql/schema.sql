

CREATE TABLE IF NOT EXISTS `account` (
                           `id` bigint(20) NOT NULL AUTO_INCREMENT,
                           `created_at` datetime DEFAULT NULL,
                           `updated_at` datetime DEFAULT NULL,
                           `user_id` varchar(255) NOT NULL DEFAULT '',
                           `currency` varchar(255) NOT NULL DEFAULT '',
                           `available` decimal(32,16) NOT NULL DEFAULT '0.00000000',
                           `hold` decimal(32,16) NOT NULL DEFAULT '0.00000000',
                           PRIMARY KEY (`id`),
                           UNIQUE KEY `uk_user_id_currency` (`user_id`,`currency`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS `app` (
                       `id` bigint(20) NOT NULL AUTO_INCREMENT,
                       `created_at` datetime DEFAULT NULL,
                       `updated_at` datetime DEFAULT NULL,
                       `app_id` varchar(255) NOT NULL DEFAULT '',
                       `user_id` varchar(255) NOT NULL DEFAULT '',
                       `name` varchar(255) DEFAULT NULL,
                       `access_key` varchar(255) NOT NULL DEFAULT '',
                       `secret_key` varchar(255) NOT NULL DEFAULT '',
                       PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



CREATE TABLE IF NOT EXISTS `bill` (
                        `id` bigint(20) NOT NULL AUTO_INCREMENT,
                        `created_at` datetime DEFAULT NULL,
                        `updated_at` datetime DEFAULT NULL,
                        `bill_id` varchar(255) NOT NULL DEFAULT '',
                        `user_id` varchar(255) NOT NULL DEFAULT '',
                        `currency` varchar(255) NOT NULL DEFAULT '',
                        `available_increment` decimal(32,16) NOT NULL DEFAULT '0.00000000',
                        `hold_increment` decimal(32,16) NOT NULL DEFAULT '0.00000000',
                        `notes` varchar(255) DEFAULT NULL,
                        `settled` bit(1) NOT NULL,
                        `type` varchar(255) DEFAULT NULL,
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_bill_id` (`bill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



CREATE TABLE IF NOT EXISTS `candle` (
                          `id` bigint(20) NOT NULL AUTO_INCREMENT,
                          `created_at` datetime DEFAULT NULL,
                          `updated_at` datetime DEFAULT NULL,
                          `product_id` varchar(255) NOT NULL DEFAULT '',
                          `granularity` int(11) NOT NULL,
                          `open` decimal(32,16) NOT NULL DEFAULT '0.00000000',
                          `close` decimal(32,16) NOT NULL DEFAULT '0.00000000',
                          `high` decimal(32,16) NOT NULL DEFAULT '0.00000000',
                          `low` decimal(32,16) NOT NULL DEFAULT '0.00000000',
                          `volume` decimal(32,16) NOT NULL DEFAULT '0.00000000',
                          `time` bigint(20) NOT NULL,
                          `trade_id` bigint(20) NOT NULL,
                          `order_book_log_offset` bigint(20) NOT NULL,
                          PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS `fill` (
                        `id` bigint(20) NOT NULL AUTO_INCREMENT,
                        `created_at` datetime DEFAULT NULL,
                        `updated_at` datetime DEFAULT NULL,
                        `fill_id` varchar(255) NOT NULL DEFAULT '',
                        `order_id` varchar(255) DEFAULT NULL,
                        `product_id` varchar(255) DEFAULT NULL,
                        `fee` decimal(32,16) DEFAULT NULL,
                        `side` int(11) DEFAULT NULL,
                        `size` decimal(32,16) NOT NULL DEFAULT '0.00000000',
                        `price` decimal(32,16) DEFAULT NULL,
                        `funds` decimal(32,16) NOT NULL DEFAULT '0.00000000',
                        `liquidity` varchar(255) DEFAULT NULL,
                        `settled` bit(1) NOT NULL,
                        `trade_id` bigint(20) NOT NULL,
                        `done` bit(1) NOT NULL,
                        `done_reason` varchar(255) DEFAULT NULL,
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_fill_id` (`fill_id`),
                        UNIQUE KEY `uk_order_id_trade_id` (`order_id`,`trade_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



CREATE TABLE IF NOT EXISTS `order` (
                         `id` bigint(20) NOT NULL AUTO_INCREMENT,
                         `created_at` datetime DEFAULT NULL,
                         `updated_at` datetime DEFAULT NULL,
                         `order_id` varchar(255) DEFAULT NULL,
                         `client_oid` varchar(255) DEFAULT NULL,
                         `user_id` varchar(255) DEFAULT NULL,
                         `product_id` varchar(255) DEFAULT NULL,
                         `side` varchar(255) DEFAULT NULL,
                         `type` varchar(255) DEFAULT NULL,
                         `size` decimal(32,16) NOT NULL DEFAULT '0.00000000',
                         `filled_size` decimal(32,16) NOT NULL DEFAULT '0.00000000',
                         `price` decimal(32,16) NOT NULL DEFAULT '0.00000000',
                         `funds` decimal(32,16) NOT NULL DEFAULT '0.00000000',
                         `executed_value` decimal(32,16) NOT NULL DEFAULT '0.00000000',
                         `fill_fees` decimal(32,16) NOT NULL DEFAULT '0.00000000',
                         `status` varchar(255) NOT NULL DEFAULT '',
                         `settled` bit(1) NOT NULL,
                         `time_in_force` varchar(255) DEFAULT NULL,
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `uk_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



CREATE TABLE IF NOT EXISTS `product` (
                           `id` bigint(20) NOT NULL AUTO_INCREMENT,
                           `created_at` datetime DEFAULT NULL,
                           `updated_at` datetime DEFAULT NULL,
                           `product_id` varchar(255) NOT NULL DEFAULT '',
                           `base_currency` varchar(255) NOT NULL DEFAULT '',
                           `quote_currency` varchar(255) NOT NULL DEFAULT '',
                           `base_max_size` decimal(19,2) NOT NULL DEFAULT '0.00',
                           `base_min_size` decimal(19,2) NOT NULL DEFAULT '0.00',
                           `base_scale` int(11) NOT NULL,
                           `quote_scale` int(11) NOT NULL,
                           `quote_max_size` decimal(19,2) NOT NULL DEFAULT '0.00',
                           `quote_min_size` decimal(19,2) NOT NULL DEFAULT '0.00',
                           `quote_increment` float NOT NULL,
                           `taker_fee_rate` float NOT NULL,
                           `maker_fee_rate` float NOT NULL,
                           `display_order` int(11) NOT NULL,
                           PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS `trade` (
                         `id` bigint(20) NOT NULL AUTO_INCREMENT,
                         `created_at` datetime DEFAULT NULL,
                         `updated_at` datetime DEFAULT NULL,
                         `trade_id` bigint(20) NOT NULL,
                         `product_id` varchar(255) DEFAULT NULL,
                         `taker_order_id` varchar(255) DEFAULT NULL,
                         `maker_order_id` varchar(255) DEFAULT NULL,
                         `side` varchar(255) NOT NULL,
                         `price` decimal(32,16) DEFAULT NULL,
                         `size` decimal(32,16) NOT NULL,
                         `time` datetime DEFAULT NULL,
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `uk_product_id_trade_id` (`product_id`,`trade_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



CREATE TABLE IF NOT EXISTS `user` (
                        `id` bigint(20) NOT NULL AUTO_INCREMENT,
                        `created_at` datetime DEFAULT NULL,
                        `updated_at` datetime DEFAULT NULL,
                        `user_id` varchar(255) DEFAULT NULL,
                        `email` varchar(255) DEFAULT NULL,
                        `nick_name` varchar(255) DEFAULT NULL,
                        `password_hash` varchar(255) DEFAULT NULL,
                        `password_salt` varchar(255) DEFAULT NULL,
                        `two_step_verification_type` varchar(255) DEFAULT NULL,
                        `gotp_secret` decimal(19,2) DEFAULT NULL,
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_user_id` (`user_id`),
                        UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
