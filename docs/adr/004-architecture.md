# ADR — Arquitectura del sistema

## Contexto

El sistema debe desarrollarse en un plazo de una semana y será implementado por una sola persona. Por lo tanto, se necesita una arquitectura que permita desarrollar las funcionalidades rápidamente, mantener el código organizado y explicar claramente dónde se encuentra cada parte del sistema durante la defensa.

El alcance del proyecto tampoco requiere integraciones externas ni componentes que deban escalar de manera independiente.

## Decisión

Se utilizará un **monolito modular con una arquitectura de tres capas**.

El sistema será una única aplicación Spring Boot, pero su código estará organizado por módulos de dominio. Cada módulo podrá contener las capas necesarias para manejar su funcionalidad.

Las principales capas serán:

* **Controller:** recibe las peticiones HTTP, valida los datos de entrada y delega el procesamiento al Service. No contiene reglas de negocio.
* **Service:** contiene las reglas de negocio y coordina las operaciones del sistema. Por ejemplo, validar que exista suficiente stock, aplicar las reglas de los traslados o distribuir un pedido entre varias bodegas.
* **Repository:** se encarga de consultar y guardar información en la base de datos mediante Spring Data JPA. No contiene reglas de negocio.

La organización del código será **por módulo de dominio**, en lugar de agrupar todos los archivos según su tipo.

Por ejemplo:

```text
inventario/
├── InventarioApplication.java
│
├── producto/
│   ├── ProductoController.java
│   ├── ProductoService.java
│   └── ProductoRepository.java
│
├── bodega/
│   ├── BodegaController.java
│   ├── BodegaService.java
│   └── BodegaRepository.java
│
├── movimiento/
│   ├── MovimientoController.java
│   ├── MovimientoService.java
│   └── MovimientoRepository.java
│
└── pedido/
    ├── PedidoController.java
    ├── PedidoService.java
    └── PedidoRepository.java
```

No todos los módulos están obligados a tener exactamente las tres capas. Se crearán únicamente los componentes necesarios para cada funcionalidad.

La clase principal de Spring Boot estará ubicada en el paquete raíz de la aplicación para que Spring pueda detectar automáticamente los componentes de los módulos.

## Alternativas descartadas

### Microservicios

Se descarta dividir el sistema en varios servicios independientes.

Para el alcance de esta prueba, esta arquitectura agregaría complejidad relacionada con la comunicación entre servicios, configuración y despliegue, sin aportar una necesidad real al sistema.

El tiempo disponible se utilizará principalmente en implementar y probar las reglas de negocio del inventario.

### Arquitectura hexagonal

Se consideró utilizar una arquitectura hexagonal con puertos y adaptadores para separar completamente la lógica de negocio de la infraestructura.

Se descarta porque introduce más capas y abstracciones de las necesarias para el alcance actual. El sistema no requiere cambiar de proveedor de base de datos ni integrar múltiples fuentes externas.

### Agrupar todo por tipo de componente

También se consideró una estructura como:

```text
controllers/
services/
repositories/
```

con todos los componentes del sistema agrupados en cada carpeta.

Se descarta porque dificulta identificar qué código pertenece a cada parte del dominio. La organización por módulo permite encontrar más fácilmente las funcionalidades relacionadas con productos, bodegas, movimientos y pedidos.

### Colocar la lógica de negocio en los Controllers

Se descarta porque mezclaría el manejo de las peticiones HTTP con las reglas del sistema.

Mantener la lógica de negocio en los Services permite identificar de manera clara dónde se implementa cada regla y facilita su prueba y mantenimiento.

## Consecuencias

* El sistema se mantiene como una única aplicación, lo que simplifica su ejecución y despliegue.
* La organización por módulos facilita localizar el código relacionado con cada parte del dominio.
* Las reglas de negocio se concentran principalmente en los Services, haciendo más clara su ubicación durante el desarrollo y la defensa.
* La separación entre Controller, Service y Repository evita mezclar la recepción de peticiones, la lógica de negocio y el acceso a datos.
* Algunos módulos pueden tener más componentes que otros, ya que no se obliga a todos a seguir exactamente la misma estructura.
* La arquitectura puede generar cierta repetición de estructura entre módulos, pero se acepta a cambio de una organización más clara y fácil de explicar.
