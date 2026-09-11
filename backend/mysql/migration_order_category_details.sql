USE appdomicilios;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS service_subcategory VARCHAR(120) NULL AFTER service_category,
    ADD COLUMN IF NOT EXISTS destination_latitude DECIMAL(10, 7) NULL AFTER longitude,
    ADD COLUMN IF NOT EXISTS destination_longitude DECIMAL(10, 7) NULL AFTER destination_latitude,
    ADD COLUMN IF NOT EXISTS destination_reference VARCHAR(255) NULL AFTER destination_longitude,
    ADD COLUMN IF NOT EXISTS package_weight_kg DECIMAL(10, 2) NULL AFTER pickup_payment_amount;

ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS quantity INT UNSIGNED NOT NULL DEFAULT 1 AFTER name;
