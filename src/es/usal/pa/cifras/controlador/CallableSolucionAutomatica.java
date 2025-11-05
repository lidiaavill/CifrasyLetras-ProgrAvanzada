package es.usal.pa.cifras.controlador;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import es.usal.pa.cifras.modelo.Operacion;
import es.usal.pa.cifras.modelo.Solucion;

/**
 * Genera soluciones automáticamente mediante búsqueda exhaustiva con límite de tiempo
 *
 * @author Lidia
 */
public class CallableSolucionAutomatica implements Callable<Solucion> {

    private List<Integer> numerosDisponibles;
    private Integer valorBuscado;
    private Solucion mejorSolucion;
    private int mejorDistancia;
    private volatile boolean interrumpido = false;

    public CallableSolucionAutomatica(List<Integer> numeros, Integer objetivo) {
        this.numerosDisponibles = new ArrayList<>(numeros);
        this.valorBuscado = objetivo;
        this.mejorSolucion = new Solucion();
        this.mejorDistancia = Integer.MAX_VALUE;
    }

    @Override
    public Solucion call() throws Exception {
        System.out.println("   🤖 Buscando solución automáticamente...");

        // Iniciar búsqueda recursiva
        buscarSolucionRecursiva(new ArrayList<>(numerosDisponibles), new Solucion());

        if (interrumpido) {
            System.out.println("   ⏱️  Tiempo agotado. Mejor solución encontrada:");
        } else {
            System.out.println("   ✓ Búsqueda completada. Solución encontrada:");
        }

        if (mejorSolucion.getListaOperacion().isEmpty()) {
            System.out.println("      ⚠ No se encontró ninguna solución");
        } else {
            Integer resultado = calcularResultadoFinal(mejorSolucion, numerosDisponibles);
            System.out.println("      Resultado: " + resultado + " (distancia: " + mejorDistancia + ")");
            System.out.println("      Operaciones: " + mejorSolucion.getListaOperacion().size());
        }

        return mejorSolucion;
    }

    /**
     * Búsqueda recursiva exhaustiva de soluciones
     */
    private void buscarSolucionRecursiva(List<Integer> numerosActuales, Solucion solucionActual) {
        // Verificar si se interrumpió el hilo
        if (Thread.currentThread().isInterrupted() || interrumpido) {
            interrumpido = true;
            return;
        }

        // Evaluar la solución actual
        evaluarSolucion(numerosActuales, solucionActual);

        // Si solo queda un número, no podemos hacer más operaciones
        if (numerosActuales.size() < 2) {
            return;
        }

        // Probar todas las combinaciones de pares de números
        for (int i = 0; i < numerosActuales.size(); i++) {
            for (int j = i + 1; j < numerosActuales.size(); j++) {
                Integer num1 = numerosActuales.get(i);
                Integer num2 = numerosActuales.get(j);

                // Probar las 4 operaciones
                probarOperacion(numerosActuales, solucionActual, i, j, num1, num2, '+');
                probarOperacion(numerosActuales, solucionActual, i, j, num1, num2, '-');
                probarOperacion(numerosActuales, solucionActual, i, j, num2, num1, '-'); // Invertido
                probarOperacion(numerosActuales, solucionActual, i, j, num1, num2, '*');
                probarOperacion(numerosActuales, solucionActual, i, j, num1, num2, '/');
                probarOperacion(numerosActuales, solucionActual, i, j, num2, num1, '/'); // Invertido
            }
        }
    }

    /**
     * Prueba una operación específica y continúa la búsqueda recursivamente
     */
    /**
     * Prueba una operación específica y continúa la búsqueda recursivamente
     */
    private void probarOperacion(List<Integer> numerosActuales, Solucion solucionActual,
                                 int idx1, int idx2, Integer num1, Integer num2, char operador) {
        if (interrumpido) return;

        // ========== VALIDACIONES PREVIAS ==========

        // 1. Evitar divisiones por cero
        if (operador == '/' && num2 == 0) {
            return;
        }

        // 2. Evitar divisiones no exactas (solo divisiones enteras permitidas)
        if (operador == '/' && num1 % num2 != 0) {
            return;
        }

        // 3. Evitar restas que den resultados negativos (opcional, depende de las reglas)
        // Si las reglas del juego lo permiten, comenta estas líneas
        if (operador == '-' && num1 < num2) {
            return;
        }

        // Crear la operación
        Operacion op = new Operacion(num1, num2, operador);
        Integer resultado = AuxOperacion.calcularOperacion(op);

        // Si la operación es válida (AuxOperacion ya valida divisiones exactas)
        if (resultado != null) {
            // Crear nueva lista de números (sin los usados, con el resultado)
            List<Integer> nuevosNumeros = new ArrayList<>(numerosActuales);

            // IMPORTANTE: Remover en orden correcto para evitar problemas con índices
            if (idx1 > idx2) {
                nuevosNumeros.remove(idx1);
                nuevosNumeros.remove(idx2);
            } else {
                nuevosNumeros.remove(idx2);
                nuevosNumeros.remove(idx1);
            }

            nuevosNumeros.add(resultado);

            // Crear nueva solución con esta operación
            Solucion nuevaSolucion = solucionActual.clone();
            nuevaSolucion.addOpereacion(op);

            // Continuar búsqueda recursivamente
            buscarSolucionRecursiva(nuevosNumeros, nuevaSolucion);
        }
    }
    /**
     * Evalúa si la solución actual es mejor que la mejor encontrada hasta ahora
     */
    private void evaluarSolucion(List<Integer> numerosActuales, Solucion solucion) {
        // Buscar en los números actuales el más cercano al objetivo
        for (Integer num : numerosActuales) {
            int distancia = Math.abs(valorBuscado - num);

            // Si encontramos una mejor solución (menor distancia, o misma distancia con menos operaciones)
            if (distancia < mejorDistancia ||
                    (distancia == mejorDistancia && solucion.getListaOperacion().size() < mejorSolucion.getListaOperacion().size())) {

                mejorDistancia = distancia;
                mejorSolucion = solucion.clone();

                // Si encontramos la solución exacta, podemos parar
                if (distancia == 0) {
                    interrumpido = true;
                    return;
                }
            }
        }
    }

    /**
     * Calcula el resultado final de una solución
     */
    private Integer calcularResultadoFinal(Solucion solucion, List<Integer> numerosOriginales) {
        return AuxSolucion.calcularSolucion(solucion, numerosOriginales, valorBuscado);
    }

    /**
     * Obtiene la mejor solución encontrada hasta el momento
     */
    public Solucion getMejorSolucion() {
        return mejorSolucion;
    }
}