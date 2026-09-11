ALTER TABLE orders
    ADD COLUMN service_category ENUM('shopping', 'domicilios', 'tramites', 'mototaxi', 'envios') NOT NULL DEFAULT 'shopping' AFTER courier_id,
    ADD COLUMN request_type ENUM('shopping', 'pickup_delivery', 'other') NOT NULL DEFAULT 'shopping' AFTER courier_id,
    ADD COLUMN pickup_address VARCHAR(255) NULL AFTER share_location,
    ADD COLUMN pickup_contact_name VARCHAR(120) NULL AFTER pickup_address,
    ADD COLUMN pickup_contact_phone VARCHAR(30) NULL AFTER pickup_contact_name,
    ADD COLUMN dropoff_contact_name VARCHAR(120) NULL AFTER pickup_contact_phone,
    ADD COLUMN dropoff_contact_phone VARCHAR(30) NULL AFTER dropoff_contact_name,
    ADD COLUMN pickup_payment_amount INT NOT NULL DEFAULT 0 AFTER dropoff_contact_phone;

UPDATE orders
SET service_category = CASE request_type
    WHEN 'pickup_delivery' THEN 'domicilios'
    WHEN 'other' THEN 'tramites'
    ELSE 'shopping'
END
WHERE service_category = 'shopping' OR service_category IS NULL;
