# ADR — Stack tecnológico

## Contexto

Se requiere definir las tecnologías para la persistencia de datos, el backend y la interfaz del Sistema C. La solución debe desarrollarse en un plazo de una semana y será implementada por una sola persona, por lo que la selección tecnológica debe reducir el riesgo de desarrollo y permitir avanzar rápidamente hacia una solución funcional.

## Decisión

Se utilizará el siguiente stack tecnológico:

* **Backend:** Spring Boot con Java.
* **Base de datos:** MySQL.
* **Interfaz:** React, consumiendo la API REST expuesta por el backend.

Esta combinación permite separar las responsabilidades del backend y la interfaz, manteniendo una arquitectura basada en una API REST como punto de comunicación entre ambas partes.

## Alternativas descartadas

### PostgreSQL

PostgreSQL y MySQL permiten cubrir las necesidades de persistencia y las reglas de negocio requeridas por el sistema. La decisión no se basa en una limitación técnica de PostgreSQL, sino en la experiencia previa con MySQL.

Utilizar una tecnología conocida reduce el tiempo necesario para familiarizarse con el motor y disminuye el riesgo de encontrar dificultades no previstas durante el desarrollo dentro del plazo disponible.

### Thymeleaf

Se consideró Thymeleaf como alternativa para implementar la interfaz mediante renderizado del lado del servidor, manteniendo el frontend y backend dentro de una misma aplicación.

Se descartó debido a la falta de experiencia previa con esta tecnología. Bajo el plazo establecido, introducir una tecnología nueva representa un riesgo mayor frente a utilizar React, con el que ya se cuenta con mayor familiaridad.

## Consecuencias

* La implementación de las operaciones concurrentes de inventario requiere definir un mecanismo explícito de control de concurrencia, independientemente del motor de base de datos seleccionado. Esta decisión se desarrolla en el ADR correspondiente a la concurrencia.
* La separación entre React y Spring Boot implica mantener dos procesos durante el desarrollo y requiere configurar la comunicación entre ambos, incluyendo el manejo de CORS.
* El uso de Spring Boot y React permite separar las responsabilidades del backend y la interfaz, pero introduce una mayor complejidad de configuración que una solución basada únicamente en renderizado del lado del servidor.
* La elección de tecnologías conocidas reduce el tiempo de aprendizaje y permite concentrar el esfuerzo en la implementación de las reglas de negocio del sistema.
