USE appdomicilios;

CREATE TABLE IF NOT EXISTS courier_ratings (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT UNSIGNED NOT NULL,
    customer_id BIGINT UNSIGNED NOT NULL,
    courier_id BIGINT UNSIGNED NOT NULL,
    rating_value TINYINT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_courier_ratings_order_id (order_id),
    KEY idx_courier_ratings_customer_id (customer_id),
    KEY idx_courier_ratings_courier_id (courier_id),
    CONSTRAINT fk_courier_ratings_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_courier_ratings_customer
        FOREIGN KEY (customer_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_courier_ratings_courier
        FOREIGN KEY (courier_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_courier_ratings_value
        CHECK (rating_value BETWEEN 1 AND 5)
);
