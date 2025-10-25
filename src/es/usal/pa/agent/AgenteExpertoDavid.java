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
 * Agente Experto David - Versión básica de prueba
 * Solo recibe el mensaje de turno y lo muestra
 * 
 * @author Lidia
 */
public class AgenteExpertoDavid extends Agent {

    @Override
    protected void setup() {
        System.out.println("╔═══════════════════════════════════╗");
        System.out.println("║   Agente DAVID iniciado           ║");
        System.out.println("║   Experto en Cifras               ║");
        System.out.println("╚═══════════════════════════════════╝");
        
        System.out.println("Mi nombre es: " + getAID().getName());
        System.out.println();
        
        // IMPORTANTE: Registrarse en el DF para que Aitor pueda encontrarnos
        registrarEnDF();
        
        // Añadir behaviour para recibir mensajes
        addBehaviour(new RecibirMensajesBehaviour());
    }
    
    /**
     * Registra a David en el Directory Facilitator (DF)
     * Esto es ESENCIAL para que Aitor pueda encontrarlo
     */
    private void registrarEnDF() {
        // Crear descripción del agente
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        
        // Crear descripción del servicio
        ServiceDescription sd = new ServiceDescription();
        sd.setType("ExpertoCifras");  // ⭐ IMPORTANTE: Mismo tipo que busca Aitor
        sd.setName("David");
        dfd.addServices(sd);
        
        try {
            // Registrar en el DF
            DFService.register(this, dfd);
            System.out.println("✓ David registrado en el DF como 'ExpertoCifras'");
            System.out.println();
        } catch (FIPAException e) {
            System.err.println("❌ Error al registrar a David en el DF:");
            e.printStackTrace();
        }
    }
    
    /**
     * Behaviour para recibir mensajes (versión básica)
     */
    private class RecibirMensajesBehaviour extends CyclicBehaviour {
        
        @Override
        public void action() {
            // Esperar cualquier mensaje
            ACLMessage mensaje = myAgent.receive();
            
            if (mensaje != null) {
                // Procesar el mensaje
                String contenido = mensaje.getContent();
                
                System.out.println("═══════════════════════════════════");
                System.out.println("📨 DAVID ha recibido un mensaje:");
                System.out.println("   Remitente: " + mensaje.getSender().getLocalName());
                System.out.println("   Contenido: " + contenido);
                System.out.println("═══════════════════════════════════");
                
                // Verificar si es un mensaje de turno
                if (contenido != null && contenido.contains(TipoMensaje.AITOR_TURNO_DAVID_JUGADORES.toString())) {
                    System.out.println("✓ ¡Es mi turno! Mensaje de Aitor recibido correctamente");
                    System.out.println("   (Aquí iría la lógica de la ronda de cifras)");
                    System.out.println();
                }
                
            } else {
                // No hay mensajes, bloquear hasta que llegue uno
                block();
            }
        }
    }
    
    /**
     * Método que se ejecuta cuando el agente se destruye
     */
    @Override
    protected void takeDown() {
        // Desregistrarse del DF
        try {
            DFService.deregister(this);
            System.out.println("David desregistrado del DF");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        
        System.out.println("Agente DAVID finalizando...");
    }
}
