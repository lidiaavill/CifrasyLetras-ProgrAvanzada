package es.usal.pa.agent;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import es.usal.pa.cifras.controlador.CallableSolucionAutomatica.NivelJugador; // ← NUEVO IMPORT

/**
 * Clase Main para probar el sistema con jugadores de diferentes niveles
 *
 * @author Lidia & Carolina
 */
public class MainPrueba {

    public static void main(String[] args) {

        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║     Cifras y Letras - Práctica PA1         ║");
        System.out.println("║    Carolina De Jesús y Lidia Villarreal    ║");
        System.out.println("╚════════════════════════════════════════════╝\n");

        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        p.setParameter(Profile.MAIN_HOST, "localhost");
        p.setParameter(Profile.GUI, "true");

        ContainerController cc = rt.createMainContainer(p);

        try {

            // Jugador EXPERTO
            AgentController jugador1 = cc.createNewAgent(
                    "Fran",
                    "es.usal.pa.agent.AgenteJugador",
                    new Object[] {true, NivelJugador.EXPERTO}  //
            );
            jugador1.start();

            // Jugador INTERMEDIO
            AgentController jugador2 = cc.createNewAgent(
                    "Maria",
                    "es.usal.pa.agent.AgenteJugador",
                    new Object[] {true, NivelJugador.INTERMEDIO}
            );
            jugador2.start();

            // Jugador PRINCIPIANTE
            AgentController jugador3 = cc.createNewAgent(
                    "Carlos",
                    "es.usal.pa.agent.AgenteJugador",
                    new Object[] {true, NivelJugador.PRINCIPIANTE}
            );
            jugador3.start();

            // Jugador ALEATORIO (comportamiento impredecible)
            AgentController jugador4 = cc.createNewAgent(
                    "Ana",
                    "es.usal.pa.agent.AgenteJugador",
                    new Object[] {true, NivelJugador.ALEATORIO}
            );
            jugador4.start();



            // Esperar para que los jugadores se registren en el DF
            Thread.sleep(1000);

            // Crear el Experto David
            AgentController david = cc.createNewAgent(
                    "ExpertoDavid",
                    "es.usal.pa.agent.AgenteExpertoDavid",
                    null
            );
            david.start();

            // Esperar a que David se registre
            Thread.sleep(1500);

            // Crear al presentador Aitor
            AgentController aitor = cc.createNewAgent(
                    "Aitor",
                    "es.usal.pa.agent.AgenteAitor",
                    null
            );
            aitor.start();


        } catch (StaleProxyException e) {
            System.err.println("❌ Error al crear los agentes:");
            e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println("❌ Error en la espera:");
            e.printStackTrace();
        }
    }
}