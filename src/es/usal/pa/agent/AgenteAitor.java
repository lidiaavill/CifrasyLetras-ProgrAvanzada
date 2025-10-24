package es.usal.pa.agent;

import jade.core.Agent;

/**
 * Agente Aitor - Presentador del juego Cifras y Letras
 * Responsable de:
 * - Hacer cuenta atrás de 15 a 0
 * - Dar el turno a David
 * - Esperar a que David comunique los ganadores
 * - Reiniciar el ciclo
 */
public class AgenteAitor extends Agent {

    /**
     * Método setup() - Se ejecuta UNA VEZ cuando el agente se crea
     * Es como el "nacimiento" del agente
     */
    @Override
    protected void setup() {
        // Mensaje de bienvenida para saber que el agente se ha creado correctamente
        System.out.println("╔═══════════════════════════════════╗");
        System.out.println("║   Agente AITOR iniciado           ║");
        System.out.println("║  Presentador de Cifras y Letras   ║");
        System.out.println("╚═══════════════════════════════════╝");

        // Mostrar el nombre del agente (lo verás en la GUI de JADE)
        System.out.println("Mi nombre es: " + getAID().getName());
        System.out.println("Estoy listo para comenzar el juego\n");

        // TODO: Aquí añadiremos más cosas después (obtener jugadores, añadir behaviours, etc.)
    }

    /**
     * Método takeDown() - Se ejecuta cuando el agente se destruye
     * Es como la "muerte" del agente (opcional pero buena práctica)
     */
    @Override
    protected void takeDown() {
        System.out.println("Agente AITOR finalizando...");
    }
}