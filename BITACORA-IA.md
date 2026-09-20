# Sesion 1 - 18/09/2024

# 1. Primera peticion 

Pedí: candidatas de asunciones para ASSUMPTIONS.md, empezando por analizar la consulta "Existencias por producto y bodega, señalando las que estan por debajo del minimo".

Propuse: que el mínimo se evaluara por bodega individual, con el argumento de que evaluarlo por total implicaría stock "fuera de las bodegas".

Rechacé y por qué: el agente señaló que ese argumento tenía un error logico, que es el que los productos finalmente se encuentran en las bodegas, no hay producto externo a ellas. Además, evaluar por bodega individual genera falsas alarmas (una bodega en cero con otra bodega llena) y no es consistente con la regla de que un pedido puede despacharse combinando varias bodegas.

Acepté con ajuste: el umbral se evalúa sobre el total del producto (suma de todas las bodegas); donde se determino agregar un valor por defecto de 10 unidades para stock_minimo cuando no se configura explícitamente.

Quedó sin verificar: si conviene una segunda señal no bloqueante para "bodega en cero" aunque el total del producto esté sano, esto es una propuesta no resuelta.

---

# 2. Segunda peticion 

Pedí: decidir si la concurrencia entre traslados simultáneos sobre el mismo producto y bodega se documenta como asunción o como ADR.

Me propuso: dejarla como una asunción simple, confiando en que el motor de base de datos maneja bien varias operaciones al mismo tiempo por defecto.

Decidí: subirla a ADR en vez de asunción, porque es una decisión de arquitectura (qué tan estricto debe ser el control de operaciones simultáneas y qué riesgo se acepta).

Quedó sin verificar: cómo se comporta exactamente el motor de base de datos que se termine usando, y si hace falta configurarlo explícitamente para que coincida con lo que diga el ADR.

---

# Sesion 2 - 19/09/2026

## 1. Primera peticion

**Pedí:** definir cómo debía comportarse un pedido cuando una de sus líneas no tiene suficiente inventario.

**Propuso:** considerar estados intermedios como `PENDIENTE`, `PARCIAL` y `DESPACHADO`, permitiendo que un pedido pudiera quedar parcialmente atendido.

**Rechacé y por qué:** se decidió que el sistema sería más sencillo si el pedido se resolviera inmediatamente al registrarse. Además, permitir despachos parciales complicaría la consistencia del inventario y el significado del estado del pedido.

**Acepté:** el procesamiento de pedidos será síncrono. Al registrar un pedido, primero se valida si todas sus líneas pueden ser atendidas completamente utilizando una o varias bodegas. Si todas pueden atenderse, el pedido queda `DESPACHADO`. Si alguna línea no puede completarse, el pedido queda `CANCELADO`.

No se permiten despachos parciales. En caso de cancelación, no se generan `DespachoDetalle` ni movimientos `SALIDA`. La cantidad que falta se almacena en `LineaPedido.cantidad_faltante`.

**Quedó sin verificar:** el comportamiento exacto que deberá tener la transacción si, después de realizar la validación inicial, otra operación concurrente consume parte del stock antes de que el pedido realice sus descuentos.

---

## 2. Segunda peticion

**Pedí:** determinar si `Existencia` debía ser la fuente principal del inventario o si los movimientos debían considerarse la fuente de verdad.

**Propuso:** mantener `Existencia` como el saldo actual consultable y utilizar `Movimiento` como historial de los cambios realizados.

**Acepté:** `Movimiento` será la fuente de verdad del inventario y `Existencia` funcionará como una proyección del saldo actual. Cada entrada, salida o traslado deberá registrar su movimiento correspondiente y actualizar la existencia dentro de la misma transacción.

**Motivo de la decisión:** de esta forma se conserva un historial de las operaciones y es posible reconstruir el saldo a partir de los movimientos, mientras que `Existencia` permite realizar las consultas de stock de manera eficiente.

**Quedó sin verificar:** la estrategia concreta para reconstruir el saldo a partir del historial y cómo se comprobará que la proyección `Existencia` no se desincronice respecto de los movimientos.

---

## 3. Tercera peticion

**Pedí:** analizar qué debía ocurrir con los productos marcados como `descontinuado`, especialmente cuando se realiza un traslado entre bodegas.

**Propuso:** permitir el traslado de productos descontinuados porque un traslado no representa necesariamente una entrada de inventario al sistema, sino un cambio de ubicación.

**Rechacé y por qué:** se interpretó que recibir el producto en la bodega de destino constituye una nueva entrada en esa bodega. Permitir el traslado permitiría incrementar la existencia del producto en otra ubicación, contradiciendo la regla de que un producto descontinuado no admite nuevas entradas.

**Acepté:** un producto descontinuado puede mantener las existencias que ya posee y puede tener operaciones de salida, incluyendo despachos de pedidos, pero no puede recibir nuevas entradas. Por tanto, los traslados de productos descontinuados quedan rechazados.

**Quedó sin verificar:** cómo deberá reflejarse esta restricción tanto en las validaciones del servicio como en las pruebas de los diferentes tipos de movimiento.

---

## 4. Cuarta peticion

**Pedí:** definir las tecnologías y la estructura general del sistema considerando que el proyecto debe desarrollarse en un periodo corto.

**Propuso:** utilizar un backend con Spring Boot y una base de datos relacional, dejando abierta la posibilidad de utilizar diferentes tecnologías para la interfaz.

**Acepté:** se decidió utilizar Java con Spring Boot para el backend, MySQL para persistencia y React para la interfaz. El frontend se comunicará con el backend mediante una API REST.

**Motivo de la decisión:** se busca utilizar una arquitectura conocida, reducir el riesgo técnico durante el tiempo disponible y mantener separadas la interfaz, las reglas de negocio y la persistencia.

**Quedó sin verificar:** la configuración concreta de Spring Data JPA, la conexión con MySQL y la estructura definitiva de los endpoints REST.

---

## 5. Quinta peticion

**Pedí:** determinar cómo organizar el backend para evitar mezclar las reglas de negocio con los controladores y el acceso a la base de datos.

**Propuso:** una arquitectura monolítica separada en las capas `Controller`, `Service` y `Repository`, organizada por dominios funcionales.

**Acepté:** se utilizará un monolito modular con paquetes orientados al dominio, como `producto`, `bodega`, `movimiento` y `pedido`. Los `Controller` se encargarán de recibir las solicitudes HTTP, los `Service` contendrán las reglas de negocio y los `Repository` gestionarán la persistencia mediante Spring Data JPA.

**Rechacé:** utilizar microservicios, debido a que introducirían complejidad adicional sin ser necesarios para el alcance del sistema.

**Quedó sin verificar:** la estructura concreta de paquetes y clases una vez que comience la implementación del backend.

---

# Sesion 3 - 19/09/2026

## Auditoria y correccion del nucleo Stock/Movement

Se revisaron `AGENTS.md`, `ASSUMPTIONS.md`, los ADR de arquitectura y concurrencia, y la
implementacion existente de `Stock` y `Movement`.

Se corrigio lo siguiente:

- `Stock` y `Product` validan sus cantidades tambien desde el modelo Java y declaran
  restricciones `CHECK` para impedir cantidades negativas.
- `Movement` valida producto, cantidad y combinaciones de bodegas, y declara restricciones
  persistentes para las reglas de `INBOUND`, `OUTBOUND` y `TRANSFER`.
- La consulta por producto ahora incluye todas las bodegas, incluso cuando la existencia es
  cero; las consultas con producto o bodega inexistentes responden con el error de recurso no
  encontrado en lugar de inventar un saldo cero.
- El resumen general carga stocks y bodegas en bloque para evitar el patron N+1.
- Se agrego una consulta de reconstruccion desde `Movement` y un endpoint de reconciliacion para
  comparar el saldo proyectado de `Stock` con el saldo historico.
- La actualizacion atomica de incrementos ahora verifica que la operacion haya afectado filas.
- Se agregaron pruebas para una entrada exitosa y se conservaron las pruebas de rechazos existentes.

La base de datos actual utiliza MySQL, por lo que el incremento/creacion atomico sigue usando
`INSERT ... ON DUPLICATE KEY UPDATE`. La asociacion obligatoria con `Admin` permanece como el
siguiente incremento porque requiere implementar primero la sesion HTTP y no se debe inventar un
administrador dentro de los movimientos.

## 6. Trazabilidad del administrador

Se aclaro en `AGENTS.md` que toda operacion que cambie o registre estado de negocio debe ser
trazable al `Administrador` autenticado. Esto incluye entradas, salidas, traslados, pedidos,
despachos y futuras operaciones de inventario.

En la rama funcional siguiente se implemento el fundamento de esta regla:

- `Admin` almacena username, nombre y solo el hash de la contraseña.
- `POST /api/auth/register` permite crear el administrador inicial y cierra el registro
  despues de la primera cuenta.
- `POST /api/auth/login` crea una sesion HTTP; `POST /api/auth/logout` la invalida.
- Las operaciones de movimiento requieren una sesion autenticada.
- `Movement` guarda `administrator_id` y las respuestas exponen el administrador responsable
  sin exponer su contraseña.

## 7. Pedidos sincronicos

Se inicio el modulo de pedidos en la rama `feature/synchronous-orders`:

- `Order`, `OrderLine` y `DispatchDetail` persisten el pedido, sus lineas y el desglose
  por bodega.
- `POST /api/orders` requiere sesion de administrador y evalua todas las lineas antes de
  descontar inventario.
- Un pedido insuficiente queda `CANCELLED`, conserva `missingQuantity` y no genera salidas
  ni detalles de despacho.
- Un pedido completo queda `DISPATCHED` y puede consumir una linea desde varias bodegas.
- Cada salida generada por el pedido conserva la trazabilidad del administrador y referencia
  el pedido.
- `GET /api/orders/{id}` consulta el detalle resultante.

La implementacion mantiene la transaccion unica y reutiliza el descuento condicional de
existencias para que una carrera de concurrencia revierta el pedido completo.

## 8. Interfaz React inicial

Se agrego `frontend/` como aplicacion React/Vite para operar el backend existente:

- inicio de sesion y cierre de sesion usando la cookie de `HttpSession`;
- dashboard de existencias por producto y bodega, con alerta de minimo;
- formulario para registrar entradas, salidas y traslados;
- manejo visible de errores y respuestas de la API;
- configuracion de CORS restringida al origen local `http://localhost:5173`.

La interfaz usa `VITE_API_URL` para cambiar la URL del backend sin modificar el codigo.
