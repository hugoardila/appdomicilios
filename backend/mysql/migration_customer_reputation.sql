USE appdomicilios;

ALTER TABLE users
    ADD COLUMN customer_reputation_score DECIMAL(3,2) NOT NULL DEFAULT 5.00 AFTER city,
    ADD COLUMN customer_incidents_count SMALLINT UNSIGNED NOT NULL DEFAULT 0 AFTER customer_reputation_score;

ALTER TABLE orders
    ADD COLUMN resolution_reason ENUM('delivered', 'customer_not_found', 'customer_noncompliance', 'cancelled_by_customer') NULL AFTER courier_fee_reversed,
    ADD COLUMN resolved_by_user_id BIGINT UNSIGNED NULL AFTER resolution_reason,
    ADD COLUMN resolved_at DATETIME NULL AFTER resolved_by_user_id,
    ADD CONSTRAINT fk_orders_resolved_by_user
        FOREIGN KEY (resolved_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS customer_reputation_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT UNSIGNED NOT NULL,
    customer_id BIGINT UNSIGNED NOT NULL,
    courier_id BIGINT UNSIGNED NOT NULL,
    event_reason ENUM('customer_not_found', 'customer_noncompliance') NOT NULL,
    score_before DECIMAL(3,2) NOT NULL,
    score_after DECIMAL(3,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_customer_reputation_events_order (order_id),
    KEY idx_customer_reputation_events_customer_id (customer_id),
    KEY idx_customer_reputation_events_courier_id (courier_id),
    CONSTRAINT fk_customer_reputation_events_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_customer_reputation_events_customer
        FOREIGN KEY (customer_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_customer_reputation_events_courier
        FOREIGN KEY (courier_id) REFERENCES users(id)
        ON DELETE CASCADE
);
