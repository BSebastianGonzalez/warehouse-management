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

## Language and Naming Conventions

* All source code must be written in English.
* Use English for:

  * Java class names.
  * Interfaces and enums.
  * Variables and method names.
  * Package names.
  * Database table and column names.
  * REST endpoints.
  * DTOs.
  * Exception classes and messages.
  * Code comments and JavaDoc.
  * Test names and test descriptions.
* Follow standard Java naming conventions:

  * Classes and interfaces: `PascalCase`.
  * Methods and variables: `camelCase`.
  * Constants: `UPPER_SNAKE_CASE`.
  * Packages: lowercase.
* Domain concepts should use consistent English terminology throughout the codebase.

Examples:

* `Producto` → `Product`
* `Bodega` → `Warehouse`
* `Existencia` → `Stock`
* `Movimiento` → `Movement`
* `Pedido` → `Order`
* `LineaPedido` → `OrderLine`
* `DespachoDetalle` → `DispatchDetail`
* `Administrador` → `Admin`
* `cantidad` → `quantity`
* `stock_minimo` → `minimumStock`

Project documentation may remain in Spanish unless otherwise specified. The codebase itself must follow the English convention consistently.

### Gestión de catálogo
- Registrar, consultar, actualizar productos (incluyendo `stock_minimo` y marcar/desmarcar `descontinuado`).
- Registrar, consultar, actualizar bodegas.
- Crear o actualizar un producto no crea ni modifica existencias en ninguna bodega.

### Movimientos de inventario
- Registrar una `ENTRADA` de producto en una bodega (rechazada si el producto está `descontinuado`).
- La `ENTRADA` es la operación oficial para agregar inventario a una bodega: la primera entrada crea la combinación producto-bodega en `Existencia` y las siguientes entradas incrementan su cantidad.
- La cantidad inicial de un producto en una bodega también debe registrarse como `ENTRADA`; no existe un CRUD directo para `Existencia`.
- Registrar una `SALIDA` de producto desde una bodega.
- La `SALIDA` (`OUTBOUND`) descuenta inventario de una única bodega y puede utilizarse para salidas generales mientras no exista un pedido que origine el despacho.
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
- Las operaciones de escritura de movimientos y pedidos requieren sesión iniciada.
- Producto y Bodega son operaciones públicas de catálogo y no requieren sesión ni asociación persistida con un administrador.
- El `Administrador` autenticado es el actor de toda operación de movimiento o pedido que cambie o registre estado de inventario.
- Cada `ENTRADA`, `SALIDA`, `TRASLADO`, pedido y despacho debe quedar trazablemente asociado al `Administrador` que lo realizó.
- No se deben crear movimientos ni pedidos anónimos; las futuras operaciones de inventario o pedidos que modifiquen el estado también deben conservar esta asociación.

### Frontend
- La interfaz React vive en `frontend/` y usa Vite.
- Las peticiones autenticadas deben enviar credenciales (`credentials: include`) para conservar la sesión HTTP.
- En desarrollo, el frontend se ejecuta en `http://localhost:5173` y el backend en `http://localhost:8080`.
- La URL base del backend se configura con `VITE_API_URL`; por defecto es `http://localhost:8080/api`.

### Persistencia durante el alcance inicial
- El entorno de desarrollo mantiene `spring.jpa.hibernate.ddl-auto=update` para evitar bloquear el trabajo funcional con migraciones antes de estabilizar el modelo.
- Antes de un despliegue o de una evolución compartida del esquema se debe introducir Flyway con una migración inicial y cambiar `ddl-auto` a `validate`.

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


## 8. Frontend

## Frontend — React + Vite

El frontend se encuentra en `frontend/` y debe consumir exclusivamente la API REST del backend para realizar operaciones de negocio.

### Objetivo general

Construir una interfaz web sencilla, clara y funcional para la gestión del inventario multi-bodega.

La interfaz no debe intentar replicar la complejidad interna del backend. Su objetivo es permitir que un administrador pueda realizar las operaciones principales del sistema de manera intuitiva y visualizar claramente el estado actual del inventario.

El frontend debe priorizar:

1. Claridad de uso.
2. Separación visual y funcional de las diferentes áreas del sistema.
3. Flujo sencillo para las operaciones frecuentes.
4. Retroalimentación clara después de cada operación.
5. Consistencia visual entre las diferentes pantallas.
6. Diseño suficientemente atractivo para una presentación académica, sin introducir complejidad innecesaria.

### Organización funcional

La interfaz debe dividirse en secciones o vistas según las responsabilidades principales del sistema.

Como mínimo deben existir áreas para:

* Dashboard / resumen de inventario.
* Productos.
* Bodegas.
* Movimientos.

  * Entrada.
  * Salida.
  * Traslado.
* Pedidos.
* Consulta de existencias.
* Autenticación / inicio de sesión.

La organización exacta de las pantallas, navegación y componentes debe ser propuesta por el agente antes de realizar una implementación importante.

### Dashboard

El dashboard debe funcionar como punto de entrada después del inicio de sesión y proporcionar una visión rápida del estado del inventario.

Puede incluir información como:

* Cantidad de productos registrados.
* Cantidad de bodegas.
* Existencias que se encuentran por debajo del mínimo.
* Resumen de stock por bodega.
* Actividad reciente de movimientos o pedidos, si la API disponible lo permite.

No debe duplicar innecesariamente información que ya pueda consultarse en las secciones específicas.

### Productos

Debe permitir consultar y gestionar los productos mediante las operaciones disponibles en el backend.

La interfaz debe permitir visualizar claramente:

* SKU.
* Nombre.
* Unidad de medida.
* Stock mínimo.
* Estado del producto.
* Acción para crear/editar.
* Estado descontinuado.

Las acciones relacionadas con productos deben respetar las reglas de negocio implementadas en el backend.

El frontend no debe implementar reglas de negocio que correspondan al backend. Las validaciones de interfaz pueden utilizarse para mejorar la experiencia del usuario, pero el backend continúa siendo la autoridad final.

### Bodegas

Debe existir una sección para consultar, crear y actualizar bodegas.

Como mínimo debe mostrar:

* Nombre.
* Ubicación.

La interfaz debe evitar exponer operaciones inexistentes en el backend.

### Existencias

Debe existir una vista clara para consultar el inventario por producto y bodega.

Debe permitir identificar:

* Producto.
* Existencia en cada bodega.
* Existencia total del producto.
* Stock mínimo.
* Productos cuya existencia total se encuentra por debajo del mínimo.

Esta vista representa una de las funcionalidades principales del sistema y debe ser fácilmente accesible desde la navegación.

Cuando resulte útil, puede utilizarse una tabla o una representación visual equivalente para comparar las existencias entre bodegas.

### Movimientos

Las operaciones de inventario deben estar separadas visualmente según su tipo:

* Entrada.
* Salida.
* Traslado.

Cada formulario debe solicitar únicamente la información necesaria para realizar la operación.

El frontend debe:

* Mostrar claramente la bodega de origen y/o destino según corresponda.
* Permitir seleccionar el producto.
* Solicitar una cantidad válida.
* Permitir indicar una referencia cuando la API lo contemple.
* Mostrar los errores provenientes del backend.
* Informar claramente cuando la operación haya sido realizada correctamente.

No se debe permitir desde la interfaz una operación que contradiga evidentemente las reglas conocidas, por ejemplo seleccionar la misma bodega como origen y destino de un traslado.

Sin embargo, estas comprobaciones de interfaz no sustituyen las validaciones del backend.

#### Historial y trazabilidad de movimientos

Además de registrar nuevos movimientos, el frontend debe permitir consultar el
historial de movimientos ya realizados. Esta consulta representa una
auditoría de inventario y no debe confundirse con el formulario de creación.

La vista debe consumir `GET /api/movements` y:

* Requerir una sesión autenticada.
* Mostrar tipo, fecha, producto, cantidad, bodega de origen, bodega de
  destino y administrador responsable.
* Permitir filtrar por tipo, producto, bodega (como origen o destino),
  administrador y rango de fechas.
* El filtro de administrador debe ser opcional y presentarse como una lista de
  administradores. Si no se selecciona ninguno, deben mostrarse movimientos de
  todos los administradores.
* Mostrar los resultados ordenados del más reciente al más antiguo.
* Soportar paginación y estados de carga, vacío y error.

La identidad del administrador debe mostrarse como información de auditoría,
no como un campo editable. El frontend no debe reconstruir saldos ni inferir
movimientos a partir de la existencia actual; el backend es la fuente de
verdad del historial.

### Pedidos

Debe existir una interfaz para registrar pedidos con una o varias líneas.

El usuario debe poder:

1. Crear un pedido.
2. Agregar productos y cantidades.
3. Revisar las líneas antes de enviarlo.
4. Registrar el pedido.
5. Visualizar inmediatamente el resultado del procesamiento.

Como el backend procesa los pedidos de forma síncrona, la interfaz debe reflejar directamente uno de los dos resultados posibles:

* `DESPACHADO`.
* `CANCELADO`.

Para un pedido despachado, debe poder visualizarse desde qué bodegas se atendió cada línea cuando esta información esté disponible.

Para un pedido cancelado, debe mostrarse claramente la cantidad faltante por línea.

No debe existir en el frontend un estado de pedido que no exista en el modelo del backend.

#### Consulta histórica de pedidos

La interfaz también debe permitir consultar pedidos previamente registrados,
además de crear uno nuevo. La vista debe consumir `GET /api/orders`, requerir
sesión autenticada y:

* Mostrar identificador, fecha, estado, administrador responsable, cantidad de
  líneas, cantidad solicitada total y cantidad faltante total.
* Permitir filtrar por estado, administrador opcional y rango de fechas. El
  administrador debe seleccionarse desde una lista; sin selección se consultan
  pedidos de todos los administradores.
* Mostrar los resultados del más reciente al más antiguo con paginación.
* Permitir abrir el detalle mediante `GET /api/orders/{id}`.
* Mostrar en el detalle las líneas, faltantes y bodegas desde las que se
  despachó cada línea.
* Contemplar estados de carga, vacío, error y error de autorización.

El listado debe usar un DTO resumido; no debe cargar ni presentar todas las
líneas y despachos de cada pedido hasta que el usuario abra su detalle.

La implementación interna de los filtros puede usar `Specification`, pero debe
inicializar siempre una especificación no nula cuando no se envían filtros. La
consulta sin filtros es válida y no debe producir errores de tipo
`Specification must not be null`.

### Reconciliación de existencias

La consulta de existencias debe ofrecer una acción de reconciliación por
producto y bodega usando
`GET /api/stocks/products/{productId}/warehouses/{warehouseId}/reconciliation`.
Debe mostrar la cantidad proyectada, la cantidad reconstruida desde los
movimientos y si ambas son consistentes.

La reconciliación es una consulta de control y no una operación de edición. El
frontend debe mostrar el resultado entregado por el backend y no recalcular la
consistencia en React. También debe contemplar estados de carga, resultado
inconsistente, resultado consistente, vacío y error.

### Autenticación

El frontend debe proporcionar una pantalla de inicio de sesión.

Las peticiones que dependan de una sesión autenticada deben utilizar:

`credentials: include`

La sesión debe mantenerse mientras el usuario navega por la aplicación.

Debe existir una acción clara para cerrar sesión.

Las funcionalidades que requieren autenticación deben protegerse también desde la interfaz, pero el backend continúa siendo responsable de autorizar las operaciones.

### Navegación

La aplicación debe contar con una navegación consistente entre las diferentes secciones.

Se puede utilizar, según la propuesta del agente:

* Sidebar.
* Navbar.
* Menú lateral colapsable.
* Otra estructura equivalente.

La navegación debe permitir distinguir fácilmente entre:

* Consulta de información.
* Gestión del catálogo.
* Operaciones de inventario.
* Gestión de pedidos.

El agente debe proponer la estructura de navegación antes de implementarla si existen varias alternativas razonables.

### Componentización

El frontend debe evitar concentrar toda la aplicación en un único componente.

Los componentes deben dividirse de acuerdo con responsabilidades claras.

Se recomienda separar como mínimo:

* Layout general.
* Navegación.
* Autenticación.
* Componentes reutilizables de formularios.
* Tablas o componentes de consulta.
* Mensajes de error/éxito.
* Componentes específicos de cada dominio.

La estructura concreta de carpetas debe ser propuesta por el agente teniendo en cuenta el tamaño real de la aplicación y evitando sobrearquitectura.

No crear abstracciones genéricas únicamente por anticipación.

### Estado y comunicación con la API

La comunicación con el backend debe estar centralizada de forma razonable para evitar repetir lógica HTTP en cada componente.

El agente debe proponer una estrategia sencilla para:

* GET.
* POST.
* PUT.
* Manejo de errores.
* Sesión.
* Estados de carga.

No incorporar Redux, Zustand u otra solución de gestión global únicamente por costumbre. Si se considera necesaria una herramienta adicional, el agente debe justificarla antes de incorporarla.

### Estados de interfaz

Las vistas que realicen peticiones al backend deben contemplar al menos:

* Estado de carga.
* Estado exitoso.
* Estado vacío cuando corresponda.
* Error de comunicación.
* Error de validación o negocio proveniente del backend.

Los errores deben mostrarse de forma comprensible para el usuario y no limitarse a mostrar excepciones técnicas o respuestas JSON crudas.

### Diseño visual

El diseño debe ser sencillo, moderno y consistente.

No se busca construir una interfaz visualmente compleja ni utilizar efectos únicamente por estética.

Se pueden utilizar:

* Animaciones pequeñas para transiciones.
* Estados hover.
* Transiciones entre vistas.
* Modales o diálogos cuando simplifiquen una operación.
* Indicadores visuales para estados de pedido.
* Indicadores visuales para stock bajo.
* Skeletons o loaders cuando aporten claridad.

Las animaciones deben ser discretas y no interferir con la operación del sistema.

### Responsive

La interfaz debe ser usable al menos en resoluciones de escritorio y tablet.

No es necesario implementar una experiencia móvil compleja si esta no aporta valor al alcance del proyecto.

### Consistencia

Mantener una misma convención visual para:

* Botones.
* Formularios.
* Tablas.
* Mensajes.
* Estados.
* Modales.
* Espaciado.
* Tipografía.

Evitar implementar cada pantalla con un estilo visual diferente.

### Reglas de implementación

* No modificar las reglas de negocio del backend para facilitar el frontend.
* No duplicar innecesariamente lógica de negocio en React.
* No crear endpoints nuevos sin justificar la necesidad.
* Reutilizar los endpoints existentes siempre que sea posible.
* No agregar dependencias de UI o estado global sin justificar su incorporación.
* Priorizar componentes sencillos y reutilizables.
* Mantener el código del frontend en inglés, siguiendo las convenciones generales del proyecto.
* Los textos visibles para el usuario pueden estar en español.
* Mantener la separación entre lógica de presentación, comunicación con API y componentes de interfaz.
* No introducir una arquitectura frontend excesivamente compleja para el tamaño y alcance del proyecto.

### Proceso de trabajo con IA

Antes de realizar cambios importantes en el frontend, el agente debe:

1. Inspeccionar la implementación actual del frontend.
2. Revisar los endpoints disponibles en el backend.
3. Identificar qué funcionalidades ya están disponibles y cuáles faltan.
4. Proponer una estructura de navegación y organización de componentes.
5. Explicar las principales decisiones de diseño.
6. Indicar dependencias adicionales que considere necesarias y justificar cada una.
7. Esperar la decisión sobre las propuestas que impliquen cambios arquitectónicos o tecnológicos.
8. Implementar por etapas.
9. Ejecutar las pruebas/build correspondientes.
10. Informar qué fue implementado y qué aspectos quedan pendientes de verificar.

El agente no debe asumir que una propuesta de diseño está aprobada únicamente por estar incluida en el plan. Las decisiones deben discutirse y registrarse en `BITACORA-IA.md` cuando sean relevantes para el proyecto.
