USE appdomicilios;

START TRANSACTION;

DELETE FROM order_images;
DELETE FROM order_messages;
DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM wallet_movements;
DELETE FROM wallet_topups;
DELETE FROM api_tokens;

UPDATE wallet_accounts wa
JOIN users u ON u.id = wa.user_id
SET
    wa.balance = 0,
    wa.customer_free_orders_total = CASE WHEN u.role = 'customer' THEN 2 ELSE 0 END,
    wa.customer_free_orders_remaining = CASE WHEN u.role = 'customer' THEN 2 ELSE 0 END,
    wa.customer_deferred_charges = 0,
    wa.courier_free_takes_total = CASE WHEN u.role = 'courier' THEN 3 ELSE 0 END,
    wa.courier_free_takes_remaining = CASE WHEN u.role = 'courier' THEN 3 ELSE 0 END,
    wa.courier_deferred_charges = 0,
    wa.updated_at = CURRENT_TIMESTAMP;

COMMIT;

ALTER TABLE order_images AUTO_INCREMENT = 1;
ALTER TABLE order_messages AUTO_INCREMENT = 1;
ALTER TABLE order_items AUTO_INCREMENT = 1;
ALTER TABLE orders AUTO_INCREMENT = 1;
ALTER TABLE wallet_movements AUTO_INCREMENT = 1;
ALTER TABLE wallet_topups AUTO_INCREMENT = 1;
ALTER TABLE api_tokens AUTO_INCREMENT = 1;

SELECT 'users' AS table_name, COUNT(*) AS total FROM users
UNION ALL
SELECT 'api_tokens' AS table_name, COUNT(*) AS total FROM api_tokens
UNION ALL
SELECT 'wallet_accounts' AS table_name, COUNT(*) AS total FROM wallet_accounts
UNION ALL
SELECT 'wallet_movements' AS table_name, COUNT(*) AS total FROM wallet_movements
UNION ALL
SELECT 'wallet_topups' AS table_name, COUNT(*) AS total FROM wallet_topups
UNION ALL
SELECT 'orders' AS table_name, COUNT(*) AS total FROM orders
UNION ALL
SELECT 'order_items' AS table_name, COUNT(*) AS total FROM order_items
UNION ALL
SELECT 'order_messages' AS table_name, COUNT(*) AS total FROM order_messages
UNION ALL
SELECT 'order_images' AS table_name, COUNT(*) AS total FROM order_images;
