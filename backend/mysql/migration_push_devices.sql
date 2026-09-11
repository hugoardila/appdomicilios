USE appdomicilios;

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
