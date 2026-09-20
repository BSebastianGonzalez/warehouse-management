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

## 1. Primera petición

**Pedí:** revisar si `Stock` y `Movement` cumplían realmente el modelo definido en `AGENTS.md`, y no solo los casos felices (entradas, salidas y traslados con stock suficiente y sin condiciones de carrera).

**Propuso:** agregar validaciones tanto en la entidad Java como restricciones `CHECK` persistentes (para que la base de datos rechace estados inválidos incluso si algo se salta la capa de servicio), consultas de reconciliación que reconstruyan el saldo desde el historial de `Movement`, y pruebas adicionales para los casos de rechazo.

**Acepté:** mantener `Movement` como fuente de verdad y `Stock` como proyección (según el ADR correspondiente); que la consulta de existencias incluya bodegas con cantidad cero en vez de omitirlas; y que la API devuelva error cuando el producto o la bodega consultados no existen, en vez de responder con un saldo cero que podría confundirse con "existe pero está vacío".

**Motivo:** un saldo cero inventado para un recurso que no existe oculta errores (por ejemplo, un `productId` mal escrito desde el frontend pasaría inadvertido). Incluir explícitamente las combinaciones producto-bodega en cero es necesario para que la consulta obligatoria pueda señalar productos por debajo del mínimo incluso cuando nunca han tenido movimiento en alguna bodega.

**Quedó sin verificar:** una reconstrucción completa del saldo contra una base MySQL real después de acumular muchos movimientos (la reconciliación se probó, pero no a volumen).

## 2. Segunda petición

**Pedí:** aclarar cómo se agregan productos a una bodega sin abrir un CRUD directo sobre `Stock`, dado que `Existencia` es una proyección y no debe mutarse fuera del flujo de movimientos.

**Propuso:** usar `POST /api/movements/inbound` como único punto de entrada — la primera entrada para una combinación producto-bodega crea el registro de `Stock`, y las siguientes simplemente incrementan la cantidad existente.

**Acepté:** no crear ningún endpoint de escritura directa sobre `Stock`. `OUTBOUND` queda reservado para salidas generales que no vienen de un pedido (mermas, ajustes manuales) desde una única bodega, mientras que los pedidos reutilizan el mismo mecanismo de descuento atómico que ya existía para `OUTBOUND`, en vez de duplicar la lógica.

**Rechacé:** que el frontend pudiera actualizar directamente la cantidad de una existencia (por ejemplo, un formulario de "editar stock"), porque rompería la regla de que todo cambio de inventario debe generar su `Movimiento` correspondiente.

**Quedó sin verificar:** si hará falta, más adelante, un flujo separado para ajustes por conteo físico que no encaje bien como `ENTRADA` ni como `SALIDA` ordinaria.

## 3. Tercera petición

**Pedí:** corregir los endpoints de consulta de stock que estaban devolviendo error `500`.

**Propuso:** revisar cómo el repositorio navegaba las propiedades anidadas de la entidad `Stock` en las consultas derivadas de Spring Data.

**Acepté:** cambiar la consulta a `findByProduct_IdAndWarehouse_Id`, ya que `productId` y `warehouseId` no son propiedades directas de `Stock` sino que hay que atravesar las relaciones `product` y `warehouse` para llegar a su `id`.

**Quedó sin verificar:** si el resto de los endpoints equivalentes de consulta están cubiertos por pruebas HTTP de extremo a extremo, o si solo se probó el caso puntual que fallaba.

---

# Sesion 4 - 19/09/2026

## 1. Primera petición

**Pedí:** añadir trazabilidad del administrador a las operaciones del sistema, sin implementar autenticación avanzada.

**Propuso:** crear la entidad `Admin` almacenando la contraseña con BCrypt, usar `HttpSession` para manejar la sesión, y proteger los endpoints de movimientos y pedidos exigiendo sesión activa.

**Acepté:** un flujo de registro inicial (que se cierra después de la primera cuenta), login, logout, y la asociación obligatoria entre cada `Movement`/`Order` y el administrador autenticado que los registró — consistente con las relaciones `Administrador → Movimiento` y `Administrador → Pedido` definidas en el modelo.

**Rechacé:** JWT, OAuth, manejo de roles y recuperación de contraseña, porque el alcance excluye explícitamente autenticación avanzada y solo hay un tipo de usuario.

**Quedó sin verificar:** cómo cerrar o proteger operativamente el endpoint de registro inicial fuera de un ambiente local.

## 2. Segunda petición

**Pedí:** decidir si los endpoints de `Producto` y `Bodega` (catálogo) también debían requerir sesión iniciada.

**Propuso:** proteger todas las escrituras del sistema por igual, para una política de seguridad uniforme.

**Rechacé y por qué:** el alcance solo exige sesión para movimientos y pedidos, y como no existen roles, no hay forma de distinguir "quien administra el catálogo" de "quien opera el inventario" — proteger todo por igual habría sido una regla sin sustento en los requisitos.

**Acepté:** mantener el catálogo sin autenticación y auditar únicamente las operaciones que cambian el estado del inventario o crean pedidos.

**Quedó sin verificar:** si una versión posterior necesitará auditar también la edición del catálogo.

---

# Sesion 5 - 19/09/2026

## 1. Primera petición

**Pedí:** completar el módulo de pedidos una vez resueltos catálogo, movimientos, stock y autenticación.

**Propuso:** implementar `Order`, `OrderLine` y `DispatchDetail` con evaluación síncrona al crear el pedido.

**Acepté:** despachar desde una o varias bodegas cuando sea posible completar cada línea, y cancelar sin ningún efecto sobre el inventario cuando falte stock — sin estados intermedios.

**Rechacé:** estados como `PENDIENTE` o despachos parciales, ya descartados desde la Sesión 2: el sistema resuelve el pedido de inmediato y en su totalidad, o lo cancela por completo.

**Quedó sin verificar:** todavía no existe una política explícita de prioridad entre bodegas cuando una línea se reparte entre varias; por ahora se usa el orden en que la consulta devuelve las bodegas, lo cual habría que contrastar contra el ADR de despacho multi-bodega.

## 2. Segunda petición

**Pedí:** garantizar que un pedido nunca dejara cambios parciales aplicados al inventario, ni ante una falla de concurrencia a mitad de proceso.

**Propuso:** validar todas las líneas antes de descontar cualquier existencia, y ejecutar descuentos, `DispatchDetail` y `Movement` dentro de una única transacción.

**Acepté:** que una falla en la actualización condicional de stock durante la ejecución lance una excepción que revierta toda la transacción del pedido. Un pedido cancelado, por diseño, no genera `OUTBOUND`, ni `DispatchDetail`, ni modifica ningún `Stock`.

**Quedó sin verificar:** una prueba real de concurrencia con dos transacciones simultáneas compitiendo por el mismo stock en una base MySQL real — hasta ahora la garantía es teórica, no probada bajo carga concurrente.

## 3. Tercera petición

**Pedí:** decidir cómo relacionar las salidas generadas por un pedido con el pedido que las originó.

**Propuso:** crear una relación JPA directa (`@ManyToOne`) entre `Movement` y `Order`.

**Rechacé por ahora:** agregar esa relación formal antes de que el modelo de pedidos esté más estable, dado que ya existe una forma de vincularlos sin tocar el esquema: el campo `referencia` de `Movement` (por ejemplo `ORDER:{id}`).

**Acepté:** conservar por ahora la referencia textual, y evaluar más adelante una FK real si surgen necesidades de reportes o integridad referencial más estricta.

**Quedó sin verificar:** si la referencia textual será suficiente el día que haga falta soportar devoluciones o auditorías más detalladas.

---

# Sesion 6 - 19/09/2026

## 1. Primera petición

**Pedí:** añadir una interfaz para operar el sistema completo, sin depender exclusivamente de Swagger.

**Propuso:** un frontend en React/Vite que consumiera la API REST existente, sin duplicar ninguna regla de negocio.

**Acepté:** que la interfaz cubra login/logout, creación básica de catálogo, un dashboard de existencias, formularios para entradas/salidas/traslados, y creación de pedidos — el flujo operativo esencial de principio a fin.

**Rechacé:** duplicar las reglas de inventario en el frontend (por ejemplo, validar stock en JavaScript antes de enviar la solicitud) o implementar una autenticación paralela en React, ya que la sesión HTTP del backend resuelve eso.

**Quedó sin verificar:** la edición completa del catálogo (por ahora solo hay creación) y una consulta visual detallada de pedidos ya procesados.

## 2. Segunda petición

**Pedí:** conectar el frontend en React con la sesión HTTP manejada por el backend, en vez de un esquema de tokens.

**Propuso:** enviar `credentials: include` en cada solicitud desde React para incluir la cookie de sesión, y habilitar CORS en el backend únicamente para `http://localhost:5173`.

**Acepté:** usar `VITE_API_URL` para cambiar la URL del backend sin tocar código, y mantener el CORS restringido al entorno de desarrollo local por ahora.

**Quedó sin verificar:** cómo se configurará esto para un dominio definitivo, si se requerirá HTTPS y cookies seguras, y cuál será la política de CORS fuera del entorno local.

## 3. Tercera petición

**Pedí:** decidir si convenía reemplazar de inmediato `ddl-auto=update` por migraciones con Flyway.

**Propuso:** introducir Flyway antes de seguir agregando funcionalidad nueva.

**Rechacé por ahora:** hacerlo en este punto, porque el modelo de pedidos y el frontend todavía estaban cambiando activamente, y las migraciones de Flyway son más difíciles de corregir retroactivamente que un esquema autogenerado durante cambios frecuentes.

**Acepté:** mantener `ddl-auto=update` únicamente como conveniencia de desarrollo local, y dejar explícito que Flyway con `ddl-auto=validate` es requisito obligatorio antes de cualquier despliegue.

---

# Sesion 7 - 20/09/2026

## Primera petición

**Pedí:** diseñar un plan por fases para mejorar el frontend básico y realizar un commit por cada fase.

**Propuso:** separar el frontend en un shell de aplicación, cliente REST, autenticación, navegación, dashboard, existencias, catálogo, movimientos y pedidos, implementando cada bloque de forma incremental y validándolo con `npm run build`.

**Acepté:** utilizar React Router para la navegación declarativa y la protección de rutas. No se incorporarán Redux, Zustand ni una librería visual adicional inicialmente; el estado permanecerá local a cada vista y la sesión se conservará mediante `credentials: include`.

**Límites aceptados:** el frontend reutilizará los endpoints existentes y no simulará listados de movimientos o pedidos que el backend todavía no expone. Tampoco se crearán endpoints nuevos para facilitar la interfaz sin una decisión explícita.

**Estructura acordada:** Dashboard, Inventory, Products, Warehouses, Orders y Authentication, con textos visibles en español y código fuente en inglés.

**Flujo de trabajo acordado:** un commit por fase, con validación del build y comprobaciones de los flujos REST relevantes antes de avanzar.

**Quedó sin verificar:** cómo se vería la migración inicial de Flyway aplicada sobre una base de datos que ya tiene datos.

---

# Sesion 8 - 20/09/2026

## Trazabilidad, listados históricos y reconciliación en frontend

Se identificó que el backend ya conserva la trazabilidad del administrador en
movimientos y pedidos, pero el frontend solo cubría la creación de movimientos
y pedidos y no ofrecía consultas históricas ni usaba la reconciliación de
existencias.

Se aceptó:

- documentar en `AGENTS.md` las vistas de historial de movimientos y pedidos;
- requerir sesión autenticada para las consultas de auditoría y listados;
- agregar filtros y paginación ordenada de más reciente a más antiguo;
- mostrar el administrador responsable como dato de auditoría no editable;
- mantener el detalle completo de pedidos separado del DTO resumido del listado;
- exponer en el frontend la reconciliación entre existencia proyectada e
  historial de movimientos sin duplicar el cálculo en React.

Los filtros acordados para movimientos son tipo, producto, bodega de origen o
destino, administrador opcional y rango de fechas. La referencia se conserva
en los movimientos registrados para trazabilidad, pero no se utiliza como
filtro ni se muestra en el historial. Para pedidos son estado, administrador
opcional y rango de fechas; sin administrador seleccionado se consultan todos.

Se corrigió la causa del error `Specification must not be null`: las consultas
históricas deben iniciar con una especificación no nula cuando no hay filtros.
No se elimina `Specification` del backend porque sigue siendo útil para
combinar filtros opcionales.

## 4. Cuarta petición

**Pedí:** comprobar que el incremento de esta sesión pudiera integrarse de forma segura al resto del proyecto.

**Propuso:** ejecutar las pruebas de Maven, compilar el frontend, revisar el estado de Git y publicar la rama para abrir un PR hacia `main`.

**Acepté:** dar por verificado el incremento únicamente cuando `backend\mvnw.cmd -q test` y `frontend\npm run build` terminaran sin errores y el árbol de trabajo quedara limpio.

**Quedó sin verificar:** una prueba end-to-end real, con el backend levantado y datos reales en MySQL, en vez de solo pruebas unitarias/de integración.
