# App Domicilios

Base inicial de una app Android para domicilios con dos roles:

- Cliente: crea pedidos libres por texto, agrega fotos y revisa el estado.
- Domiciliario: toma hasta 3 pedidos, marca productos y chatea con el cliente.

## Estado actual

Este scaffold incluye:

- Navegacion Compose para demo por rol.
- Flujo base de pedidos con datos simulados.
- Pantalla de crear pedido.
- Pantallas separadas para cliente y domiciliario.
- Detalle del pedido con chat y avance de estados.
- Login y registro conectados a una API PHP local sobre XAMPP.
- Base de datos MySQL/MariaDB con tablas para usuarios, pedidos, items, mensajes e imagenes.

## Backend local

- Base de datos: `appdomicilios`
- Script SQL: [backend/mysql/schema.sql](backend/mysql/schema.sql)
- Semilla de pedidos de prueba: [backend/mysql/seed_orders.sql](backend/mysql/seed_orders.sql)
- API PHP: [backend/php-api](backend/php-api)
- Publicacion esperada en XAMPP: `C:\xampp\htdocs\appdomicilios_api`

## Endpoints activos

- `GET /appdomicilios_api/api/health.php`
- `POST /appdomicilios_api/api/login.php`
- `POST /appdomicilios_api/api/register.php`
- `GET /appdomicilios_api/api/orders.php`
- `GET /appdomicilios_api/api/order_detail.php`
- `POST /appdomicilios_api/api/create_order.php`
- `POST /appdomicilios_api/api/take_order.php`
- `POST /appdomicilios_api/api/update_item_status.php`
- `POST /appdomicilios_api/api/update_order_status.php`
- `POST /appdomicilios_api/api/add_order_item.php`

## Credenciales demo en MySQL

- Cliente: `laura@example.com / cliente123`
- Domiciliario: `andres@example.com / domi123`

## Prueba en Android

- En emulador, la app ya apunta a `http://10.0.2.2/appdomicilios_api/api/`.
- En telefono fisico debes cambiar `API_BASE_URL` en [app/build.gradle.kts](app/build.gradle.kts) por la IP local del computador.
- IP local actual detectada: `192.168.1.6`
- La app ya intenta ambas rutas: emulador (`10.0.2.2`) y red local (`192.168.1.6`).
- Login, registro, crear pedido, listar pedidos, tomar pedido, actualizar estado del pedido y actualizar items ya pegan a MySQL.
- El chat y las fotos todavia no estan conectados al backend.

## Siguiente paso recomendado

1. Conectar chat real a `order_messages`.
2. Subir fotos a un endpoint PHP y guardarlas en `uploads/`.
3. Proteger endpoints sensibles con token.
4. Crear un panel admin simple para aprobar domiciliarios.
