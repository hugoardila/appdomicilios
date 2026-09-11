USE appdomicilios;

ALTER TABLE orders
    MODIFY COLUMN service_category
    ENUM('shopping', 'restaurantes', 'domicilios', 'tramites', 'mototaxi', 'envios')
    NOT NULL DEFAULT 'shopping';
