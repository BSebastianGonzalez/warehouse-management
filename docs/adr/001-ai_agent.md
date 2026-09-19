# ADR — Elección de herramienta de IA

## Contexto

El enunciado exige utilizar herramientas de IA de manera declarada durante el desarrollo de la prueba. Además, se requiere contar desde el inicio con una herramienta de tipo agéntico y una alternativa de chat que pueda utilizarse como respaldo.

Debido a que el desarrollo se realizará durante una semana y las herramientas pueden tener límites de uso, se busca evitar depender de una única herramienta durante todo el proyecto.

## Decisión

Se utilizará **GitHub Copilot como agente principal de desarrollo**, aprovechando el acceso disponible mediante el Student Developer Pack de GitHub Education.

Como herramientas de respaldo se utilizarán:

* **ChatGPT:** para consultas generales, análisis y apoyo en tareas que no requieran modificar directamente el proyecto.
* **Claude:** como alternativa para tareas que requieran análisis adicional o una segunda opinión sobre decisiones y problemas técnicos.

La herramienta principal será Copilot debido a que permite trabajar directamente sobre el código del proyecto mediante un flujo agéntico, mientras que las herramientas de chat se utilizarán principalmente como apoyo y respaldo.

## Alternativas descartadas

### Antigravity

Se consideró como alternativa para el desarrollo agéntico. Se descartó debido a que sus límites de uso disponibles resultan menos convenientes para el volumen de trabajo previsto durante la semana en comparación con el acceso disponible a GitHub Copilot mediante el Student Developer Pack.

### Codex

Se consideró como alternativa para el desarrollo mediante agentes. Se descartó como herramienta principal debido a que el uso previsto en la capa disponible no resulta tan adecuado para mantenerlo como agente principal durante todo el desarrollo de la prueba.

### Utilizar un único chat de respaldo

Se descartó depender de una sola herramienta de chat como respaldo. Si se alcanza el límite de uso de esa herramienta, se perdería la alternativa disponible para consultas durante el desarrollo.

Mantener dos herramientas de chat permite disponer de una segunda opción en caso de que una de ellas alcance sus límites.

## Consecuencias

* GitHub Copilot será la herramienta principal para generar, modificar y revisar código dentro del proyecto.
* ChatGPT y Claude se utilizarán como herramientas complementarias para análisis, consultas y revisión de decisiones.
* El contexto importante del proyecto deberá mantenerse en archivos del repositorio, principalmente `AGENTS.md`, `ASSUMPTIONS.md`, los ADR y `BITACORA-IA.md`, en lugar de depender de la memoria de una herramienta específica.
* Utilizar varias herramientas implica mantener manualmente el contexto entre ellas, ya que no comparten automáticamente las decisiones tomadas en las demás.
* Contar con herramientas alternativas reduce el riesgo de quedar sin una herramienta de IA disponible durante una parte crítica del desarrollo.
* Las respuestas y el código generados por las herramientas de IA deberán ser revisados antes de incorporarse al proyecto.
