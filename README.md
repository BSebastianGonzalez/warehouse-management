# Gestión de inventario

Gestión de inventario es una aplicación full-stack para centralizar el control de existencias en varias bodegas, registrar movimientos de inventario y procesar pedidos que pueden despacharse desde más de una bodega.

El proyecto implementa los requisitos de una distribuidora que anteriormente administraba el inventario en hojas de cálculo independientes. Incluye una API REST con Spring Boot, una interfaz web con React y persistencia en MySQL.

## Funcionalidades

- Gestión del catálogo de productos:
  - Crear, listar, consultar y actualizar productos.
  - Configurar la unidad de medida y el stock mínimo.
  - Marcar productos como descontinuados.
- Gestión del catálogo de bodegas:
  - Crear, listar, consultar y actualizar bodegas.
- Movimientos de inventario:
  - Registrar entradas.
  - Registrar salidas.
  - Trasladar existencias entre bodegas.
  - Evitar existencias negativas mediante una actualización condicional atómica.
  - Asociar cada movimiento con el administrador que lo registró.
- Pedidos:
  - Crear pedidos con una o varias líneas de productos.
  - Despachar una línea desde varias bodegas cuando sea necesario.
  - Procesar pedidos de forma síncrona.
  - Marcar pedidos como `DESPACHADO` o `CANCELADO`.
  - Registrar la cantidad faltante de las líneas canceladas.
  - Evitar despachos parciales cuando un pedido no puede completarse.
- Consulta de existencias:
  - Consultar las existencias de cada producto en cada bodega.
  - Consultar la existencia total de un producto.
  - Identificar productos por debajo de su stock mínimo configurado.
  - Comparar la proyección actual de existencias con el historial de movimientos.
- Autenticación:
  - Registrar el administrador inicial.
  - Iniciar y cerrar sesión mediante una sesión HTTP.
  - Exigir un administrador autenticado para registrar movimientos y pedidos.
  - Guardar la referencia del administrador en movimientos y pedidos.

## Reglas de negocio

El backend es la autoridad para todas las reglas de inventario:

1. Un traslado descuenta existencias en la bodega de origen y las suma en la bodega de destino.
2. Un traslado no puede utilizar la misma bodega como origen y destino.
3. Un descuento de existencias solo se acepta cuando la cantidad disponible es suficiente en el momento de la actualización.
4. Todo movimiento tiene una cantidad positiva y una configuración de bodegas válida según su tipo.
5. `INBOUND` suma existencias, `OUTBOUND` las descuenta y `TRANSFER` las mueve entre bodegas.
6. Un producto descontinuado puede despacharse, pero no puede recibir nuevas entradas ni ser trasladado.
7. Una línea de pedido puede combinar existencias de varias bodegas.
8. Si alguna línea no puede completarse, el pedido completo se cancela. No se crean despachos parciales, movimientos de salida ni cambios de existencias.
9. La existencia actual es una proyección; el historial de movimientos es la fuente de verdad utilizada para la conciliación.

## Stack tecnológico

### Backend

- Java 21
- Spring Boot
- Spring Data JPA / Hibernate
- MySQL
- Maven
- Springdoc OpenAPI

### Frontend

- React 19
- Vite
- JavaScript

El backend se ejecuta en `http://localhost:8080` y el servidor de desarrollo del frontend en `http://localhost:5173`.

## Requisitos

Instala el siguiente software antes de comenzar:

- JDK 21
- MySQL
- Node.js y npm
- Git

El repositorio incluye Maven Wrapper, por lo que no es necesario instalar Maven por separado.

## Configuración desde cero

### 1. Clonar el repositorio

```bash
git clone https://github.com/BSebastianGonzalez/warehouse-management.git
cd warehouse-management
```

### 2. Crear la base de datos MySQL

Crea la base de datos utilizada por el backend:

```sql
CREATE DATABASE warehouse;
```

La aplicación utiliza Hibernate con `spring.jpa.hibernate.ddl-auto=update` en la configuración actual de desarrollo. Las tablas requeridas se crean o actualizan cuando inicia el backend.

### 3. Configurar el backend

Revisa `backend/src/main/resources/application.properties` y configura la conexión de MySQL para tu máquina local. Como mínimo, establece:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/warehouse
spring.datasource.username=tu_usuario_mysql
spring.datasource.password=tu_contraseña_mysql
```

El repositorio ignora `backend/.env`; puede utilizarse para secretos locales cuando el entorno esté configurado para cargarlos. No subas contraseñas de base de datos ni otras credenciales.

### 4. Iniciar el backend

En Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

En macOS o Linux:

```bash
cd backend
./mvnw spring-boot:run
```

La API estará disponible en `http://localhost:8080/api`.

### 5. Iniciar el frontend

Abre una segunda terminal:

```bash
cd frontend
npm install
npm run dev
```

Abre `http://localhost:5173` en el navegador. El frontend lee la URL base del backend desde `VITE_API_URL`; si no está definida, utiliza:

```text
http://localhost:8080/api
```

Para utilizar otra URL de backend, crea `frontend/.env.local`:

```properties
VITE_API_URL=http://localhost:8080/api
```

Las peticiones autenticadas utilizan `credentials: include` para conservar la sesión HTTP entre el frontend y el backend.

## API REST

Todos los endpoints de la aplicación utilizan el prefijo `/api`. Las operaciones del catálogo de productos y bodegas son públicas. El registro de movimientos y la creación de pedidos requieren un administrador autenticado.

### Autenticación

| Método | Endpoint | Descripción | Autenticación |
|---|---|---|---|
| `POST` | `/api/auth/register` | Registrar el administrador inicial | Pública |
| `POST` | `/api/auth/login` | Iniciar una sesión de administrador | Pública |
| `POST` | `/api/auth/logout` | Cerrar la sesión actual | Sesión |

### Productos

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/products` | Listar productos |
| `GET` | `/api/products/{id}` | Consultar un producto |
| `POST` | `/api/products` | Crear un producto |
| `PUT` | `/api/products/{id}` | Actualizar un producto |
| `PATCH` | `/api/products/{id}/discontinued` | Cambiar el estado de descontinuado |

Crear o actualizar un producto no crea ni modifica existencias.

### Bodegas

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/warehouses` | Listar bodegas |
| `GET` | `/api/warehouses/{id}` | Consultar una bodega |
| `POST` | `/api/warehouses` | Crear una bodega |
| `PUT` | `/api/warehouses/{id}` | Actualizar una bodega |

### Movimientos

| Método | Endpoint | Descripción | Autenticación |
|---|---|---|---|
| `POST` | `/api/movements/inbound` | Añadir existencias a una bodega | Requerida |
| `POST` | `/api/movements/outbound` | Retirar existencias de una bodega | Requerida |
| `POST` | `/api/movements/transfers` | Trasladar existencias entre bodegas | Requerida |

La cantidad inicial de un producto en una bodega también debe registrarse como una entrada. No existe una operación CRUD directa para modificar existencias.

### Existencias

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/stocks` | Listar existencias de todos los productos y bodegas |
| `GET` | `/api/stocks/products/{productId}` | Listar las existencias de un producto por bodega |
| `GET` | `/api/stocks/products/{productId}/warehouses/{warehouseId}` | Consultar la existencia de un producto en una bodega |
| `GET` | `/api/stocks/products/{productId}/warehouses/{warehouseId}/reconciliation` | Comparar la existencia proyectada con el historial de movimientos |

### Pedidos

| Método | Endpoint | Descripción | Autenticación |
|---|---|---|---|
| `POST` | `/api/orders` | Crear y procesar un pedido de forma síncrona | Requerida |
| `GET` | `/api/orders/{id}` | Consultar estado, líneas, detalles de despacho y cantidades faltantes | Pública |

La documentación interactiva de la API está disponible mediante Springdoc cuando el backend está en ejecución:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- JSON de OpenAPI: `http://localhost:8080/v3/api-docs`

## Arquitectura

El backend es un monolito modular organizado por dominio. Cada dominio agrupa, cuando corresponde, sus controllers, services, repositories, entidades, DTOs y excepciones relacionadas.

```text
backend/src/main/java/co/wm/warehouse/
├── admin/
├── movement/
├── order/
├── product/
├── stock/
├── warehouse/
├── config/
└── error/
```

Las capas principales son:

- **Controller:** recibe las peticiones HTTP, valida los DTOs de entrada y delega la operación.
- **Service:** aplica las reglas de negocio, coordina los repositories y define los límites transaccionales.
- **Repository:** realiza las operaciones de persistencia mediante Spring Data JPA.

La aplicación React mantiene una estructura sencilla: centraliza la comunicación con la API en un helper compartido y agrupa los formularios operativos principales junto con la consulta de existencias.

## Estructura del proyecto

```text
.
├── backend/                 # API Spring Boot y pruebas
│   ├── src/main/java/
│   ├── src/main/resources/
│   └── src/test/java/
├── frontend/                # Aplicación React + Vite
│   └── src/
├── docs/adr/                # Registros de decisiones arquitectónicas
├── ASSUMPTIONS.md           # Supuestos explícitos del negocio
├── AGENTS.md                # Instrucciones internas del proyecto
└── BITACORA-IA.md           # Registro del desarrollo asistido por IA
```

## Pruebas y compilación

Ejecutar las pruebas del backend:

En Windows:

```powershell
cd backend
.\mvnw.cmd test
```

En macOS o Linux:

```bash
cd backend
./mvnw test
```

Compilar el frontend:

```bash
cd frontend
npm install
npm run build
```

Previsualizar el paquete de producción del frontend:

```bash
npm run preview
```

## Documentación

El repositorio contiene decisiones y contexto adicionales:

- [ADR de arquitectura](docs/adr/003-architecture.md): monolito modular y organización en tres capas.
- [ADR de concurrencia](docs/adr/004-concurrency.md): descuentos atómicos de existencias y límites transaccionales.
- [ADR de procesamiento de pedidos](docs/adr/005-order_processing.md): procesamiento síncrono, cancelación y despacho desde varias bodegas.
- [ADR del stack tecnológico](docs/adr/002-framework_stack.md): tecnologías seleccionadas para backend, frontend y base de datos.
- [Supuestos](ASSUMPTIONS.md): decisiones tomadas cuando el enunciado original era ambiguo.
- [Bitácora de desarrollo con IA](BITACORA-IA.md): registro del trabajo realizado con asistencia de IA.

`AGENTS.md` contiene instrucciones internas para los agentes que trabajan en el repositorio y no pretende sustituir esta documentación para desarrolladores.

## Alcance y limitaciones

El alcance actual incluye inventario, catálogos, pedidos, autenticación de administradores y la interfaz React. No incluye pagos, clientes, integraciones externas, autenticación OAuth/JWT, recuperación de contraseñas, roles diferenciados ni configuración de despliegue en producción.
