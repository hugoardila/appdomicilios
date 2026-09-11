USE appdomicilios;

SET @customer_laura = (SELECT id FROM users WHERE email = 'laura@example.com' LIMIT 1);
SET @courier_andres = (SELECT id FROM users WHERE email = 'andres@example.com' LIMIT 1);

INSERT INTO orders (
    customer_id,
    courier_id,
    status,
    address_text,
    share_location,
    city,
    payment_method,
    service_fee,
    delivery_fee,
    original_request_text
)
SELECT
    @customer_laura,
    NULL,
    'waiting',
    'Cra 4 #12-18, barrio Los Lagos',
    1,
    'Pitalito, Huila',
    'cash',
    2500,
    6000,
    'Arroz Diana 1kg\nHuevos por cubeta\nTomates maduros'
WHERE @customer_laura IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM orders
      WHERE customer_id = @customer_laura
        AND address_text = 'Cra 4 #12-18, barrio Los Lagos'
  );

INSERT INTO orders (
    customer_id,
    courier_id,
    status,
    address_text,
    share_location,
    city,
    payment_method,
    service_fee,
    delivery_fee,
    original_request_text
)
SELECT
    @customer_laura,
    NULL,
    'waiting',
    'Mz B Casa 8, conjunto Campo Real',
    0,
    'Pitalito, Huila',
    'transfer',
    2500,
    7000,
    'Leche deslactosada\nPan integral\nBanano'
WHERE @customer_laura IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM orders
      WHERE customer_id = @customer_laura
        AND address_text = 'Mz B Casa 8, conjunto Campo Real'
  );

INSERT INTO orders (
    customer_id,
    courier_id,
    status,
    address_text,
    share_location,
    city,
    payment_method,
    service_fee,
    delivery_fee,
    original_request_text
)
SELECT
    @customer_laura,
    NULL,
    'waiting',
    'Calle 7 #3-25, centro',
    1,
    'Pitalito, Huila',
    'digital',
    2500,
    5000,
    'Acetaminofen 500mg\nVitamina C\nAgua micelar'
WHERE @customer_laura IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM orders
      WHERE customer_id = @customer_laura
        AND address_text = 'Calle 7 #3-25, centro'
  );

INSERT INTO orders (
    customer_id,
    courier_id,
    status,
    address_text,
    share_location,
    city,
    payment_method,
    service_fee,
    delivery_fee,
    original_request_text
)
SELECT
    @customer_laura,
    NULL,
    'waiting',
    'Calle 10 #6-14, barrio Cundinamarca',
    1,
    'Pitalito, Huila',
    'cash',
    2500,
    5500,
    'Queso campesino\nArepas boyacenses\nCafe molido'
WHERE @customer_laura IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM orders
      WHERE customer_id = @customer_laura
        AND address_text = 'Calle 10 #6-14, barrio Cundinamarca'
  );

INSERT INTO orders (
    customer_id,
    courier_id,
    status,
    address_text,
    share_location,
    city,
    payment_method,
    service_fee,
    delivery_fee,
    original_request_text
)
SELECT
    @customer_laura,
    NULL,
    'waiting',
    'Cra 1 #9-30, barrio Trinidad',
    0,
    'Pitalito, Huila',
    'transfer',
    2500,
    6500,
    'Detergente en polvo\nSuavizante\nPapel higienico'
WHERE @customer_laura IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM orders
      WHERE customer_id = @customer_laura
        AND address_text = 'Cra 1 #9-30, barrio Trinidad'
  );

INSERT INTO orders (
    customer_id,
    courier_id,
    status,
    address_text,
    share_location,
    city,
    payment_method,
    service_fee,
    delivery_fee,
    original_request_text
)
SELECT
    @customer_laura,
    NULL,
    'waiting',
    'Vereda Guacacallo, lote 3',
    1,
    'Pitalito, Huila',
    'digital',
    2500,
    9000,
    'Alimento para perro\nShampoo\nPan tajado'
WHERE @customer_laura IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM orders
      WHERE customer_id = @customer_laura
        AND address_text = 'Vereda Guacacallo, lote 3'
  );

INSERT INTO order_items (order_id, name, status)
SELECT order_id, item_name, item_status
FROM (
    SELECT
        (SELECT id FROM orders WHERE address_text = 'Cra 4 #12-18, barrio Los Lagos' LIMIT 1) AS order_id,
        'Arroz Diana 1kg' AS item_name,
        'pending' AS item_status
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Cra 4 #12-18, barrio Los Lagos' LIMIT 1), 'Huevos por cubeta', 'pending'
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Cra 4 #12-18, barrio Los Lagos' LIMIT 1), 'Tomates maduros', 'pending'
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Mz B Casa 8, conjunto Campo Real' LIMIT 1), 'Leche deslactosada', 'pending'
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Mz B Casa 8, conjunto Campo Real' LIMIT 1), 'Pan integral', 'pending'
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Mz B Casa 8, conjunto Campo Real' LIMIT 1), 'Banano', 'pending'
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Calle 7 #3-25, centro' LIMIT 1), 'Acetaminofen 500mg', 'pending'
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Calle 7 #3-25, centro' LIMIT 1), 'Vitamina C', 'pending'
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Calle 7 #3-25, centro' LIMIT 1), 'Agua micelar', 'pending'
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Calle 10 #6-14, barrio Cundinamarca' LIMIT 1), 'Queso campesino', 'pending'
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Calle 10 #6-14, barrio Cundinamarca' LIMIT 1), 'Arepas boyacenses', 'pending'
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Calle 10 #6-14, barrio Cundinamarca' LIMIT 1), 'Cafe molido', 'pending'
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Cra 1 #9-30, barrio Trinidad' LIMIT 1), 'Detergente en polvo', 'pending'
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Cra 1 #9-30, barrio Trinidad' LIMIT 1), 'Suavizante', 'pending'
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Cra 1 #9-30, barrio Trinidad' LIMIT 1), 'Papel higienico', 'pending'
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Vereda Guacacallo, lote 3' LIMIT 1), 'Alimento para perro', 'pending'
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Vereda Guacacallo, lote 3' LIMIT 1), 'Shampoo', 'pending'
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Vereda Guacacallo, lote 3' LIMIT 1), 'Pan tajado', 'pending'
) AS seed_items
WHERE order_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM order_items oi
      WHERE oi.order_id = seed_items.order_id
        AND oi.name = seed_items.item_name
  );

INSERT INTO order_messages (order_id, sender_user_id, sender_name, message_text)
SELECT order_id, NULL, 'Sistema', message_text
FROM (
    SELECT (SELECT id FROM orders WHERE address_text = 'Cra 4 #12-18, barrio Los Lagos' LIMIT 1) AS order_id, 'Pedido creado y esperando un domiciliario.' AS message_text
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Mz B Casa 8, conjunto Campo Real' LIMIT 1), 'Pedido creado y esperando un domiciliario.'
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Calle 7 #3-25, centro' LIMIT 1), 'Pedido creado y esperando un domiciliario.'
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Calle 10 #6-14, barrio Cundinamarca' LIMIT 1), 'Pedido creado y esperando un domiciliario.'
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Cra 1 #9-30, barrio Trinidad' LIMIT 1), 'Pedido creado y esperando un domiciliario.'
    UNION ALL
    SELECT (SELECT id FROM orders WHERE address_text = 'Vereda Guacacallo, lote 3' LIMIT 1), 'Pedido creado y esperando un domiciliario.'
) AS seed_messages
WHERE order_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM order_messages om
      WHERE om.order_id = seed_messages.order_id
        AND om.message_text = seed_messages.message_text
  );
