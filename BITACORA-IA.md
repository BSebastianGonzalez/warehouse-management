# Sesion 1 - 18/09/2024

# 1. Primera peticion 

Pedí: candidatas de asunciones para ASSUMPTIONS.md, empezando por analizar la consulta "Existencias por producto y bodega, señalando las que estan por debajo del minimo".

Propuse: que el mínimo se evaluara por bodega individual, con el argumento de que evaluarlo por total implicaría stock "fuera de las bodegas".

Rechacé y por qué: el agente señaló que ese argumento tenía un error logico, que es el que los productos finalmente se encuentran en las bodegas, no hay producto externo a ellas. Además, evaluar por bodega individual genera falsas alarmas (una bodega en cero con otra bodega llena) y no es consistente con la regla de que un pedido puede despacharse combinando varias bodegas.

Acepté con ajuste: el umbral se evalúa sobre el total del producto (suma de todas las bodegas); donde se determino agregar un valor por defecto de 10 unidades para stock_minimo cuando no se configura explícitamente.

Quedó sin verificar: si conviene una segunda señal no bloqueante para "bodega en cero" aunque el total del producto esté sano, esto es una propuesta no resuelta.

# 2. Segunda peticion 

Pedí: decidir si la concurrencia entre traslados simultáneos sobre el mismo producto y bodega se documenta como asunción o como ADR.

Me propuso: dejarla como una asunción simple, confiando en que el motor de base de datos maneja bien varias operaciones al mismo tiempo por defecto.

Decidí: subirla a ADR en vez de asunción, porque es una decisión de arquitectura (qué tan estricto debe ser el control de operaciones simultáneas y qué riesgo se acepta).

Quedó sin verificar: cómo se comporta exactamente el motor de base de datos que se termine usando, y si hace falta configurarlo explícitamente para que coincida con lo que diga el ADR.