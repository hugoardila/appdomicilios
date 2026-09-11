USE appdomicilios;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS courier_document_type ENUM('national_id_front', 'driver_license_front') NULL AFTER city,
    ADD COLUMN IF NOT EXISTS courier_document_path VARCHAR(255) NULL AFTER courier_document_type,
    ADD COLUMN IF NOT EXISTS courier_selfie_path VARCHAR(255) NULL AFTER courier_document_path,
    ADD COLUMN IF NOT EXISTS courier_verification_submitted_at DATETIME NULL AFTER courier_selfie_path;
