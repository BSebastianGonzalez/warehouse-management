# ADR - Elección de herramienta de IA

## Contexto
El enunciado exige usar IA de forma declarada durante toda la prueba, y tener al menos dos vías (una agéntica y un chat de respaldo) desde el primer día, para no quedar sin herramienta si se agota la cuota gratuita de una de ellas a mitad de semana.

## Decisión
Se usa GitHub Copilot en como agente principal, con acceso a la capa gratuita ampliada mediante el Student Developer Pack de GitHub Education. Como respaldo de chat se usan dos herramientas en paralelo: ChatGPT para tareas generales, por su límite de uso más amplio en la capa gratuita, y Claude para tareas que requieren mayor precisión o análisis más complejo.

## Alternativas descartadas
- **Antigravity**: entorno agéntico gratuito, pero con un número de peticiones diarias más reducido que Copilot vía Student Pack.
- **Codex (ChatGPT)**: pensado para uso ocasional en la capa gratuita, insuficiente como herramienta agéntica principal para el volumen de trabajo de una semana completa.
- **Un único chat de respaldo**: se descartó tener solo uno, porque el límite gratuito de una sola herramienta podría agotarse.

## Consecuencias
Mantener tres herramientas distintas implica sincronizar manualmente el contexto entre ellas (AGENTS.md, decisiones tomadas), ya que no comparten memoria entre sí. A cambio, el riesgo de quedar sin cuota en un momento crítico de la semana queda prácticamente eliminado.