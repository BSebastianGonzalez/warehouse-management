# Sesion 1 - 18/09/2024

## 1. Primera peticion

**Pedí:** analizar cómo debía señalarse el stock por debajo del mínimo.

**Propuse:** inicialmente evaluar el mínimo por bodega individual.

**Rechacé y por qué:** se observó que una bodega en cero no implica que falte producto si otra bodega tiene suficiente. Además, los pedidos pueden combinar varias bodegas.

**Acepté:** evaluar el mínimo sobre el total del producto entre todas las bodegas y usar 10 como valor por defecto de `minimumStock`.

**Quedó sin verificar:** si más adelante conviene mostrar una alerta adicional para bodegas individuales en cero.

## 2. Segunda peticion

**Pedí:** decidir si la concurrencia de traslados debía quedar como asunción o como decisión arquitectónica.

**Propuso:** dejarla como una asunción simple y confiar en el comportamiento por defecto de la base de datos.

**Rechacé y por qué:** la concurrencia afecta directamente la consistencia del inventario y el riesgo aceptado por el sistema.

**Acepté:** documentarla en un ADR dedicado a concurrencia.

**Quedó sin verificar:** el comportamiento exacto del motor de base de datos configurado y si requerirá ajustes adicionales.

---

# Sesion 2 - 19/09/2026

## 1. Primera peticion

**Pedí:** definir qué ocurre cuando una línea de pedido no tiene inventario suficiente.

**Propuso:** usar estados intermedios como `PENDING`, `PARTIALLY_DISPATCHED` y `DISPATCHED`.

**Rechacé y por qué:** los estados intermedios complicarían la consistencia y no están contemplados en el alcance.

**Acepté:** el pedido se procesa de forma síncrona: queda `DISPATCHED` si todas las líneas se completan, o `CANCELLED` si alguna no puede completarse. No hay despachos parciales.

**Quedó sin verificar:** el resultado exacto si otra operación consume stock entre la validación inicial y el descuento.

## 2. Segunda peticion

**Pedí:** decidir si `Stock` o `Movement` debía ser la fuente de verdad.

**Propuso:** usar `Stock` como saldo consultable y `Movement` como historial.

**Acepté con precisión:** `Movement` es la fuente de verdad y `Stock` es una proyección actualizada dentro de la misma transacción.

**Motivo:** así se conserva la trazabilidad y también se pueden hacer consultas eficientes.

**Quedó sin verificar:** la estrategia concreta para reconstruir y reconciliar el saldo.

## 3. Tercera peticion

**Pedí:** definir qué puede hacer un producto descontinuado.

**Propuso:** permitir traslados porque no agregan unidades al inventario global.

**Rechacé y por qué:** el traslado sí incrementa la existencia en la bodega destino y, por tanto, representa una nueva entrada allí.

**Acepté:** un producto descontinuado puede salir, pero no puede recibir `INBOUND` ni `TRANSFER`.

**Quedó sin verificar:** que la regla se aplique de forma uniforme en servicio, restricciones y pruebas.

## 4. Cuarta peticion

**Pedí:** escoger tecnologías y estructura para desarrollar rápido sin perder separación.

**Propuso:** Spring Boot y base relacional para el backend, dejando abierta la interfaz.

**Acepté:** Java/Spring Boot, JPA/MySQL, React y API REST.

**Motivo:** el stack reduce riesgo técnico y separa interfaz, reglas y persistencia.

**Quedó sin verificar:** la configuración concreta de Maven, JPA, MySQL y endpoints.

## 5. Quinta peticion

**Pedí:** organizar el backend sin mezclar reglas de negocio con HTTP y persistencia.

**Propuso:** monolito modular por dominio con capas Controller, Service y Repository.

**Acepté:** paquetes separados para Product, Warehouse, Stock, Movement, Admin y Order.

**Rechacé:** microservicios, porque añadirían complejidad innecesaria para el alcance.

**Quedó sin verificar:** la estructura final de clases después de implementar los módulos.

---

# Sesion 3 - 19/09/2026

## 1. Primera peticion

**Pedí:** revisar si `Stock` y `Movement` cumplían realmente el modelo y no solo los casos felices.

**Propuso:** agregar validaciones de entidad, restricciones persistentes, consultas de reconciliación y pruebas adicionales.

**Acepté:** mantener `Movement` como historial, `Stock` como proyección, incluir bodegas con cantidad cero y devolver error cuando producto o bodega no existen.

**Motivo:** un saldo cero inventado puede ocultar errores y la reconciliación permite detectar desincronización.

**Quedó sin verificar:** una reconstrucción completa contra una base MySQL real después de muchos movimientos.

## 2. Segunda peticion

**Pedí:** aclarar cómo se agregan productos a una bodega sin permitir CRUD directo de stock.

**Propuso:** usar `POST /api/movements/inbound`; la primera entrada crea la combinación producto-bodega y las siguientes incrementan la cantidad.

**Acepté:** no crear CRUD de `Stock`. `OUTBOUND` queda para salidas generales desde una única bodega y los pedidos reutilizan el descuento atómico.

**Rechacé:** permitir que el frontend actualice directamente la existencia, porque rompería el historial.

**Quedó sin verificar:** si será necesario un flujo separado para ajustes por conteo físico.

## 3. Tercera peticion

**Pedí:** corregir los endpoints de consulta de stock que devolvían `500`.

**Propuso:** revisar la navegación de propiedades JPA en el repositorio.

**Acepté:** cambiar la consulta a `findByProduct_IdAndWarehouse_Id`, porque `productId` y `warehouseId` son propiedades anidadas.

**Quedó sin verificar:** cubrir todos los endpoints equivalentes con pruebas HTTP.

---

# Sesion 4 - 19/09/2026

## 1. Primera peticion

**Pedí:** añadir trazabilidad del administrador sin implementar autenticación avanzada.

**Propuso:** crear `Admin`, almacenar BCrypt, usar `HttpSession` y proteger movimientos y pedidos.

**Acepté:** registro inicial, login, logout y asociación obligatoria de movimientos y pedidos con el administrador autenticado.

**Rechacé:** JWT, OAuth, roles y recuperación de contraseña, porque están fuera del alcance.

**Quedó sin verificar:** cómo cerrar o proteger operativamente el registro inicial fuera de un ambiente local.

## 2. Segunda peticion

**Pedí:** decidir si Producto y Bodega también requerían sesión.

**Propuso:** proteger todas las escrituras para tener una política uniforme.

**Rechacé y por qué:** el alcance solo exige sesión para movimientos y pedidos, y todavía no existen roles para distinguir administración de catálogo.

**Acepté:** mantener el catálogo público y auditar solo las operaciones que cambian el inventario o crean pedidos.

**Quedó sin verificar:** si una versión posterior necesitará auditar también la edición del catálogo.

---

# Sesion 5 - 19/09/2026

## 1. Primera peticion

**Pedí:** completar el modelo funcional después de catálogo, movimientos, stock y autenticación.

**Propuso:** implementar `Order`, `OrderLine` y `DispatchDetail` con evaluación síncrona.

**Acepté:** despachar desde una o varias bodegas cuando sea posible y cancelar sin efectos cuando falte stock.

**Rechacé:** estados pendientes o despachos parciales, porque no forman parte del flujo definido.

**Quedó sin verificar:** una política explícita de prioridad entre bodegas; inicialmente se usa el orden de la consulta.

## 2. Segunda peticion

**Pedí:** garantizar que un pedido no dejara cambios parciales.

**Propuso:** validar todas las líneas antes de descontar y ejecutar descuentos, detalles y movimientos dentro de una transacción.

**Acepté:** una falla de actualización condicional debe lanzar excepción y revertir todo. Un pedido cancelado no genera `OUTBOUND`, detalles ni cambios de stock.

**Quedó sin verificar:** una prueba real de concurrencia con dos transacciones simultáneas en MySQL.

## 3. Tercera peticion

**Pedí:** decidir cómo relacionar las salidas con el pedido.

**Propuso:** crear una relación JPA directa entre `Movement` y `Order`.

**Rechacé por ahora:** agregar esa relación antes de estabilizar el modelo, porque ya existe `reference = ORDER:{id}`.

**Acepté:** conservar la referencia textual inicialmente y evaluar una FK si luego se requieren reportes o integridad referencial más estricta.

**Quedó sin verificar:** si la referencia textual será suficiente para auditoría y devoluciones.

---

# Sesion 6 - 19/09/2026

## 1. Primera peticion

**Pedí:** añadir una interfaz para operar el sistema sin depender exclusivamente de Swagger.

**Propuso:** crear un frontend React/Vite que consumiera la API y conservara las reglas en el backend.

**Acepté:** incluir login/logout, creación básica de catálogo, dashboard de stock, entradas, salidas, traslados y creación de pedidos.

**Rechacé:** duplicar las reglas de inventario o crear otra autenticación en React.

**Quedó sin verificar:** edición completa del catálogo y consulta visual detallada de pedidos; la primera interfaz cubre la operación esencial.

## 2. Segunda peticion

**Pedí:** conectar React con la sesión HTTP del backend.

**Propuso:** enviar `credentials: include` y habilitar CORS solo para `http://localhost:5173`.

**Acepté:** usar `VITE_API_URL` para cambiar el backend y limitar CORS al entorno local.

**Quedó sin verificar:** dominios definitivos, HTTPS, cookies seguras y CORS de producción.

## 3. Tercera peticion

**Pedí:** decidir si reemplazar inmediatamente `ddl-auto=update` por Flyway.

**Propuso:** introducir Flyway antes de seguir agregando funcionalidades.

**Rechacé por ahora:** hacerlo antes de estabilizar pedidos y frontend, porque las migraciones serían difíciles de corregir durante cambios frecuentes.

**Acepté:** mantener `ddl-auto=update` solo en desarrollo y dejar Flyway con `ddl-auto=validate` como requisito previo a despliegue.

**Quedó sin verificar:** la migración inicial sobre una base existente.

## 4. Cuarta peticion

**Pedí:** comprobar que el incremento pudiera integrarse.

**Propuso:** ejecutar pruebas Maven, compilar React, revisar Git y publicar la rama para el PR hacia `main`.

**Acepté:** considerar verificado el incremento cuando `backend\mvnw.cmd -q test` y `frontend\npm run build` terminaran correctamente y el worktree quedara limpio.

**Quedó sin verificar:** una prueba end-to-end con backend levantado y datos reales de MySQL.
