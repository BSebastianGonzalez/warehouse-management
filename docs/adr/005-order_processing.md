# ADR — Procesamiento de pedidos y validación de stock

## Contexto

El sistema debe permitir registrar pedidos que pueden ser atendidos utilizando existencias de una o varias bodegas.

El enunciado establece que las operaciones de inventario no pueden dejar existencias negativas, pero no define qué debe ocurrir cuando no existe suficiente stock para satisfacer completamente un pedido.

Además, se necesita evitar que un pedido genere despachos parciales cuando no existe cantidad suficiente para completarlo.

## Decisión

Los pedidos se procesarán de forma síncrona al momento de su creación. No existirán pedidos pendientes de procesamiento ni una cola de pedidos por atender.

Antes de modificar cualquier existencia, el sistema validará que exista cantidad suficiente para satisfacer **completamente cada línea del pedido**, considerando las existencias disponibles en todas las bodegas.

Una misma línea podrá utilizar existencias de varias bodegas. Por ejemplo, si se solicitan 15 unidades y las bodegas disponen de 8 y 7 unidades, el pedido puede ser atendido utilizando ambas.

Si todas las líneas pueden ser atendidas completamente, se procede a realizar los despachos y el pedido queda en estado `DESPACHADO`.

Si una o más líneas no pueden ser atendidas completamente, el pedido queda en estado `CANCELADO`. En este caso, no se realizan despachos parciales ni se generan movimientos de salida para el pedido.

La cantidad que falta para cada línea se registra mediante `LineaPedido.cantidad_faltante`, permitiendo identificar específicamente qué productos provocaron la cancelación.

Una vez validado que el pedido puede completarse, los descuentos de existencia se realizan aplicando el mecanismo de control de concurrencia definido en el ADR correspondiente. Los despachos, movimientos y actualizaciones de existencias se realizan dentro de la misma transacción.

## Alternativas descartadas

### Permitir despachos parciales

Se descarta permitir que un pedido sea parcialmente atendido y posteriormente cancelado. El pedido debe poder completarse en su totalidad para generar los despachos.

### Mantener pedidos en estado `PENDIENTE`

Se descarta mantener pedidos pendientes para procesarlos posteriormente, ya que el sistema los resuelve inmediatamente al momento de su creación.

### Realizar los descuentos antes de validar todo el pedido

Se descarta porque podría provocar que algunas líneas ya hayan modificado el inventario cuando posteriormente se descubra que otra línea no puede ser atendida.

La disponibilidad se valida primero y solamente después se realizan las modificaciones de inventario.

## Consecuencias

* Un pedido solamente termina en `DESPACHADO` o `CANCELADO`.
* No existen despachos parciales.
* La disponibilidad se comprueba antes de modificar el inventario.
* Una línea puede utilizar varias bodegas para completar su cantidad solicitada.
* `cantidad_faltante` permite identificar qué productos fueron insuficientes cuando un pedido es cancelado.
* Los pedidos cancelados no generan movimientos de salida ni `DespachoDetalle`.
* La validación previa no reemplaza el control de concurrencia: al realizar los descuentos se debe volver a garantizar atómicamente que el stock siga siendo suficiente.
