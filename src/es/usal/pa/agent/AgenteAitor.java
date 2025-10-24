package es.usal.pa.agent;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;
import es.usal.pa.agent.modelo.TipoMensaje;
import es.usal.pa.agent.modelo.VariablesConfiguracion;

/**
 * Agente Aitor - Presentador del juego Cifras y Letras
 * Responsable de:
 * - Hacer cuenta atrás de 15 a 0
 * - Dar el turno a David
 * - Esperar a que David comunique los ganadores
 * - Reiniciar el ciclo
 *
 * @author Lidia
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

        // Mostrar el nombre del agente
        System.out.println("Mi nombre es: " + getAID().getName());
        System.out.println("Estoy listo para comenzar el juego\n");

        // PRUEBA: Buscar jugadores conectados
        System.out.println("Buscando jugadores conectados...");
        AID[] jugadores = obtenerJugadores();

        if (jugadores != null && jugadores.length > 0) {
            System.out.println("Se han encontrado " + jugadores.length + " jugador(es):");
            for (int i = 0; i < jugadores.length; i++) {
                System.out.println("   " + (i+1) + ". " + jugadores[i].getLocalName());
            }
        } else {
            System.out.println("⚠No se han encontrado jugadores conectados");
        }
        System.out.println();

        //Behaviour de cuenta atrás
        System.out.println("Iniciando cuenta atrás...\n");
        addBehaviour(new ComportamientoCuentaAtras());
    }



    /**
     * Busca todos los agentes que proporcionan el servicio "Jugador"
     *
     * @return Array con los AID de los jugadores encontrados, o null si no hay ninguno
     */

    private AID[] obtenerJugadores() {

        // 1. Crear la plantilla de búsqueda
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Jugador");  // Buscamos agentes con servicio tipo "Jugador"
        template.addServices(sd);

        // 2. Configurar restricciones de búsqueda (queremos todos los resultados)
        SearchConstraints sc = new SearchConstraints();
        sc.setMaxResults(Long.MAX_VALUE);  // Sin límite de resultados

        try {
            // 3. Realizar la búsqueda en el DF (Directory Facilitator)
            DFAgentDescription[] results = DFService.search(this, template, sc);

            // 4. Extraer los AID de los resultados
            if (results != null && results.length > 0) {
                AID[] jugadores = new AID[results.length];
                for (int i = 0; i < results.length; i++) {
                    jugadores[i] = results[i].getName();
                }
                return jugadores;
            }

        } catch (FIPAException e) {
            // Error al buscar en el DF
            System.err.println("Error al buscar jugadores en el DF:");
            e.printStackTrace();
        }

        // Si llegamos aquí, no se encontraron jugadores o hubo un error
        return null;
    }


    /**
     * Envía un mensaje a todos los jugadores conectados
     *
     * @param tipoMensaje Tipo de mensaje (AITOR_TIEMPO_JUGADORES, etc.)
     * @param contenido Contenido del mensaje
     */

    private void enviarMensajeJugadores(TipoMensaje tipoMensaje, String contenido) {
        // Buscar jugadores cada vez que se envía un mensaje
        // (por si se conectan/desconectan durante el juego)
        AID[] jugadores = obtenerJugadores();

        if (jugadores == null || jugadores.length == 0) {
            return; // No hay jugadores, no enviamos nada
        }

        // Crear el mensaje ACL
        ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);

        // Añadir todos los jugadores como receptores
        for (AID jugador : jugadores) {
            mensaje.addReceiver(jugador);
        }

        // Establecer el contenido: TipoMensaje + contenido
        mensaje.setContent(tipoMensaje + ":" + contenido);
        mensaje.setConversationId("cifras-letras");

        // Enviar el mensaje
        send(mensaje);
    }


    /**
     * CLASE INTERNA: Behaviour para la cuenta atrás
     *
     * Este behaviour cuenta de 15 a 0 y envía mensajes a los jugadores
     * informándoles del tiempo restante
     */

    private class ComportamientoCuentaAtras extends Behaviour {

        // Variable para llevar la cuenta (empieza en 15)
        private int contador = VariablesConfiguracion.avisosInicioRonda; // 15

        // Variable para saber cuándo terminar
        private boolean terminado = false;

        /**
         * Método action() - Se ejecuta repetidamente mientras done() devuelva false
         * Es el "corazón" del behaviour
         */

        @Override
        public void action() {
            // 1. Enviar mensaje a jugadores con el tiempo actual
            System.out.println("Aitor dice: " + contador);
            enviarMensajeJugadores(TipoMensaje.AITOR_TIEMPO_JUGADORES, String.valueOf(contador));

            // 2. Decrementar el contador
            contador--;

            // 3. Si llegamos a 0, terminamos
            if (contador < 0) {
                terminado = true;
                System.out.println("¡Cuenta atrás finalizada! Turno de David\n");

                // TODO: Aquí enviaremos mensaje a David y Jugadores para iniciar ronda
                // enviarMensajeTurno();
            } else {
                // 4. Esperar 1 segundo antes de la siguiente iteración
                block(1000); // Bloquea el behaviour durante 1000 ms (1 segundo)
            }
        }

        /**
         * Método done() - Indica si el behaviour ha terminado
         * @return true si ha terminado, false si debe seguir ejecutándose
         */
        @Override
        public boolean done() {
            return terminado;
        }

        /**
         * Método onEnd() - Se ejecuta cuando el behaviour termina (opcional)
         * @return Código de terminación (0 = OK)
         */
        @Override

        public int onEnd() {
            System.out.println("Behaviour de cuenta atrás finalizado\n");
            return 0;
        }
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