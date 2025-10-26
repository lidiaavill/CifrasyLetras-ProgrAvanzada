package es.usal.pa.agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
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

    /* 
     * Obtenemos AID de aitor del DF
     */
    private AID obtenerAitor(){
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Presentador");
        template.addServices(sd);

        try{
            DFAgentDescription[] results = DFService.search(this, template);
            if(results !=null && results.length>0)
                return results[0].getName();
        }catch (FIPAException e){
            e.printStackTrace();
        }
        return new AID("Aitor", AID.ISLOCALNAME);
    }

    /*
     * Obtenemos los jugadores del DF
     */
    private AID[] obtenerJugadores(){
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Jugador");
        template.addServices(sd);

        SearchConstraints sc = new SearchConstraints();
        sc.setMaxResults(Long.MAX_VALUE);

        try{
            DFAgentDescription[] results = DFService.search(this, template, sc);

            if(results!=null && results.length>0){
                AID[] jugadores = new AID[results.length];
                for(int i=0; i< results.length;i++)
                    jugadores[i] = results[i].getName();
                return jugadores;
            } 
        } catch (FIPAException e){
                e.printStackTrace();
        }
        return null;
    }

    /*
     * Simula una ronda con envío de ganadores para ver si funciona ComportamientoEsperaGanadores
     */
    private void procesarRondaYEnviarGanadores() {
        System.out.println("═══════════════════════════════════");
        System.out.println("🎲 DAVID procesando ronda...");
        System.out.println("═══════════════════════════════════");
        
        // Simular que la ronda tarda 3 segundos
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        System.out.println("✓ Ronda procesada");
        System.out.println("📊 Determinando ganadores...\n");
        
        // Obtener jugadores y Aitor
        AID[] jugadores = obtenerJugadores();
        AID aitor = obtenerAitor();
        
        if (jugadores == null || jugadores.length == 0) {
            System.out.println("⚠ No hay jugadores, enviando mensaje sin ganadores");
            enviarMensajeSinGanadores(aitor, jugadores);
            return;
        }
        
        // SIMULACIÓN: El primer jugador gana (en la implementación real,
        // aquí analizarías las soluciones recibidas)
        AID ganador = jugadores[0];
        String solucionGanadora = "25+6=31, 7*4=28, 28*31=868, 868-1=867";
        
        enviarMensajeGanador(ganador, solucionGanadora, aitor, jugadores);
        
        System.out.println("✓ Mensaje de ganador enviado\n");
    }

    /*
     * Enviar mensaje de ganador a jugadores y Aitor
     */
    private void enviarMensajeGanador (AID ganador, String solucion, AID aitor, AID[] jugadores){
        ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);

        //añadimos a aitor como destinatario
        mensaje.addReceiver(aitor);

        //añadimos a los jugadores
        if(jugadores!=null){
            for(AID jugador:jugadores)
                mensaje.addReceiver(jugador);
        }

        // Formato: DAVID_GANADOR_JUGADORES_AITOR:NombreGanador:Solución
        String contenido = TipoMensaje.DAVID_GANADOR_JUGADORES_AITOR.toString() + 
                          ":" + ganador.getLocalName() + 
                          ":" + solucion;
        mensaje.setContent(contenido);
        mensaje.setConversationId("cifras-letras");
        send(mensaje);

        // Imprimir resultado
        System.out.println("\n╔═══════════════════════════════════════════════╗");
        System.out.println("║          🏆 RESULTADO DE LA RONDA 🏆          ║");
        System.out.println("╠═══════════════════════════════════════════════╣");
        System.out.println("║  Ganador: " + String.format("%-35s", ganador.getLocalName()) + "║");
        System.out.println("║  Solución: " + String.format("%-34s", 
            solucion.length() > 34 ? solucion.substring(0, 31) + "..." : solucion) + "║");
        System.out.println("╚═══════════════════════════════════════════════╝\n");
    }

    /*
     * Envia mensaje cuando no hay ganadores
     */
    private void enviarMensajeSinGanadores(AID aitor, AID[] jugadores){
        ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);

        mensaje.addReceiver(aitor);

        if(jugadores!=null){
            for(AID jugador:jugadores)
                mensaje.addReceiver(jugador);
        }

        String contenido = TipoMensaje.DAVID_GANADOR_JUGADORES_AITOR.toString() + 
                          ":Ninguno:No hubo soluciones válidas";
        mensaje.setContent(contenido);
        mensaje.setConversationId("cifras-letras");
        send(mensaje);

        System.out.println("⚠ No hay ganadores en esta ronda");
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
                    
                    procesarRondaYEnviarGanadores();
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
