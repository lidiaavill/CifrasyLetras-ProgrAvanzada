package es.usal.pa.agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Arrays;                      // Para Arrays.asList()
import es.usal.pa.cifras.controlador.AuxProblema;  // Para generar números

import java.util.ArrayList;
import java.util.List;

import es.usal.pa.cifras.modelo.SolucionJugador;
import es.usal.pa.agent.modelo.TipoMensaje;

/**
 * Agente Experto David - Experto en Cifras
 *
 * @author Lidia
 */
public class AgenteExpertoDavid extends Agent {

    // ========== VARIABLES DE INSTANCIA ==========

    /**
     * Lista de 6 números que se usarán en la ronda actual
     * Ejemplo: [25, 7, 4, 6, 4, 1]
     */
    private List<Integer> numerosRonda;

    /**
     * Número objetivo que los jugadores deben alcanzar
     * Ejemplo: 866
     */
    private Integer valorBuscado;

    /**
     * Lista de soluciones recibidas de los jugadores durante la ronda actual
     * Se limpia al inicio de cada nueva ronda
     */
    private List<SolucionJugador> solucionesRecibidas;

    /**
     * Modo de depuración: si true, usa valores fijos; si false, genera aleatorios
     */
    private boolean modoDepuracion = false;  // ⭐ Cambiar a false para aleatorios

    @Override
    protected void setup() {
        System.out.println("╔═══════════════════════════════════╗");
        System.out.println("║   Agente DAVID iniciado           ║");
        System.out.println("║   Experto en Cifras               ║");
        System.out.println("╚═══════════════════════════════════╝");

        System.out.println("Mi nombre es: " + getAID().getName());
        System.out.println();

        // Inicializar variables de instancia
        numerosRonda = new ArrayList<>();
        valorBuscado = null;
        solucionesRecibidas = new ArrayList<>();
        System.out.println("✓ Variables de instancia inicializadas:");
        System.out.println("  - numerosRonda: " + numerosRonda);
        System.out.println("  - valorBuscado: " + valorBuscado);
        System.out.println("  - solucionesRecibidas: " + solucionesRecibidas);
        System.out.println();

        // IMPORTANTE: Registrarse en el DF para que Aitor pueda encontrarnos
        registrarEnDF();

        // Añadir behaviour para recibir mensajes
        addBehaviour(new ComportamientoEsperarTurno());
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


    // ========== MÉTODOS DE ENVÍO A JUGADORES ==========

    /**
     * Envía los números de la ronda a todos los jugadores.
     * Los envía de uno en uno con un delay de 100ms entre cada envío.
     *
     * @param jugadores Array con los AIDs de todos los jugadores
     */
    private void enviarNumeros(AID[] jugadores) {
        System.out.println("📤 Enviando números a los jugadores...");

        // Verificar que hay jugadores
        if (jugadores == null || jugadores.length == 0) {
            System.out.println("⚠ No hay jugadores conectados, no se envían números\n");
            return;
        }

        // Iterar sobre cada número de la ronda
        for (int i = 0; i < numerosRonda.size(); i++) {
            Integer numero = numerosRonda.get(i);

            // Crear mensaje ACL
            ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);

            // Añadir todos los jugadores como receptores
            for (AID jugador : jugadores) {
                mensaje.addReceiver(jugador);
            }

            // Establecer el contenido: TIPO_MENSAJE:numero
            mensaje.setContent(TipoMensaje.DAVID_NUMERO_JUGADORES.toString() + ":" + numero);
            mensaje.setConversationId("cifras-letras");

            // Enviar el mensaje
            send(mensaje);

            System.out.println("   ✓ Número " + (i + 1) + "/6 enviado: " + numero);

            // Esperar 100ms antes de enviar el siguiente
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.err.println("⚠ Error en el delay entre envíos: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("✓ Todos los números enviados\n");
    }

    // ========== MÉTODOS DE GENERACIÓN DE PROBLEMA ==========

    /**
     * Genera un nuevo problema de cifras:
     * - Crea lista de 6 números (aleatorios o fijos según modo)
     * - Genera valor buscado (aleatorio o fijo según modo)
     * - Limpia soluciones anteriores
     *
     * Modos:
     * - modoDepuracion = true  → Valores fijos (25, 7, 4, 6, 4, 1) y 866
     * - modoDepuracion = false → Valores aleatorios
     */
    private void generarProblema() {
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║       🎲 GENERANDO NUEVO PROBLEMA         ║");
        System.out.println("╚════════════════════════════════════════════╝");

        if (modoDepuracion) {
            // ========== MODO DEPURACIÓN: Valores fijos ==========
            System.out.println("   Modo: DEPURACIÓN (valores fijos)");

            // Valores del ejemplo del enunciado
            numerosRonda = new ArrayList<>(Arrays.asList(25, 7, 4, 6, 4, 1));
            valorBuscado = 866;

        } else {
            // ========== MODO NORMAL: Valores aleatorios ==========
            System.out.println("   Modo: NORMAL (valores aleatorios)");

            // Generar 6 números aleatorios
            numerosRonda = AuxProblema.calcularListaNumeros(6);

            // Generar valor buscado entre 100 y 999
            valorBuscado = AuxProblema.calcularResultado(100, 999);
        }

        // Limpiar soluciones de ronda anterior
        solucionesRecibidas.clear();

        // Imprimir problema generado
        System.out.println();
        System.out.println("   📋 Números disponibles:");
        System.out.println("      " + numerosRonda);
        System.out.println();
        System.out.println("   🎯 Valor buscado: " + valorBuscado);
        System.out.println();
        System.out.println("   ✓ Lista de soluciones limpiada");
        System.out.println("╚════════════════════════════════════════════╝\n");
    }

    /*
     * Simula una ronda con envío de ganadores para ver si funciona ComportamientoEsperaGanadores
     */
    private void procesarRondaYEnviarGanadores() {
        System.out.println("═══════════════════════════════════");
        System.out.println("🎲 DAVID procesando ronda...");
        System.out.println("═══════════════════════════════════");

        generarProblema();

        AID [] jugadores = obtenerJugadores();
        AID aitor = obtenerAitor();
        enviarNumeros (jugadores);
        // Simular que la ronda tarda 3 segundos
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("✓ Ronda procesada");
        System.out.println("📊 Determinando ganadores...\n");


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
     * CLASE INTERNA: Behaviour para esperar el turno de Aitor
     *
     * Este behaviour se queda esperando a recibir el mensaje AITOR_TURNO_DAVID_JUGADORES
     * Cuando lo recibe, marca turnoRecibido=true y termina
     */
    private class ComportamientoEsperarTurno extends Behaviour {

        // Variable para saber si hemos recibido el turno
        private boolean turnoRecibido = false;

        @Override
        public void action() {
            // Crear filtro para recibir SOLO mensajes de tipo INFORM con conversationId "cifras-letras"
            MessageTemplate filtro = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("cifras-letras")
            );

            // Intentar recibir mensaje con el filtro (no bloqueante)
            ACLMessage mensaje = myAgent.receive(filtro);

            if (mensaje != null) {
                // Hay mensaje, procesarlo
                String contenido = mensaje.getContent();

                System.out.println("📨 David recibió mensaje: " + contenido);

                // Verificar si es el mensaje de turno de Aitor
                if (contenido != null && contenido.equals(TipoMensaje.AITOR_TURNO_DAVID_JUGADORES.toString())) {
                    System.out.println("✓ ¡Es mi turno! Comenzando ronda de cifras\n");
                    turnoRecibido = true;

                    // TODO: Aquí se procesará la ronda completa (siguiente paso)
                    // Por ahora solo simulamos
                    procesarRondaYEnviarGanadores();
                } else {
                    // Mensaje que no es de turno, lo ignoramos
                    System.out.println("⚠ Mensaje ignorado (no es de turno)");
                }

            } else {
                // No hay mensajes, bloquear hasta que llegue uno
                block();
            }
        }

        @Override
        public boolean done() {
            // El behaviour termina cuando recibe el turno
            return turnoRecibido;
        }

        @Override
        public int onEnd() {
            System.out.println("🔄 Behaviour EsperarTurno finalizado");
            System.out.println("   Añadiendo nuevo behaviour para esperar siguiente turno...\n");

            // IMPORTANTE: Añadir de nuevo este behaviour para la siguiente ronda
            myAgent.addBehaviour(new ComportamientoEsperarTurno());

            return 0;
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