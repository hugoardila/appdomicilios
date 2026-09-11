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
