package es.usal.pa.agent.modelo;

/**
 * Constantes manejadas en el juego
 * @author Fran
 *
 */

/**
 * Aitor: cuenta atrñas
 * David: tiempos y delays
 * Jugado: limite de tiempo
 */
public class VariablesConfiguracion
{
	/**
	 * Cuenta atrás
	 */
    //NO ESPERA 15s ES MENOS -- mirar
	public static final int avisosInicioRonda=15000;
	
	/**
	 * Tiempo ronda de cifras
	 */
	//public static final long tiempoRondaCifras=40000;

    //He puesto 5000 para depurar y no estar esperando 40s, CAMBIAR LUEGO A LO DE ARRIBA
    public static final long tiempoRondaCifras=400000;


    /*
    Igual hay que poner estas aquí --> Lidia

    // Delay entre envío de números (milisegundos)
    public static final long delayEnvioNumeros = 100; // 0.1 segundos

    // Número de cifras a generar
    public static final int numeroCifras = 6;
     */
	
}
