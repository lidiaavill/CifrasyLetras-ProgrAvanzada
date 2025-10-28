package es.usal.pa.cifras.modelo;
import es.usal.pa.cifras.modelo.Operacion;
import es.usal.pa.cifras.modelo.Solucion;
import jade.core.AID;
import es.usal.pa.cifras.modelo.Solucion;

/**
 * Clase para almacenar la solución de un jugador
 * Contiene: nombre del jugador, su solución propuesta y el resultado obtenido
 *
 * @author Lidia
 */
public class SolucionJugador {

    // ========== CAMPOS ==========

    /**
     * Nombre del jugador que envió la solución
     */
    private String nombreJugador;

    /**
     * Solución propuesta por el jugador (lista de operaciones)
     */
    private Solucion solucion;

    /**
     * Resultado final obtenido por el jugador
     * (puede ser null si la solución es inválida)
     */
    private Integer resultadoObtenido;

    // ========== CONSTRUCTOR ==========

    /**
     * Constructor vacío
     */
    public SolucionJugador() {
    }

    /**
     * Constructor con todos los campos
     *
     * @param nombreJugador Nombre del jugador
     * @param solucion Solución propuesta
     * @param resultadoObtenido Resultado calculado
     */
    public SolucionJugador(String nombreJugador, Solucion solucion, Integer resultadoObtenido) {
        this.nombreJugador = nombreJugador;
        this.solucion = solucion;
        this.resultadoObtenido = resultadoObtenido;
    }

    /**
     * Constructor sin resultado (se calculará después)
     *
     * @param nombreJugador Nombre del jugador
     * @param solucion Solución propuesta
     */
    public SolucionJugador(String nombreJugador, Solucion solucion) {
        this.nombreJugador = nombreJugador;
        this.solucion = solucion;
        this.resultadoObtenido = null;
    }

    // ========== GETTERS Y SETTERS ==========

    public String getNombreJugador() {
        return nombreJugador;
    }

    public void setNombreJugador(String nombreJugador) {
        this.nombreJugador = nombreJugador;
    }

    public Solucion getSolucion() {
        return solucion;
    }

    public void setSolucion(Solucion solucion) {
        this.solucion = solucion;
    }

    public Integer getResultadoObtenido() {
        return resultadoObtenido;
    }

    public void setResultadoObtenido(Integer resultadoObtenido) {
        this.resultadoObtenido = resultadoObtenido;
    }

    // ========== MÉTODOS AUXILIARES (OPCIONAL) ==========

    /**
     * Calcula la distancia entre el resultado obtenido y el valor buscado
     *
     * @param valorBuscado Número objetivo
     * @return Distancia absoluta (o Integer.MAX_VALUE si no hay resultado)
     */
    public int calcularDistancia(Integer valorBuscado) {
        if (resultadoObtenido == null) {
            return Integer.MAX_VALUE; // Solución inválida
        }
        return Math.abs(valorBuscado - resultadoObtenido);
    }

    /**
     * Verifica si la solución es válida (tiene resultado)
     *
     * @return true si es válida, false si no
     */
    public boolean esValida() {
        return resultadoObtenido != null;
    }

    @Override
    public String toString() {
        return "SolucionJugador{" +
                "nombreJugador='" + nombreJugador + '\'' +
                ", resultadoObtenido=" + resultadoObtenido +
                ", numOperaciones=" + (solucion != null ? solucion.getListaOperacion().size() : 0) +
                '}';
    }
}