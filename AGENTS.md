# 1. Contexto del negocio

La distribuidora maneja tres bodegas y puede despachar pedidos desde cualquiera de ellas. Actualmente el inventario se lleva en hojas de cálculo separadas por bodega y existen diferencias entre los saldos.

El objetivo es construir un sistema que centralice el inventario y permita registrar y consultar las operaciones realizadas sobre los productos.


# 2. Modelo de datos

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

## Regla de inventario

`Movimiento` constituye la fuente de verdad del inventario, mientras que `Existencia` mantiene el saldo actual para facilitar las consultas.

Las operaciones que descuentan existencias deben realizar el descuento de forma atómica y registrar el `Movimiento` correspondiente dentro de la misma transacción.

