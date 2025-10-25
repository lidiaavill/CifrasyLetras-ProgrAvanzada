package es.usal.pa.agent;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
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

        //Buscamos al experto David
        System.out.println("Buscando al experto David...");
        AID david = obtenerExpertoDavid();

        if(david!=null)
            System.out.println("Experto david encontrado: " + david.getLocalName());
        else
            System.out.print("No se ha encontrado el experto David");
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

    /*
     * Busca al agente que proporciona el servicio ExpertoCifras
     * @return AID del experto david o null si no se encuentra
     */
    @SuppressWarnings("removal")
    private AID obtenerExpertoDavid(){
        //1.Crear la plantilla de busqueda 
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("ExpertoCifras");
        template.addServices(sd);

        //2. Restricciones de búsqueda
        SearchConstraints sc = new SearchConstraints();
        sc.setMaxResults(new Long(1)); //solo necesitamos 1

        try{
            //3. Buscamos en DF
            DFAgentDescription[] results = DFService.search(this, template, sc);
            //4. Si encontramos resultados, devolver el primero
            if (results != null && results.length>0)
                return results[0].getName();
        } catch (FIPAException e){
            System.err.println("Error al buscar al experto David en el DF");
            e.printStackTrace();
        }
        //No encontramos a David
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

    /*
     * Enviamos el mensaje de turno a David y a todos los jugadores
     * El mensaje indica que comienza la ronda de cifras
     */
    private void enviarMensajeTurno(){
        System.out.println("═══════════════════════════════════");
        System.out.println("   Enviando mensaje de TURNO");
        System.out.println("═══════════════════════════════════");
        
        //1. Buscamos a David
        AID david = obtenerExpertoDavid();
        if(david==null){
            System.err.println("No se puede enviar turno, David no encontrado");
            return;
        }

        //2. Buscar a los jugadores
        AID [] jugadores = obtenerJugadores();
        
        //3. Creamos el mensaje
        ACLMessage mensajeTurno = new ACLMessage(ACLMessage.INFORM);

        //4.Añadir a David como receptor
        mensajeTurno.addReceiver(david);
        System.out.println("Destinatario: " + david.getLocalName());

        //5. Añadimos a todos los jugadores como receptores
        if(jugadores != null && jugadores.length > 0){
            for(AID jugador : jugadores){
                mensajeTurno.addReceiver(jugador);
                System.out.println("Destinatario: " + jugador.getLocalName());
            }
        }
        else
            System.out.println("No hay jugadores conectados");

        //6. Establecer el contenido del mensaje
        mensajeTurno.setContent(TipoMensaje.AITOR_TURNO_DAVID_JUGADORES.toString());
        mensajeTurno.setConversationId("Cifras-letras");

        //7. Enviar mensaje
        send(mensajeTurno);

        System.out.println("✓ Mensaje de turno enviado correctamente");
        System.out.println("═══════════════════════════════════\n");
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

                //Aquí enviaremos mensaje a David y Jugadores para iniciar ronda
                enviarMensajeTurno();
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

        /*
         * CLASE INTERNA: Behaviour para esperar ganadores de David
         */
        private class ComportamientoEsperaGanadores extends CyclicBehaviour{
            private boolean ganadorRecibido = false;
            private long tiempoUltimoMensaje = 0;
            private static final long TIMEOUT = 2000; //2seg sin mensajes

            public void action(){
                MessageTemplate template = MessageTemplate.and(
                                    MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
                                    MessageTemplate.MatchConversationId("cifras-letras"));
                
                ACLMessage mensaje = myAgent.receive(template);

                if(mensaje != null){
                    String contenido = mensaje.getContent();

                    //Verificar si es un mensaje de ganador
                    if (contenido.contains(TipoMensaje.DAVID_GANADOR_JUGADORES_AITOR.toString())) {
                        System.out.println("═══════════════════════════════════");
                        System.out.println("🏆 AITOR recibió ganador:");
                        System.out.println("   " + contenido);
                        System.out.println("═══════════════════════════════════");
                        
                        ganadorRecibido = true;
                        tiempoUltimoMensaje = System.currentTimeMillis();
                    }
                } else {
                    //No hay mensajes
                    if(ganadorRecibido){
                        long tiempoTranscurrido = System.currentTimeMillis() - tiempoUltimoMensaje;

                        if(tiempoTranscurrido >= TIMEOUT){
                            // No hay más ganadores, iniciar nueva ronda
                            System.out.println("\n" + "─".repeat(45));
                            System.out.println("✓ Ronda finalizada");
                            System.out.println("🔄 Iniciando nueva ronda...");
                            System.out.println("─".repeat(45) + "\n");

                            //Iniciar nueva cuenta atrás
                            myAgent.addBehaviour(new ComportamientoCuentaAtras());

                            //Terminar behaviour
                            myAgent.removeBehaviour(this);
                        }
                    }
                    block(500);
                }       
            }
        }

        @Override
        protected void takeDown(){
            System.out.println("Agente Aitor finalizando...");
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