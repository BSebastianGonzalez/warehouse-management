# 1. Contexto del negocio

Una distribuidora opera con tres bodegas desde las cuales despacha pedidos. Actualmente el inventario se lleva en hojas de cálculo independientes por bodega, lo que provoca que el saldo real nunca cuadre entre lo registrado y lo físico.

El sistema debe centralizar el control de inventario multi-bodega, permitiendo registrar movimientos (entradas, salidas y traslados), procesar pedidos que pueden despacharse desde varias bodegas, y consultar en todo momento cuánto stock hay de cada producto y en qué bodega, señalando cuáles están por debajo del mínimo configurado.

# 2. Alcance

**Dentro del alcance:**
- Persistencia de datos (productos, bodegas, movimientos, pedidos, administradores).
- Interfaz para operar el sistema (registrar productos/bodegas, registrar movimientos, registrar pedidos, consultar existencias).
- Las reglas de negocio descritas abajo.
- La consulta obligatoria de existencias por producto y bodega.
- Login simple de un `Administrador` (sin roles ni permisos diferenciados).

**Fuera de alcance:**
- Autenticación avanzada (OAuth, JWT, recuperación de contraseña, roles).
- Pagos reales.
- Despliegue en producción.
- Integraciones externas.
- Gestión de clientes (no existe entidad `Cliente`; el `Administrador` registra los pedidos).

# 3. Stack tecnológico

- Backend: Java + Spring Boot.
- Persistencia: Spring Data JPA / Hibernate.
- Base de datos: MySQL.
- Frontend: React.
- Comunicación entre frontend y backend: API REST.
- Gestión de dependencias: Maven.

# 4 . Arquitectura del sistema

# Arquitectura

El backend utilizará una arquitectura monolítica modular con separación en tres capas principales:

- Controller: recibe las solicitudes HTTP y delega la operación.
- Service: contiene las reglas de negocio y coordina las operaciones.
- Repository: gestiona el acceso a datos mediante Spring Data JPA.

Los paquetes se organizarán principalmente por dominio funcional, por ejemplo:

- producto/
- bodega/
- movimiento/
- pedido/

Las reglas de negocio no deben implementarse directamente en los Controllers. Mayor informacion puedes consultarla en docs\adr\003-architecture.md

# 5. Reglas de negocio

1. Un traslado entre bodegas descuenta la cantidad en la bodega de origen y la suma en la bodega de destino, sin que la existencia de origen quede negativa.
2. Un pedido puede despacharse desde varias bodegas si ninguna tiene por sí sola la cantidad completa solicitada en una línea.
3. Todo movimiento de inventario queda registrado (`Movimiento`) de forma que el saldo actual (`Existencia`) se pueda reconstruir sumando el historial.
4. Un producto marcado como `descontinuado` admite salidas (incluyendo despachos de pedidos) pero no admite entradas nuevas, ya sea por `ENTRADA` o por `TRASLADO` (ver asunción correspondiente).

## Requisitos funcionales

### Gestión de catálogo
- Registrar, consultar, actualizar productos (incluyendo `stock_minimo` y marcar/desmarcar `descontinuado`).
- Registrar, consultar, actualizar bodegas.

### Movimientos de inventario
- Registrar una `ENTRADA` de producto en una bodega (rechazada si el producto está `descontinuado`).
- Registrar una `SALIDA` de producto desde una bodega.
- Registrar un `TRASLADO` entre dos bodegas (rechazado si el producto está `descontinuado`, y si la bodega de origen no tiene existencia suficiente).
- Todo movimiento queda asociado al `Administrador` que lo registró.

### Pedidos
- Registrar un pedido con una o varias líneas (producto + cantidad solicitada).
- El sistema evalúa el pedido de forma síncrona al registrarse (ver sección "Procesamiento de pedidos") y determina si queda `DESPACHADO` o `CANCELADO`.
- Consultar el detalle de un pedido, incluyendo desde qué bodegas se despachó cada línea (`DespachoDetalle`) o, si fue cancelado, la `cantidad_faltante` por línea.

### Consulta obligatoria
- Mostrar, para cada producto, la existencia en cada bodega.
- Señalar los productos cuya existencia total (suma de todas las bodegas) está por debajo de `stock_minimo`.

### Autenticación
- Login de `Administrador` mediante `username`/`password`.
- Las operaciones de escritura (movimientos, pedidos) requieren sesión iniciada.

## Invariantes del inventario

- `Existencia.cantidad` nunca puede ser negativa.
- La cantidad de un `Movimiento` siempre debe ser positiva.
- Una `ENTRADA` tiene bodega destino y no tiene bodega origen.
- Una `SALIDA` tiene bodega origen y no tiene bodega destino.
- Un `TRASLADO` tiene bodega origen y bodega destino.
- La bodega de origen y destino de un traslado deben ser diferentes.
- Las modificaciones de `Existencia` no deben realizarse directamente desde un CRUD.
- Todo cambio de inventario debe generar su `Movimiento` correspondiente.
- La actualización de `Existencia` y el registro de `Movimiento` deben pertenecer a la misma transacción.

# 6. Modelo de datos

## Entidades

### Producto

| Campo           | Descripción                                                                                             |
| --------------- | ------------------------------------------------------------------------------------------------------- |
| `id`            | Identificador único del producto                                                                        |
| `sku`           | Código único del producto                                                                               |
| `nombre`        | Nombre del producto                                                                                     |
| `unidad_medida` | Unidad en la que se maneja el producto                                                                  |
| `stock_minimo`  | Cantidad mínima utilizada para identificar existencias por debajo del mínimo en la consulta obligatoria |
| `descontinuado` | Indica si el producto está descontinuado. Por defecto es `false`                                        |

### Bodega

| Campo       | Descripción                      |
| ----------- | -------------------------------- |
| `id`        | Identificador único de la bodega |
| `nombre`    | Nombre de la bodega              |
| `ubicacion` | Ubicación de la bodega           |

### Existencia

Representa la cantidad actual de un producto en una bodega. Es una **proyección del inventario y no la fuente de verdad**. La fuente de verdad es `Movimiento`.

| Campo         | Descripción                                          |
| ------------- | ---------------------------------------------------- |
| `id`          | Identificador único de la existencia                 |
| `producto_id` | Referencia a `Producto`                              |
| `bodega_id`   | Referencia a `Bodega`                                |
| `cantidad`    | Cantidad actual disponible del producto en la bodega |

**Restricción:** debe existir como máximo una existencia para cada combinación `producto_id` + `bodega_id`.

La cantidad se actualiza dentro de la misma transacción en la que se registra el `Movimiento` correspondiente.

### Movimiento

Representa cada cambio de inventario y constituye la **fuente de verdad del stock**. El saldo puede reconstruirse a partir del historial de movimientos.

| Campo               | Descripción                                                              |
| ------------------- | ------------------------------------------------------------------------ |
| `id`                | Identificador único del movimiento                                       |
| `tipo`              | `ENTRADA`, `SALIDA` o `TRASLADO`                                         |
| `producto_id`       | Referencia a `Producto`                                                  |
| `bodega_origen_id`  | Referencia a `Bodega` de origen. Es `null` para una `ENTRADA`            |
| `bodega_destino_id` | Referencia a `Bodega` de destino. Es `null` para una `SALIDA`            |
| `cantidad`          | Cantidad involucrada en el movimiento                                    |
| `fecha`             | Fecha y hora del movimiento                                              |
| `referencia`        | Texto opcional para indicar un motivo, pedido asociado u otra referencia |
| `administrador_id`  | Referencia al `Administrador` que registró el movimiento                 |

### Administrador

Representa al usuario encargado de registrar las operaciones del sistema.

| Campo           | Descripción                            |
| --------------- | -------------------------------------- |
| `id`            | Identificador único del administrador  |
| `username`      | Usuario de acceso                      |
| `password_hash` | Contraseña almacenada mediante un hash |
| `nombre`        | Nombre para mostrar                    |

### Pedido

Representa una solicitud de productos que se procesa de forma síncrona al momento de su creación y debe ser despachada desde una o varias bodegas.

| Campo              | Descripción                                          |
| ------------------ | ---------------------------------------------------- |
| `id`               | Identificador único del pedido                       |
| `fecha`            | Fecha del pedido                                     |
| `estado`           | `DESPACHADO` o `CANCELADO`                           |
| `administrador_id` | Referencia al `Administrador` que registró el pedido |

Los pedidos no permanecen en un estado intermedio. Al registrarse, el sistema evalúa inmediatamente la disponibilidad de los productos y determina el estado final del pedido.

### LineaPedido

Representa un producto y la cantidad solicitada dentro de un pedido.

| Campo                 | Descripción                                                                          |
| --------------------- | ------------------------------------------------------------------------------------ |
| `id`                  | Identificador único de la línea                                                      |
| `pedido_id`           | Referencia a `Pedido`                                                                |
| `producto_id`         | Referencia a `Producto`                                                              |
| `cantidad_solicitada` | Cantidad del producto solicitada                                                     |
| `cantidad_faltante`   | Cantidad del producto que no pudo ser despachada. Es `0` cuando la línea se completa |

El faltante se registra a nivel de línea porque un pedido puede contener varios productos y no necesariamente todos presentan el mismo resultado.

### DespachoDetalle

Permite registrar desde qué bodega se despacha cada cantidad de una línea de pedido. Una misma línea puede tener varios detalles, permitiendo que sea atendida desde varias bodegas.

| Campo                 | Descripción                                               |
| --------------------- | --------------------------------------------------------- |
| `id`                  | Identificador único del detalle                           |
| `linea_pedido_id`     | Referencia a `LineaPedido`                                |
| `bodega_id`           | Referencia a `Bodega` desde la que se realiza el despacho |
| `cantidad_despachada` | Cantidad del producto despachada desde esa bodega         |

---

## Estados de Pedido

El pedido solamente puede terminar en uno de dos estados:

```text
DESPACHADO | CANCELADO
```

El procesamiento es síncrono: al registrar un pedido, el sistema evalúa inmediatamente la disponibilidad del inventario y determina su estado final.

Un pedido queda:

* **`DESPACHADO`** cuando todas sus líneas pueden ser atendidas completamente.
* **`CANCELADO`** cuando al menos una de sus líneas no puede ser atendida completamente con el inventario disponible.

Cuando una línea no puede completarse, su `cantidad_faltante` registra la cantidad que no pudo ser despachada.

---

## Relaciones

| Entidad                           | Relación                  |
| --------------------------------- | ------------------------- |
| `Producto` → `Existencia`         | 1:N                       |
| `Producto` → `Movimiento`         | 1:N                       |
| `Producto` → `LineaPedido`        | 1:N                       |
| `Bodega` → `Existencia`           | 1:N                       |
| `Bodega` → `Movimiento`           | 1:N como origen o destino |
| `Bodega` → `DespachoDetalle`      | 1:N                       |
| `Administrador` → `Movimiento`    | 1:N                       |
| `Administrador` → `Pedido`        | 1:N                       |
| `Pedido` → `LineaPedido`          | 1:N                       |
| `LineaPedido` → `DespachoDetalle` | 1:N                       |

---

## Flujo conceptual del pedido

```text
Administrador
      │
      ↓
   Pedido
      │
      ↓
 LineaPedido
      │
      ↓
Evaluar disponibilidad
      │
      ├───────────────┐
      ↓               ↓
Se puede          No se puede
completar         completar alguna línea
      │               │
      ↓               ↓
DESPACHADO         CANCELADO
      │               │
      ↓               ↓
DespachoDetalle   cantidad_faltante
      │
      ↓
Movimiento SALIDA
      │
      ↓
Actualización de Existencia
```
Cuando un pedido no puede ser atendido completamente:

- El pedido queda `CANCELADO`.
- No se realiza ningún despacho parcial.
- No se crean `DespachoDetalle`.
- No se generan movimientos `SALIDA`.
- No se modifica la existencia.
- `cantidad_faltante` registra la cantidad que faltaba para completar cada línea afectada.

## 7. Reglas de inventario

`Movimiento` constituye la fuente de verdad del inventario, mientras que `Existencia` mantiene el saldo actual para facilitar las consultas.

Las operaciones que descuentan existencias deben validar y realizar el descuento de forma atómica.

No se debe depender únicamente de:

1. consultar la existencia;
2. comprobar que hay suficiente stock;
3. modificar la cantidad posteriormente.

La operación de descuento debe verificar que la cantidad disponible sigue siendo suficiente en el momento de realizar la actualización.

Si una operación de un pedido falla al descontar una existencia debido a concurrencia, no debe quedar un pedido parcialmente aplicado. La transacción completa debe revertirse. Mayor informacion en docs\adr\004-concurrency.md
