# ADR — Control de concurrencia en operaciones sobre existencias

## Contexto

Las operaciones de inventario pueden ejecutarse de manera simultánea sobre el mismo producto y bodega. Por ejemplo, un traslado y un despacho podrían intentar descontar existencias al mismo tiempo.

Si ambas operaciones consultan el saldo disponible antes de realizar el descuento, podrían utilizar el mismo valor inicial y aprobar individualmente la validación de stock suficiente, aunque la suma de ambas operaciones supere la cantidad disponible.

Por lo tanto, se necesita garantizar que una operación no pueda descontar existencias si el saldo disponible ya no es suficiente.

## Decisión

El descuento de existencias se realizará de forma atómica, verificando la cantidad disponible como parte de la misma operación que realiza el descuento.

Conceptualmente, la operación seguirá esta regla:

```sql
UPDATE existencia
SET cantidad = cantidad - :cantidad
WHERE producto_id = :producto
  AND bodega_id = :bodega
  AND cantidad >= :cantidad;
```

Si la operación no modifica ninguna fila, significa que no existe suficiente stock disponible y la operación será rechazada.

El descuento y el registro del `Movimiento` correspondiente se ejecutarán dentro de la misma transacción, de manera que ambas operaciones se confirmen o se reviertan conjuntamente.

## Alternativas descartadas

### Leer, validar y actualizar por separado

Se descarta el enfoque de consultar primero la existencia, validar el saldo en la aplicación y posteriormente realizar el descuento.

Este enfoque puede presentar una condición de carrera cuando dos operaciones se ejecutan simultáneamente sobre la misma existencia.

### Bloquear explícitamente la existencia antes de actualizar

Se consideró realizar un bloqueo explícito de la fila antes de consultar y modificar el saldo.

Aunque este enfoque permite controlar la concurrencia, se decidió utilizar una actualización condicional porque concentra la validación y el descuento en una única operación y mantiene el mecanismo más simple para este caso.

## Consecuencias

* El sistema garantiza que una operación no pueda descontar una cantidad superior al stock disponible.
* La validación de stock y el descuento deben realizarse mediante una operación atómica.
* La lógica de actualización de existencias no debe depender únicamente de leer la entidad, modificarla en memoria y posteriormente guardarla.
* La implementación requiere una consulta específica para realizar el descuento condicional.
* El descuento de existencia y el registro del movimiento deben formar parte de la misma transacción.
    