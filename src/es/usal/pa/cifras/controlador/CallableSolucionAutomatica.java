package es.usal.pa.cifras.controlador;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import es.usal.pa.cifras.modelo.Operacion;
import es.usal.pa.cifras.modelo.Solucion;

/**
 * Genera soluciones automáticamente con diferentes niveles de dificultad
 * Permite simular jugadores con distintas habilidades
 *
 * @author Lidia & Carolina
 */
public class CallableSolucionAutomatica implements Callable<Solucion> {

    public enum NivelJugador {
        EXPERTO,      // Encuentra solución óptima (o muy cercana)
        INTERMEDIO,   // Encuentra soluciones aceptables
        PRINCIPIANTE, // Puede no encontrar solución o encontrar muy alejada
        ALEATORIO     // Comportamiento impredecible
    }

    private List<Integer> numerosDisponibles;
    private Integer valorBuscado;
    private Solucion mejorSolucion;
    private int mejorDistancia;
    private volatile boolean interrumpido = false; //Volatile garantiza que todos los hilos vean el valor actualizado

    private NivelJugador nivel;
    private Random random;

    // Parámetros que varían según el nivel
    private int maxProfundidad;        // Cuántas operaciones explorar
    private double probabilidadError;   // Probabilidad de tomar malas decisiones
    private int maxIntentos;           // Intentos máximos de búsqueda
    private long tiempoMaximoBusqueda; // Tiempo máximo en milisegundos


    // ========== CONSTRUCTORES ==========


     //Constructor con nivel específico
     public CallableSolucionAutomatica(List<Integer> numeros, Integer objetivo, NivelJugador nivel) {
        this.numerosDisponibles = new ArrayList<>(numeros);
        this.valorBuscado = objetivo;
        this.mejorSolucion = new Solucion();
        this.mejorDistancia = Integer.MAX_VALUE;
        this.nivel = nivel;
        this.random = new Random();

        configurarNivel();
    }


    //Constructor que elige nivel aleatorio
    //Constructor sobrecargado con delegación
    public CallableSolucionAutomatica(List<Integer> numeros, Integer objetivo) {
        this(numeros, objetivo, elegirNivelAleatorio());
    }


    // ========== CONFIGURACIÓN POR NIVEL ==========

    private void configurarNivel() {
        switch(nivel) {
            case EXPERTO:
                maxProfundidad = 6;           // Explora todas las operaciones
                probabilidadError = 0.0;       // Sin errores
                maxIntentos = Integer.MAX_VALUE;
                tiempoMaximoBusqueda = 35000;  // 35 segundos
                break;

            case INTERMEDIO:
                maxProfundidad = 4;            // Explora menos profundidad
                probabilidadError = 0.15;      // 15% de decisiones subóptimas
                maxIntentos = 50000;           // Menos intentos
                tiempoMaximoBusqueda = 25000;  // 25 segundos
                break;

            case PRINCIPIANTE:
                maxProfundidad = 3;            // Poca profundidad
                probabilidadError = 0.35;      // 35% de decisiones malas
                maxIntentos = 10000;           // Pocos intentos
                tiempoMaximoBusqueda = 15000;  // 15 segundos
                break;

            case ALEATORIO:
                // Configuración aleatoria
                maxProfundidad = 2 + random.nextInt(4);           // 2-5
                probabilidadError = 0.1 + random.nextDouble() * 0.4; // 10%-50%
                maxIntentos = 5000 + random.nextInt(45000);       // 5k-50k
                tiempoMaximoBusqueda = 10000 + random.nextInt(25000); // 10-35s
                break;
        }
    }

    private static NivelJugador elegirNivelAleatorio() {
        NivelJugador[] niveles = NivelJugador.values();
        return niveles[new Random().nextInt(niveles.length)];
    }

    // ========== MÉTODO PRINCIPAL ==========

    @Override
    public Solucion call() throws Exception {
        //System.out.println("   🤖 [Nivel: " + nivel + "] Buscando solución...");
        long tiempoInicio = System.currentTimeMillis();

        // Iniciar búsqueda según el nivel
        if (nivel == NivelJugador.EXPERTO) {
            buscarSolucionRecursivaOptima(new ArrayList<>(numerosDisponibles), new Solucion(), 0);
        } else {
            buscarSolucionConLimitaciones(tiempoInicio);
        }

        if (interrumpido) {
            System.out.println("   ⏱️  Tiempo agotado.");
        }

        // Aplicar "errores" según el nivel
        aplicarErroresSegunNivel();

        if (mejorSolucion.getListaOperacion().isEmpty()) {
            System.out.println("      ⚠ No se encontró ninguna solución");
        } else {
            Integer resultado = calcularResultadoFinal(mejorSolucion, numerosDisponibles);
            //System.out.println("      Resultado: " + resultado + " (distancia: " + mejorDistancia + ")");
            //System.out.println("      Operaciones: " + mejorSolucion.getListaOperacion().size());
        }

        return mejorSolucion;
    }

    // ========== BÚSQUEDA ÓPTIMA (EXPERTO) ==========

    private void buscarSolucionRecursivaOptima(List<Integer> numerosActuales, Solucion solucionActual, int profundidad) {
        //Condición de parada: timeout o interrupción
         if (Thread.currentThread().isInterrupted() || interrumpido) {
            interrumpido = true;
            return;
        }

         //Condición de parada: alcanzamos profundidadMaxima
        if (profundidad >= maxProfundidad) {
            return;
        }

        //¿Esta solución es mejor que la que teníamos?
        evaluarSolucion(numerosActuales, solucionActual);

        //Condición de parada: No hay suficientes nº para operar
        if (numerosActuales.size() < 2) {
            return;
        }

        //Recursión: probar todas las combinaciones de 2 numeros con todos los operadores
        //Algoritmo exponencial O (6^n)
        for (int i = 0; i < numerosActuales.size(); i++) {
            for (int j = i + 1; j < numerosActuales.size(); j++) {
                Integer num1 = numerosActuales.get(i);
                Integer num2 = numerosActuales.get(j);

                probarOperacion(numerosActuales, solucionActual, i, j, num1, num2, '+', profundidad);
                probarOperacion(numerosActuales, solucionActual, i, j, num1, num2, '-', profundidad);
                probarOperacion(numerosActuales, solucionActual, i, j, num2, num1, '-', profundidad);
                probarOperacion(numerosActuales, solucionActual, i, j, num1, num2, '*', profundidad);
                probarOperacion(numerosActuales, solucionActual, i, j, num1, num2, '/', profundidad);
                probarOperacion(numerosActuales, solucionActual, i, j, num2, num1, '/', profundidad);
            }
        }
    }

    // ========== BÚSQUEDA CON LIMITACIONES (INTERMEDIO/PRINCIPIANTE) ==========

    private void buscarSolucionConLimitaciones(long tiempoInicio) {
        int intentos = 0;

        while (intentos < maxIntentos && !interrumpido) {
            // Verificar timeout
            if (System.currentTimeMillis() - tiempoInicio > tiempoMaximoBusqueda) {
                interrumpido = true;
                break;
            }

            // Generar una solución aleatoria (con heuristica)
            Solucion solucionPrueba = generarSolucionAleatoria();

            if (solucionPrueba != null) {
                Integer resultado = calcularResultadoFinal(solucionPrueba, numerosDisponibles);

                if (resultado != null) {
                    int distancia = Math.abs(valorBuscado - resultado);

                    if (distancia < mejorDistancia) {
                        mejorDistancia = distancia;
                        mejorSolucion = solucionPrueba;

                        if (distancia == 0) {
                            break; // Solución exacta encontrada
                        }
                    }
                }
            }

            intentos++;
        }
    }

    // ========== GENERACIÓN ALEATORIA CON HEURÍSTICAS ==========

    private Solucion generarSolucionAleatoria() {
        List<Integer> numerosActuales = new ArrayList<>(numerosDisponibles);
        Solucion solucion = new Solucion();

        int operaciones = Math.min(maxProfundidad, numerosActuales.size() - 1);

        for (int i = 0; i < operaciones && numerosActuales.size() >= 2; i++) {
            // Decidir si cometer un error intencional
            boolean cometerError = random.nextDouble() < probabilidadError;

            Operacion op;
            if (cometerError) {
                op = elegirOperacionAleatoria(numerosActuales);
            } else {
                op = elegirOperacionHeuristica(numerosActuales);
            }

            if (op != null) {
                Integer resultado = AuxOperacion.calcularOperacion(op);

                if (resultado != null) {
                    numerosActuales.remove(op.getOperando1());
                    numerosActuales.remove(op.getOperando2());
                    numerosActuales.add(resultado);
                    solucion.addOpereacion(op);
                } else {
                    break; // Operación inválida
                }
            }
        }

        return solucion;
    }

    private Operacion elegirOperacionAleatoria(List<Integer> numeros) {
        if (numeros.size() < 2) return null;

        int idx1 = random.nextInt(numeros.size());
        int idx2;
        do {
            idx2 = random.nextInt(numeros.size());
        } while (idx1 == idx2);

        Integer num1 = numeros.get(idx1);
        Integer num2 = numeros.get(idx2);

        char[] operadores = {'+', '-', '*', '/'};
        char op = operadores[random.nextInt(operadores.length)];

        // Evitar divisiones inválidas
        if (op == '/' && (num2 == 0 || num1 % num2 != 0)) {
            op = '+'; // Fallback
        }

        return new Operacion(num1, num2, op);
    }

    private Operacion elegirOperacionHeuristica(List<Integer> numeros) {
        // Heurística: priorizar operaciones que acerquen al objetivo
        List<Operacion> candidatas = new ArrayList<>();

        for (int i = 0; i < numeros.size(); i++) {
            for (int j = i + 1; j < numeros.size(); j++) {
                Integer n1 = numeros.get(i);
                Integer n2 = numeros.get(j);

                // Probar operaciones válidas
                agregarSiValida(candidatas, n1, n2, '+');
                agregarSiValida(candidatas, n1, n2, '-');
                agregarSiValida(candidatas, n2, n1, '-');
                agregarSiValida(candidatas, n1, n2, '*');
                agregarSiValida(candidatas, n1, n2, '/');
                agregarSiValida(candidatas, n2, n1, '/');
            }
        }

        if (candidatas.isEmpty()) return null;

        // Elegir la operación que más acerque al objetivo
        Operacion mejor = null;
        int mejorDistancia = Integer.MAX_VALUE;

        for (Operacion op : candidatas) {
            Integer res = AuxOperacion.calcularOperacion(op);
            if (res != null) {
                int dist = Math.abs(valorBuscado - res);
                if (dist < mejorDistancia) {
                    mejorDistancia = dist;
                    mejor = op;
                }
            }
        }

        return mejor != null ? mejor : candidatas.get(random.nextInt(candidatas.size()));
    }

    private void agregarSiValida(List<Operacion> lista, Integer n1, Integer n2, char op) {
        if (op == '/' && (n2 == 0 || n1 % n2 != 0)) return;
        if (op == '-' && n1 < n2) return; // Evitar negativos

        Operacion operacion = new Operacion(n1, n2, op);
        if (AuxOperacion.calcularOperacion(operacion) != null) {
            lista.add(operacion);
        }
    }

    // ========== APLICAR ERRORES FINALES ==========

    private void aplicarErroresSegunNivel() {
        // Principiantes a veces no encuentran nada
        if (nivel == NivelJugador.PRINCIPIANTE && random.nextDouble() < 0.25) {
            mejorSolucion = new Solucion(); // Solución vacía (no encontró nada)
            mejorDistancia = Integer.MAX_VALUE;
            System.out.println("      ❌ Este jugador no encontró solución");
        }
    }

    // ========== MÉTODOS AUXILIARES ==========

    private void probarOperacion(List<Integer> numerosActuales, Solucion solucionActual,
                                 int idx1, int idx2, Integer num1, Integer num2, char operador, int profundidad) {
        if (interrumpido) return; //Verificar timeout

        if (operador == '/' && num2 == 0) return; //No dividir por 0
        if (operador == '/' && num1 % num2 != 0) return; //Solo divisiones exactas
        if (operador == '-' && num1 < num2) return; //Evitar negativos

        Operacion op = new Operacion(num1, num2, operador);
        Integer resultado = AuxOperacion.calcularOperacion(op);

        //Si operción válida
        if (resultado != null) {
            List<Integer> nuevosNumeros = new ArrayList<>(numerosActuales);

            if (idx1 > idx2) {
                nuevosNumeros.remove(idx1);
                nuevosNumeros.remove(idx2);
            } else {
                nuevosNumeros.remove(idx2);
                nuevosNumeros.remove(idx1);
            }

            nuevosNumeros.add(resultado);

            Solucion nuevaSolucion = solucionActual.clone();
            nuevaSolucion.addOpereacion(op);

            buscarSolucionRecursivaOptima(nuevosNumeros, nuevaSolucion, profundidad + 1);
        }
    }

    private void evaluarSolucion(List<Integer> numerosActuales, Solucion solucion) {
        for (Integer num : numerosActuales) { //Revisar numeros disponibles
            int distancia = Math.abs(valorBuscado - num);

            //¿Es mejor que la actual?
            //0 si la distancia es igual, ¿tiene menos operaciones?
            if (distancia < mejorDistancia ||
                    (distancia == mejorDistancia && solucion.getListaOperacion().size() < mejorSolucion.getListaOperacion().size())) {

                mejorDistancia = distancia;
                mejorSolucion = solucion.clone();

                //Si encontramos numero exacto paramos
                if (distancia == 0) {
                    interrumpido = true;
                    return;
                }
            }
        }
    }

    private Integer calcularResultadoFinal(Solucion solucion, List<Integer> numerosOriginales) {
        return AuxSolucion.calcularSolucion(solucion, numerosOriginales, valorBuscado);
    }

    public Solucion getMejorSolucion() {
        return mejorSolucion;
    }

    public NivelJugador getNivel() {
        return nivel;
    }
}