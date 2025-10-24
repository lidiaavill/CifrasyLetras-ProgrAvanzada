package es.usal.pa.agent;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

/**
 * Clase Main para probar el sistema de agentes
 * Crea: Jugadores → Aitor (que los buscará)
 *
 * @author Lidia
 */
public class MainPrueba {

    public static void main(String[] args) {

        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║  🚀 Iniciando plataforma JADE             ║");
        System.out.println("║     Cifras y Letras - Práctica PA         ║");
        System.out.println("╚════════════════════════════════════════════╝\n");

        // 1. Obtener la instancia del Runtime de JADE
        Runtime rt = Runtime.instance();

        // 2. Crear el perfil de configuración
        Profile p = new ProfileImpl();
        p.setParameter(Profile.MAIN_HOST, "localhost");  // Ejecutar en local
        p.setParameter(Profile.GUI, "true");             // Activar GUI (interfaz gráfica)

        // 3. Crear el contenedor principal
        ContainerController cc = rt.createMainContainer(p);

        try {
            System.out.println("📋 Paso 1: Creando JUGADORES...\n");

            // 4. Crear varios jugadores
            AgentController jugador1 = cc.createNewAgent(
                    "Fran",                              // Nombre del jugador
                    "es.usal.pa.agent.AgenteJugador",   // Clase del agente
                    null
            );
            jugador1.start();

            AgentController jugador2 = cc.createNewAgent(
                    "Maria",
                    "es.usal.pa.agent.AgenteJugador",
                    null
            );
            jugador2.start();

            AgentController jugador3 = cc.createNewAgent(
                    "Carlos",
                    "es.usal.pa.agent.AgenteJugador",
                    null
            );
            jugador3.start();

            // 5. IMPORTANTE: Esperar un poco para que los jugadores se registren en el DF
            System.out.println("⏳ Esperando que los jugadores se registren en el DF...\n");
            Thread.sleep(1000);  // 1 segundo

            System.out.println("📋 Paso 2: Creando PRESENTADOR (Aitor)...\n");

            // 6. Crear el agente Aitor (que buscará a los jugadores)
            AgentController aitor = cc.createNewAgent(
                    "Aitor",
                    "es.usal.pa.agent.AgenteAitor",
                    null
            );
            aitor.start();

            System.out.println("\n✅ Sistema iniciado correctamente");
            System.out.println("📌 Mira la consola para ver los resultados");
            System.out.println("📌 Abre la GUI de JADE para ver los agentes\n");

        } catch (StaleProxyException e) {
            System.err.println("❌ Error al crear los agentes:");
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("❌ Error en la espera:");
            e.printStackTrace();
        }
    }
}