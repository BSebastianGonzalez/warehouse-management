# 1. Existencia por debajo del minimo

El enunciado no especifica si `stock_minimo` se compara contra la existencia de una bodega puntual o contra el total del producto en todas las bodegas.

Se asume que el umbral aplica sobre el total del producto (suma de existencias en todas las bodegas), no por bodega individual. Esto es consistente con la regla de que un pedido puede despacharse combinando varias bodegas: el stock se trata como fungible entre bodegas para efectos de disponibilidad, así que la alerta de reabastecimiento también debe mirar el total, no una bodega aislada con existencia baja mientras otra tiene sobrante.

La consulta obligatoria sigue mostrando el desglose por producto y bodega; la señal de "bajo mínimo" se calcula sobre el total agregado del producto y se repite en cada fila de ese producto.

Respecto a cómo se establece un `stock_minimo`, se asume que es un campo entero propio de la entidad Producto, configurable por producto según la necesidad real de negocio (rotación, criticidad, etc.). Para esta prueba, se usa un valor por defecto de 10 unidades cuando no se especifica otro al crear el producto.

# 2. Pedido de productos insuficientes

Aunque se especifica que los pedidos no pueden dejar en negativo la cantidad de producto, no se especifica si el pedido debe manejar estados que ayuden ante estas situaciones. Se supone entonces que el pedido simplemente queda cancelado indicando de manera explícita que el/los producto(s) son insuficientes.

# 3. Traslado de un producto descontinuado

El requisito "un producto descontinuado admite salidas pero no entradas nuevas" indica de manera explícita que el sistema no debe permitir entradas de ese producto. Estas entradas se generan únicamente en dos casos: al registrar más cantidad directamente en una bodega, o al recibir el producto como destino de un traslado desde otra bodega. Por lo tanto, se asume que un traslado de un producto descontinuado se rechaza por completo cuando ese producto es el que se está trasladando, sin importar el sentido del movimiento entre bodegas.

# 4. Procesamiento síncrono de pedidos

El enunciado no especifica si los pedidos deben permanecer pendientes para ser procesados posteriormente o si deben resolverse inmediatamente al registrarse.

Se asume que los pedidos se procesan de forma síncrona al momento de su creación. No existe una fase de espera ni una cola de pedidos por atender: el sistema evalúa inmediatamente la disponibilidad del producto en las bodegas y determina el resultado del pedido.

Un pedido puede utilizar existencias de varias bodegas para intentar satisfacer una misma línea. Si todas las líneas pueden ser atendidas completamente, el pedido queda en estado `DESPACHADO`. Si una o más líneas no pueden ser atendidas completamente, el pedido queda en estado `CANCELADO`.

La información sobre la cantidad que no pudo ser atendida se registra en cada `LineaPedido` mediante el campo `cantidad_faltante`, permitiendo identificar específicamente qué productos no pudieron ser satisfechos.
