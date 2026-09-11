USE appdomicilios;

CREATE TABLE IF NOT EXISTS wallet_accounts (
    user_id BIGINT UNSIGNED PRIMARY KEY,
    balance INT NOT NULL DEFAULT 0,
    customer_free_orders_total SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    customer_free_orders_remaining SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    customer_deferred_charges INT NOT NULL DEFAULT 0,
    courier_free_takes_total SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    courier_free_takes_remaining SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    courier_deferred_charges INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_accounts_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS wallet_movements (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    order_id BIGINT UNSIGNED NULL,
    topup_id BIGINT UNSIGNED NULL,
    movement_type VARCHAR(60) NOT NULL,
    amount INT NOT NULL DEFAULT 0,
    balance_after INT NOT NULL DEFAULT 0,
    note VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_wallet_movements_user_id (user_id),
    KEY idx_wallet_movements_order_id (order_id),
    CONSTRAINT fk_wallet_movements_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS wallet_topups (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    provider ENUM('epayco') NOT NULL DEFAULT 'epayco',
    reference_code VARCHAR(80) NOT NULL,
    invoice_code VARCHAR(80) NOT NULL,
    amount INT NOT NULL,
    status ENUM('pending', 'approved', 'rejected', 'cancelled') NOT NULL DEFAULT 'pending',
    provider_transaction_id VARCHAR(120) NULL,
    provider_reference VARCHAR(120) NULL,
    response_payload LONGTEXT NULL,
    confirmed_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_wallet_topups_reference_code (reference_code),
    UNIQUE KEY uq_wallet_topups_invoice_code (invoice_code),
    KEY idx_wallet_topups_user_id (user_id),
    CONSTRAINT fk_wallet_topups_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

ALTER TABLE orders
    MODIFY COLUMN status ENUM('waiting', 'taken', 'shopping', 'on_the_way', 'delivered', 'cancelled') NOT NULL DEFAULT 'waiting',
    MODIFY COLUMN service_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS customer_fee_mode ENUM('none', 'free', 'paid') NOT NULL DEFAULT 'none' AFTER delivery_fee,
    ADD COLUMN IF NOT EXISTS customer_fee_amount INT NOT NULL DEFAULT 0 AFTER customer_fee_mode,
    ADD COLUMN IF NOT EXISTS customer_fee_reversed TINYINT(1) NOT NULL DEFAULT 0 AFTER customer_fee_amount,
    ADD COLUMN IF NOT EXISTS courier_fee_mode ENUM('none', 'free', 'paid') NOT NULL DEFAULT 'none' AFTER customer_fee_reversed,
    ADD COLUMN IF NOT EXISTS courier_fee_amount INT NOT NULL DEFAULT 0 AFTER courier_fee_mode,
    ADD COLUMN IF NOT EXISTS courier_fee_reversed TINYINT(1) NOT NULL DEFAULT 0 AFTER courier_fee_amount;

ALTER TABLE wallet_accounts
    ADD COLUMN IF NOT EXISTS customer_deferred_charges INT NOT NULL DEFAULT 0 AFTER customer_free_orders_remaining,
    ADD COLUMN IF NOT EXISTS courier_deferred_charges INT NOT NULL DEFAULT 0 AFTER courier_free_takes_remaining;

INSERT INTO wallet_accounts (
    user_id,
    balance,
    customer_free_orders_total,
    customer_free_orders_remaining,
    customer_deferred_charges,
    courier_free_takes_total,
    courier_free_takes_remaining,
    courier_deferred_charges
)
SELECT
    u.id,
    0,
    CASE WHEN u.role = 'customer' THEN 2 ELSE 0 END,
    CASE WHEN u.role = 'customer' THEN 2 ELSE 0 END,
    0,
    CASE WHEN u.role = 'courier' THEN 3 ELSE 0 END,
    CASE WHEN u.role = 'courier' THEN 3 ELSE 0 END,
    0
FROM users u
WHERE NOT EXISTS (
    SELECT 1
    FROM wallet_accounts wa
    WHERE wa.user_id = u.id
);

UPDATE wallet_accounts wa
INNER JOIN users u ON u.id = wa.user_id
SET
    wa.balance = 0,
    wa.customer_deferred_charges = 0,
    wa.courier_deferred_charges = 0,
    wa.customer_free_orders_total = CASE WHEN u.role = 'customer' THEN 2 ELSE 0 END,
    wa.customer_free_orders_remaining = CASE WHEN u.role = 'customer' THEN 2 ELSE 0 END,
    wa.courier_free_takes_total = CASE WHEN u.role = 'courier' THEN 3 ELSE 0 END,
    wa.courier_free_takes_remaining = CASE WHEN u.role = 'courier' THEN 3 ELSE 0 END
WHERE NOT EXISTS (
    SELECT 1
    FROM wallet_topups wt
    WHERE wt.user_id = wa.user_id
      AND wt.status = 'approved'
);
