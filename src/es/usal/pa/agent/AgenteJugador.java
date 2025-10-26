package es.usal.pa.agent;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import es.usal.pa.agent.modelo.TipoMensaje;

/**
 * Agente Jugador - Participante del juego Cifras y Letras
 * Escucha mensajes de Aitor y David
 *
 * @author Lidia
 */
public class AgenteJugador extends Agent {

    @Override
    protected void setup() {
        System.out.println("╔═══════════════════════════════════╗");
        System.out.println("║   Jugador " + getLocalName() + " conectado          ║");
        System.out.println("╚═══════════════════════════════════╝");

        // Registrarse en el Directory Facilitator (DF) como "Jugador"
        registrarseEnDF();

        System.out.println("Jugador " + getLocalName() + " listo para jugar\n");

        // Añadir behaviour para recibir mensajes
        addBehaviour(new ComportamientoRecibirMensajes());
    }

    /**
     * Registra este agente en el DF con el servicio tipo "Jugador"
     * Así Aitor y David podrán encontrarlo
     */
    private void registrarseEnDF() {
        // 1. Crear la descripción del agente
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());  // Mi identificador

        // 2. Crear la descripción del servicio que ofrezco
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Jugador");           // IMPORTANTE: Tipo que buscará Aitor
        sd.setName("Servicio-Jugador-" + getLocalName());
        dfd.addServices(sd);

        try {
            // 3. Registrarme en el DF
            DFService.register(this, dfd);
            System.out.println("✅ " + getLocalName() + " registrado en el DF como 'Jugador'");
        } catch (FIPAException e) {
            System.err.println("❌ Error al registrarse en el DF:");
            e.printStackTrace();
        }
    }

    /**
     * CLASE INTERNA: Behaviour para recibir mensajes
     *
     * CyclicBehaviour = Se ejecuta indefinidamente (nunca termina)
     * Perfecto para estar siempre escuchando mensajes
     */
    private class ComportamientoRecibirMensajes extends CyclicBehaviour {

        @Override
        public void action() {
            // Recibir mensaje (no bloqueante)
            ACLMessage mensaje = receive();

            if (mensaje != null) {
                // Hay mensaje, procesarlo
                procesarMensaje(mensaje);
            } else {
                // No hay mensaje, bloquear hasta que llegue uno
                block();
            }
        }

        /**
         * Procesa el mensaje recibido según su tipo
         */
        private void procesarMensaje(ACLMessage mensaje) {
            String contenido = mensaje.getContent();

            // El contenido viene en formato: "TIPO_MENSAJE:valor"
            if (contenido.startsWith(TipoMensaje.AITOR_TIEMPO_JUGADORES.toString())) {
                /*Procesamiento silencioso, Aitor ya muestra la cuenta atrás 
                // Extraer el tiempo del mensaje
                String[] partes = contenido.split(":");
                if (partes.length > 1) {
                    String tiempo = partes[1];
                    System.out.println("[" + myAgent.getLocalName() + "] ⏰ Tiempo: " + tiempo);
                }*/
            } else if (contenido.equals(TipoMensaje.AITOR_TURNO_DAVID_JUGADORES.toString())){
                //Mensaje de inicio de ronda de cifras
                System.out.println("   • [" + myAgent.getLocalName() + "] Preparado para jugar cifras");
            } else if (contenido.startsWith(TipoMensaje.DAVID_GANADOR_JUGADORES_AITOR.toString())){
                // Mensaje de ganador
                // Formato: DAVID_GANADOR_JUGADORES_AITOR:NombreGanador:Solución
                String[] partes = contenido.split(":", 3);

                if(partes.length>=3){
                    String nombreGanador = partes[1];
                    
                    //solo imprimir si soy el ganador
                    if(nombreGanador.equals(myAgent.getLocalName()))
                        System.out.println("   ✓ [" + myAgent.getLocalName() + "] ¡He ganado esta ronda!");
                } else if (partes.length>=2 && partes[1].equals("Ninguno"))
                    System.out.println("[" + myAgent.getLocalName() + "] ⚠ No hubo ganadores en esta ronda"); 
            } else{
                //otro mensaje para debug
                System.out.println("[" + myAgent.getLocalName() + "] 📨 Mensaje: " + contenido);
            }   
        }
    }

    @Override
    protected void takeDown() {
        // Importante: Des-registrarse del DF al morir
        try {
            DFService.deregister(this);
            System.out.println("Jugador " + getLocalName() + " des-registrado del DF");
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        System.out.println("Jugador " + getLocalName() + " finalizando...");
    }
}