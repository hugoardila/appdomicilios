ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS purchase_places_count SMALLINT UNSIGNED NOT NULL DEFAULT 1
    AFTER delivery_fee;

UPDATE orders
SET purchase_places_count = 1
WHERE purchase_places_count IS NULL OR purchase_places_count < 1;

UPDATE orders o
LEFT JOIN (
    SELECT order_id, COUNT(*) AS product_count
    FROM order_items
    GROUP BY order_id
) items ON items.order_id = o.id
SET o.delivery_fee = CASE
    WHEN COALESCE(items.product_count, 0) <= 1 THEN 5000
    ELSE 7000
END
WHERE o.status <> 'cancelled';
