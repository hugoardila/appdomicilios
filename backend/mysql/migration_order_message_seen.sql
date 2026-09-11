ALTER TABLE order_messages
    ADD COLUMN IF NOT EXISTS seen_by_customer_at DATETIME NULL AFTER location_label,
    ADD COLUMN IF NOT EXISTS seen_by_courier_at DATETIME NULL AFTER seen_by_customer_at;
