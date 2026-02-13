# Sistema Multiagente: Cifras y Letras

## 📋 Descripción General

Implementación de un **sistema multiagente distribuido** que replica el juego televisivo español "Cifras y Letras" usando la plataforma **JADE (Java Agent Development Framework)**. El sistema permite que múltiples jugadores compitan simultáneamente resolviendo rondas de cifras, donde deben aproximarse a un número objetivo usando operaciones matemáticas sobre un conjunto inicial de números.

Este proyecto implementa la parte de **Cifras** del juego, con soporte para jugadores automáticos con diferentes niveles de habilidad (Experto, Intermedio, Principiante, Aleatorio) y lectura manual por teclado.

**Asignatura**: Programación de Agentes (PA1)  
**Universidad**: Universidad de Salamanca

## 🎯 Objetivos

- Implementar un sistema multiagente distribuido con comunicación inter-agentes
- Aplicar patrones de coordinación y sincronización entre agentes
- Crear un sistema de resolución automática de problemas matemáticos
- Implementar diferentes estrategias de búsqueda según nivel de dificultad
- Manejar comunicación asíncrona mediante mensajes ACL (Agent Communication Language)
- Utilizar el Directory Facilitator (DF) para registro y descubrimiento de agentes

## 🎮 Reglas del Juego (Cifras)

### Objetivo
Calcular un número objetivo partiendo de una lista de números iniciales usando **sólo** las operaciones:
- **Suma** (+)
- **Resta** (-) (resultado debe ser positivo)
- **Multiplicación** (*)
- **División** (/) (sólo exacta, sin decimales)

### Restricciones
- Cada número original puede usarse **una sola vez**
- Los resultados intermedios pueden reutilizarse
- Un resultado que no ha sido usado puede volver a usarse
- No se pueden usar números negativos o decimales

### Ganador
- El jugador cuyo resultado sea **más cercano** al número objetivo
- En caso de empate, ganan todos los empatados
- Si nadie envía solución, no hay ganadores

### Ejemplo
```
Números: [25, 7, 4, 6, 4, 1]
Objetivo: 866

Solución:
25 + 6 = 31
7 * 4 = 28
28 * 31 = 868
868 - 1 = 867

Distancia: |866 - 867| = 1 (muy buena solución)
```

## 🤖 Arquitectura de Agentes

### 1. Agente Aitor (Presentador)

**Responsabilidades**:
- Controlar el flujo del juego
- Realizar cuenta atrás (15 → 0) antes de cada ronda
- Dar turno al Experto David
- Esperar y recibir resultados de ganadores

**Ciclo de vida**:
```
1. Cuenta atrás (15 segundos)
2. Mensaje de turno a David
3. Espera ganadores (con timeout de 2 segundos sin mensajes)
4. Vuelve al paso 1
```

**Mensajes que envía**:
- `AITOR_TIEMPO_JUGADORES:<tiempo>` (cada 1 segundo: 15,14,13...0)
- `AITOR_TURNO_DAVID_JUGADORES` (cuando llega a 0)

### 2. Agente ExpertoDavid (Experto en Cifras)

**Responsabilidades**:
- Generar problemas (6 números + objetivo)
- Coordinar la ronda de cifras
- Recolectar soluciones de jugadores
- Evaluar y comunicar ganadores

**Ciclo de vida de ronda**:
```
1. Recibe AITOR_TURNO_DAVID_JUGADORES
2. Genera 6 números aleatorios + objetivo
3. Envía números (con delay de 100ms entre cada uno)
4. Envía objetivo a buscar
5. Envía mensaje de INICIO
6. Espera 40 segundos sin hacer nada (jugadores resuelven)
7. Envía mensaje de FINALIZACIÓN
8. Lee todas las soluciones de la cola
9. Calcula ganador(es)
10. Envía mensaje(s) de ganador
11. Vuelve a esperar turno de Aitor
```

**Mensajes que envía**:
- `DAVID_NUMERO_JUGADORES:<numero>` (6 veces, 100ms entre cada uno)
- `DAVID_VALOR_BUSCADO_JUGADORES:<valor>`
- `DAVID_EMPEZAR_CIFRAS_JUGADORES`
- `DAVID_FINALIZAR_CIFRAS_JUGADORES`
- `DAVID_GANADOR_JUGADORES_AITOR:<nombre>:<solución>`

**Validación de soluciones**:
- Verifica que todos los operandos usados están en la lista inicial o fueron calculados
- Cada operando original se usa máximo una vez
- Las operaciones son válidas (sin divisiones por 0, sin divisiones inexactas)
- El resultado es el más cercano al objetivo

### 3. Agentes Jugador

**Responsabilidades**:
- Recibir números y objetivo
- Generar/introducir solución (automática o manual)
- Enviar solución antes de timeout
- Recibir y mostrar ganadores

**Modos de funcionamiento**:
- **Automático**: Búsqueda algorítmica con nivel configurable
- **Manual (Teclado)**: Introducción interactiva durante 40 segundos

**Niveles de dificultad (automático)**:
1. **EXPERTO** (20% probabilidad)
   - Búsqueda exhaustiva recursiva (6 operaciones)
   - Sin errores (0% probabilidad de mala decisión)
   - Encuentra soluciones óptimas o muy cercanas
   - Tiempo: hasta 35 segundos

2. **INTERMEDIO** (40% probabilidad)
   - Búsqueda parcial con heurísticas
   - 15% probabilidad de decisiones subóptimas
   - Explora hasta 4 operaciones
   - Tiempo: hasta 25 segundos

3. **PRINCIPIANTE** (30% probabilidad)
   - Búsqueda aleatoria dirigida
   - 35% probabilidad de mala decisión
   - Explora hasta 3 operaciones
   - 25% probabilidad de no encontrar nada
   - Tiempo: hasta 15 segundos

4. **ALEATORIO** (10% probabilidad)
   - Configuración completamente aleatoria
   - Comportamiento impredecible


## 📊 Estructura del Proyecto

```
proyecto/
├── src/
│   └── es/usal/pa/
│       ├── agent/
│       │   ├── AgenteAitor.java              # Presentador
│       │   ├── AgenteExpertoDavid.java       # Experto en cifras
│       │   ├── AgenteJugador.java            # Jugador (automático/manual)
│       │   ├── MainPrueba.java               # Punto de entrada
│       │   ├── Utils.java                    # Utilidades
│       │   └── modelo/
│       │       ├── TipoMensaje.java          # Enum de tipos de mensaje
│       │       └── VariablesConfiguracion.java # Constantes del sistema
│       │
│       └── cifras/
│           ├── controlador/
│           │   ├── AuxOperacion.java         # Cálculo de operaciones
│           │   ├── AuxProblema.java          # Generación de números/objetivo
│           │   ├── AuxSolucion.java          # Validación de soluciones
│           │   ├── CallableSolucionAutomatica.java  # Búsqueda automática (4 niveles)
│           │   └── CallableSolucionTeclado.java     # Entrada por teclado
│           │
│           └── modelo/
│               ├── Operacion.java            # Estructura: operando1, operador, operando2
│               ├── Solucion.java             # Lista de operaciones
│               └── SolucionJugador.java      # Contenedor: jugador + solución + resultado
│
├── README.md                                  # Este fichero
└── [build files]                              # Compilados y dependencias
```



## ⚠️ Consideraciones Importantes

### 1. Serialización de Soluciones
- Las soluciones se envían serializadas como `Object` en mensajes ACL
- Las clases `Operacion` y `Solucion` deben implementar `Serializable`

### 2. Limpieza de Cola de Mensajes
- Los jugadores deben limpiar la cola al recibir la nueva cuenta atrás
- Evita procesar mensajes antiguos de rondas anteriores
- Implementado con lectura no-bloqueante hasta que `receive()` devuelve `null`

### 3. Timeouts
- Aitor: 2 segundos sin ganadores → inicia nueva ronda
- David: 2 segundos de lectura para procesar soluciones
- Jugador: 38-40 segundos para calcular solución

### 4. Modo Automático vs Manual
- **Automático**: No requiere interacción, sólo observar
- **Manual**: Requiere entrada del usuario, ejemplo:
  ```
  opciones
  Operando1 operador Operando2
  E) para enviar y finalizar
  R) reiniciar
  
  > 2+1
  3
  > 3*20
  60
  > E
  Solución registrada
  ```

### 5. Registro en Directory Facilitator
- **Aitor**: Servicio "Presentador"
- **David**: Servicio "ExpertoCifras"
- **Jugadores**: Servicio "Jugador"
- Los agentes se buscan dinámicamente en el DF



## 📖 Referencias Técnicas

### JADE Framework
- **Agentes**: Ejecución concurrente independiente
- **Behaviours**: Acciones repetitivas o de un solo disparo
- **ACL Messages**: Comunicación asíncrona entre agentes
- **Directory Facilitator**: Registro y descubrimiento de servicios

### Clases Clave
- `jade.core.Agent`: Clase base para todos los agentes
- `jade.core.behaviours.Behaviour`: Comportamiento base
- `jade.core.behaviours.CyclicBehaviour`: Repetición infinita
- `jade.lang.acl.ACLMessage`: Mensaje FIPA
- `jade.domain.DFService`: Servicio de registro

### Conceptos Avanzados
- **Mensajes serializados**: Paso de objetos Java complejos
- **Timeouts**: `block(long timeout)` en behaviours
- **Message Templates**: Filtrado de mensajes
- **Non-blocking receive**: Lectura sin espera

## 👥 Créditos

**Asignatura**: Programación de Agentes (PA1)  
**Universidad**: Universidad de Salamanca  
**Autores de la implementación**: Carolina De Jesús Arolas & Lidia Villarreal  
**Fecha**: 2025

## 📄 Licencia

Proyecto educativo - Universidad de Salamanca

---

**Última actualización**: Febrero 2026


**Notas Finales**:
- El programa mostrará la consola de JADE (GUI = true)
- Observa los mensajes de los agentes en la consola
- Múltiples rondas se ejecutan automáticamente
- Para terminar: Cierra la ventana de JADE

---

## 🎓 Material de Referencia

- **Programa original**: https://www.rtve.es/play/videos/cifras-y-letras/
- **JADE Documentation**: https://jade.tilab.com/
- **Formato ACL**: FIPA Agent Communication Language
- **Algoritmos de búsqueda**: Recursión, backtracking, heurísticas
