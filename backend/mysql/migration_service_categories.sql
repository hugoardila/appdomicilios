ALTER TABLE orders
    ADD COLUMN service_category ENUM('shopping', 'domicilios', 'tramites', 'mototaxi', 'envios') NOT NULL DEFAULT 'shopping' AFTER courier_id;

UPDATE orders
SET service_category = CASE request_type
    WHEN 'pickup_delivery' THEN 'domicilios'
    WHEN 'other' THEN 'tramites'
    ELSE 'shopping'
END
WHERE service_category = 'shopping' OR service_category IS NULL;
