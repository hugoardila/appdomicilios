ALTER TABLE order_messages
    ADD COLUMN IF NOT EXISTS latitude DECIMAL(10, 7) NULL AFTER image_path,
    ADD COLUMN IF NOT EXISTS longitude DECIMAL(10, 7) NULL AFTER latitude,
    ADD COLUMN IF NOT EXISTS location_label VARCHAR(255) NULL AFTER longitude;
