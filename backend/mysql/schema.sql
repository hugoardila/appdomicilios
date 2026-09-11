CREATE DATABASE IF NOT EXISTS appdomicilios
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE appdomicilios;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    role ENUM('customer', 'courier') NOT NULL,
    approval_status ENUM('pending', 'approved', 'rejected') NOT NULL DEFAULT 'approved',
    full_name VARCHAR(120) NOT NULL,
    national_id VARCHAR(40) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    city VARCHAR(80) NOT NULL DEFAULT 'Pitalito, Huila',
    customer_reputation_score DECIMAL(3,2) NOT NULL DEFAULT 5.00,
    customer_incidents_count SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    courier_document_type ENUM('national_id_front', 'driver_license_front') NULL,
    courier_document_path VARCHAR(255) NULL,
    courier_selfie_path VARCHAR(255) NULL,
    courier_verification_submitted_at DATETIME NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_users_national_id (national_id),
    UNIQUE KEY uq_users_phone (phone),
    UNIQUE KEY uq_users_email (email)
);

CREATE TABLE IF NOT EXISTS api_tokens (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    token CHAR(64) NOT NULL,
    expires_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_api_tokens_token (token),
    KEY idx_api_tokens_user_id (user_id),
    CONSTRAINT fk_api_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS push_devices (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    app_instance_id VARCHAR(80) NOT NULL,
    platform VARCHAR(20) NOT NULL DEFAULT 'android',
    fcm_token VARCHAR(255) NOT NULL,
    device_label VARCHAR(120) NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    last_seen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_push_devices_app_instance (app_instance_id),
    UNIQUE KEY uq_push_devices_fcm_token (fcm_token),
    KEY idx_push_devices_user_id (user_id),
    KEY idx_push_devices_active (is_active),
    CONSTRAINT fk_push_devices_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
);

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

CREATE TABLE IF NOT EXISTS service_zones (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    city_label VARCHAR(120) NOT NULL,
    center_latitude DECIMAL(10, 7) NOT NULL,
    center_longitude DECIMAL(10, 7) NOT NULL,
    radius_meters INT NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_service_zones_name (name)
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT UNSIGNED NOT NULL,
    courier_id BIGINT UNSIGNED NULL,
    service_category ENUM('shopping', 'restaurantes', 'domicilios', 'tramites', 'mototaxi', 'envios') NOT NULL DEFAULT 'shopping',
    service_subcategory VARCHAR(120) NULL,
    request_type ENUM('shopping', 'pickup_delivery', 'other') NOT NULL DEFAULT 'shopping',
    status ENUM('waiting', 'taken', 'shopping', 'on_the_way', 'delivered', 'cancelled') NOT NULL DEFAULT 'waiting',
    address_text VARCHAR(255) NOT NULL,
    address_reference VARCHAR(255) NULL,
    latitude DECIMAL(10, 7) NULL,
    longitude DECIMAL(10, 7) NULL,
    destination_latitude DECIMAL(10, 7) NULL,
    destination_longitude DECIMAL(10, 7) NULL,
    destination_reference VARCHAR(255) NULL,
    share_location TINYINT(1) NOT NULL DEFAULT 0,
    pickup_address VARCHAR(255) NULL,
    pickup_contact_name VARCHAR(120) NULL,
    pickup_contact_phone VARCHAR(30) NULL,
    dropoff_contact_name VARCHAR(120) NULL,
    dropoff_contact_phone VARCHAR(30) NULL,
    pickup_payment_amount INT NOT NULL DEFAULT 0,
    package_weight_kg DECIMAL(10, 2) NULL,
    city VARCHAR(80) NOT NULL DEFAULT 'Pitalito, Huila',
    payment_method ENUM('cash', 'transfer', 'digital') NOT NULL DEFAULT 'cash',
    service_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    delivery_fee DECIMAL(10, 2) NOT NULL DEFAULT 5000.00,
    purchase_places_count SMALLINT UNSIGNED NOT NULL DEFAULT 1,
    customer_fee_mode ENUM('none', 'free', 'paid') NOT NULL DEFAULT 'none',
    customer_fee_amount INT NOT NULL DEFAULT 0,
    customer_fee_reversed TINYINT(1) NOT NULL DEFAULT 0,
    courier_fee_mode ENUM('none', 'free', 'paid') NOT NULL DEFAULT 'none',
    courier_fee_amount INT NOT NULL DEFAULT 0,
    courier_fee_reversed TINYINT(1) NOT NULL DEFAULT 0,
    resolution_reason ENUM('delivered', 'customer_not_found', 'customer_noncompliance', 'cancelled_by_customer') NULL,
    resolved_by_user_id BIGINT UNSIGNED NULL,
    resolved_at DATETIME NULL,
    original_request_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_orders_customer_id (customer_id),
    KEY idx_orders_courier_id (courier_id),
    KEY idx_orders_status (status),
    CONSTRAINT fk_orders_customer
        FOREIGN KEY (customer_id) REFERENCES users(id),
    CONSTRAINT fk_orders_courier
        FOREIGN KEY (courier_id) REFERENCES users(id)
        ON DELETE SET NULL,
    CONSTRAINT fk_orders_resolved_by_user
        FOREIGN KEY (resolved_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(255) NOT NULL,
    quantity INT UNSIGNED NOT NULL DEFAULT 1,
    status ENUM('pending', 'purchased', 'not_found') NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_order_items_order_id (order_id),
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS order_messages (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT UNSIGNED NOT NULL,
    sender_user_id BIGINT UNSIGNED NULL,
    sender_name VARCHAR(120) NOT NULL,
    message_text TEXT NULL,
    image_path VARCHAR(255) NULL,
    latitude DECIMAL(10, 7) NULL,
    longitude DECIMAL(10, 7) NULL,
    location_label VARCHAR(255) NULL,
    seen_by_customer_at DATETIME NULL,
    seen_by_courier_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_order_messages_order_id (order_id),
    KEY idx_order_messages_sender_user_id (sender_user_id),
    CONSTRAINT fk_order_messages_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_order_messages_sender
        FOREIGN KEY (sender_user_id) REFERENCES users(id)
        ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS order_images (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT UNSIGNED NOT NULL,
    uploader_user_id BIGINT UNSIGNED NULL,
    image_type ENUM('reference', 'chat', 'proof') NOT NULL DEFAULT 'reference',
    file_path VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_order_images_order_id (order_id),
    KEY idx_order_images_uploader_user_id (uploader_user_id),
    CONSTRAINT fk_order_images_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_order_images_uploader
        FOREIGN KEY (uploader_user_id) REFERENCES users(id)
        ON DELETE SET NULL
);

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

INSERT INTO service_zones (
    name,
    city_label,
    center_latitude,
    center_longitude,
    radius_meters,
    is_active
)
VALUES (
    'Pitalito urbano',
    'Pitalito, Huila',
    1.8537000,
    -76.0507000,
    10000,
    1
)
ON DUPLICATE KEY UPDATE
    city_label = VALUES(city_label),
    center_latitude = VALUES(center_latitude),
    center_longitude = VALUES(center_longitude),
    radius_meters = VALUES(radius_meters),
    is_active = VALUES(is_active),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO users (
    role,
    approval_status,
    full_name,
    national_id,
    phone,
    email,
    password_hash,
    city
)
VALUES
    (
        'customer',
        'approved',
        'Laura Trujillo',
        '1111222333',
        '3100000001',
        'laura@example.com',
        '$2y$10$qxnFwJSgNezuNQv7Hv1i7OL8/qiY97YGdoz0g689EnHprt1j.FPaG',
        'Pitalito, Huila'
    ),
    (
        'courier',
        'approved',
        'Andres Imbachi',
        '1222333444',
        '3100000002',
        'andres@example.com',
        '$2y$10$4F6KK5MIuAczxnyOdOoZ/..OGAhi94.xWSuJ6B9eSoqHaNUwu4IIG',
        'Pitalito, Huila'
    )
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    phone = VALUES(phone),
    email = VALUES(email),
    password_hash = VALUES(password_hash),
    approval_status = VALUES(approval_status),
    updated_at = CURRENT_TIMESTAMP;

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
    CASE
        WHEN u.role = 'customer' THEN 0
        ELSE 0
    END AS balance,
    CASE
        WHEN u.role = 'customer' THEN 2
        ELSE 0
    END AS customer_free_orders_total,
    CASE
        WHEN u.role = 'customer' THEN 2
        ELSE 0
    END AS customer_free_orders_remaining,
    0 AS customer_deferred_charges,
    CASE
        WHEN u.role = 'courier' THEN 3
        ELSE 0
    END AS courier_free_takes_total,
    CASE
        WHEN u.role = 'courier' THEN 3
        ELSE 0
    END AS courier_free_takes_remaining,
    0 AS courier_deferred_charges
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM wallet_accounts wa WHERE wa.user_id = u.id
);
