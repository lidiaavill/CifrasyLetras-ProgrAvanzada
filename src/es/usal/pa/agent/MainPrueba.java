package es.usal.pa.agent;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

/**
 * Clase Main para probar el Agente Aitor
 */
public class MainPrueba {

    public static void main(String[] args) {

        System.out.println("🚀 Iniciando plataforma JADE...\n");

        // 1. Obtener la instancia del Runtime de JADE
        Runtime rt = Runtime.instance();

        // 2. Crear el perfil de configuración
        Profile p = new ProfileImpl();
        p.setParameter(Profile.MAIN_HOST, "localhost");  // Ejecutar en local
        p.setParameter(Profile.GUI, "true");             // Activar GUI (interfaz gráfica)

        // 3. Crear el contenedor principal
        ContainerController cc = rt.createMainContainer(p);

        try {
            // 4. Crear el agente Aitor
            System.out.println("Creando agente Aitor...\n");

            AgentController aitor = cc.createNewAgent(
                    "Aitor",                           // Nombre del agente
                    "es.usal.pa.agent.AgenteAitor",   // ⚠️ Clase completa con tu paquete
                    null                               // Argumentos (ninguno por ahora)
            );

            // 5. Iniciar el agente
            aitor.start();

            System.out.println(" Agente Aitor creado y arrancado correctamente\n");

        } catch (StaleProxyException e) {
            System.err.println("Error al crear el agente:");
            e.printStackTrace();
        }
    }
}