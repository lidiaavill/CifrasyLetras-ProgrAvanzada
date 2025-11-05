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
                    new Object [] {false} //AUTOMÁTICO
            );
            jugador1.start();

           AgentController jugador2 = cc.createNewAgent(
                    "Maria",
                    "es.usal.pa.agent.AgenteJugador",
                    new Object [] {false} //AUTOMÁTICO
            );
            jugador2.start();

            AgentController jugador3 = cc.createNewAgent(
                    "Carlos",
                    "es.usal.pa.agent.AgenteJugador",
                    new Object [] {true} //AUTOMÁTICO
            );
            jugador3.start();


            // 5. IMPORTANTE: Esperar un poco para que los jugadores se registren en el DF
            System.out.println("⏳ Esperando que los jugadores se registren en el DF...\n");
            Thread.sleep(1000);  // 1 segundo

            // Creamos al presentador David
            System.out.println("📋 Paso 2: Creando EXPERTO DAVID...");
            System.out.println("────────────────────────────────────────────\n");

            AgentController david = cc.createNewAgent(
                            "ExpertoDavid", 
                            "es.usal.pa.agent.AgenteExpertoDavid", 
                            null);
            david.start();

            //Esperamos a que David se registre en el DF
            System.out.println("\n⏳ Esperando que David se registre en el DF...");
            Thread.sleep(1500);  // 1.5 segundos (más tiempo para David)
            System.out.println("✅ David registrado\n");

            System.out.println("📋 Paso 2: Creando PRESENTADOR (Aitor)...\n");

            // 6. Crear el agente Aitor (que buscará a los jugadores)
            AgentController aitor = cc.createNewAgent(
                    "Aitor",
                    "es.usal.pa.agent.AgenteAitor",
                    null
            );
            aitor.start();

            //Resumen final
            System.out.println("\n╔════════════════════════════════════════════╗");
            System.out.println("║  ✅ Sistema iniciado correctamente         ║");
            System.out.println("║                                            ║");
            System.out.println("║  Agentes creados:                          ║");
            System.out.println("║   • 3 Jugadores (Fran, Maria, Carlos)      ║");
            System.out.println("║   • 1 Experto (David)                      ║");
            System.out.println("║   • 1 Presentador (Aitor)                  ║");
            System.out.println("║                                            ║");
            System.out.println("║  📌 Mira la consola para ver el juego     ║");
            System.out.println("║  📌 Abre la GUI de JADE para ver agentes  ║");
            System.out.println("╚════════════════════════════════════════════╝\n");


        } catch (StaleProxyException e) {
            System.err.println("❌ Error al crear los agentes:");
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("❌ Error en la espera:");
            e.printStackTrace();
        }
    }
}