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
import jade.lang.acl.UnreadableException;
import java.util.Arrays;                      // Para Arrays.asList()
import es.usal.pa.cifras.controlador.AuxProblema;  // Para generar números
import es.usal.pa.agent.modelo.VariablesConfiguracion;
import es.usal.pa.cifras.controlador.AuxSolucion;
import es.usal.pa.cifras.modelo.Solucion;

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

    private List<Integer> numerosRonda;
    private Integer valorBuscado;
    private List<SolucionJugador> solucionesRecibidas; //Lista de soluciones recibidas de los jugadores durante la ronda actual
                                                       //Se limpia al inicio de cada nueva ronda

    private boolean modoDepuracion = false;  // true=valores fijos; false=genera aleatorios

    @Override
    protected void setup() {

        //System.out.println("Mi nombre es: " + getAID().getName());
       // System.out.println();

        numerosRonda = new ArrayList<>();
        valorBuscado = null;
        solucionesRecibidas = new ArrayList<>();


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
        sd.setType("ExpertoCifras");  // Mismo tipo que busca Aitor
        sd.setName("David");
        dfd.addServices(sd);

        try {
            // Registrar en el DF
            DFService.register(this, dfd);
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
                return results[0].getName(); //Devuelve AID de Aitor
        }catch (FIPAException e){
            e.printStackTrace();
        }
        return new AID("Aitor", AID.ISLOCALNAME); // Si no encuentra a Aitor: Crea un AID "a mano" asumiendo que
                                                        // Aitor se llama "Aitor" y está en el mismo contenedor local.

    }


    // Obtenemos los jugadores del DF

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
                AID[] jugadores = new AID[results.length]; //Creamos array vacio del mismo tamaño que results
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
        System.out.println("   📤 Os envio los números");

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

            //System.out.println("   ✓ Número " + (i + 1) + "/6 enviado: " + numero);

            // Esperar 100ms antes de enviar el siguiente
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.err.println("⚠ Error en el delay entre envíos: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

    }

    /**
            * Envía el valor buscado a todos los jugadores.
            *
            * @param jugadores Array con los AIDs de todos los jugadores
 */
    private void enviarValorBuscado(AID[] jugadores) {

        // Verificar que hay jugadores
        if (jugadores == null || jugadores.length == 0) {
            System.out.println("⚠ No hay jugadores conectados, no se envía valor buscado\n");
            return;
        }

        // Crear mensaje ACL
        ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);

        // Añadir todos los jugadores como receptores
        for (AID jugador : jugadores) {
            mensaje.addReceiver(jugador);
        }

        // Establecer el contenido: TIPO_MENSAJE:valorBuscado
        mensaje.setContent(TipoMensaje.DAVID_VALOR_BUSCADO_JUGADORES.toString() + ":" + valorBuscado);
        mensaje.setConversationId("cifras-letras");

        // Enviar el mensaje
        send(mensaje);

    }

    /**
     * Envía el mensaje de inicio de la ronda a todos los jugadores.
     * Indica que pueden empezar a calcular soluciones.
     *
     * @param jugadores Array con los AIDs de todos los jugadores
     */
    private void enviarInicio(AID[] jugadores) {
        System.out.println("\n   🚀 Enviando mensaje de INICIO de ronda...");

        // Verificar que hay jugadores
        if (jugadores == null || jugadores.length == 0) {
            System.out.println("⚠ No hay jugadores conectados, no se envía inicio\n");
            return;
        }

        // Crear mensaje ACL
        ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);

        // Añadir todos los jugadores como receptores
        for (AID jugador : jugadores) {
            mensaje.addReceiver(jugador);
        }

        // Establecer el contenido: solo el tipo de mensaje (no necesita datos adicionales)
        mensaje.setContent(TipoMensaje.DAVID_EMPEZAR_CIFRAS_JUGADORES.toString());
        mensaje.setConversationId("cifras-letras");

        // Enviar el mensaje
        send(mensaje);

       // System.out.println("   ✓ Mensaje de INICIO enviado");
        System.out.println("\n   ⏱️  Tenéis 40 segundos para enviar soluciones\n");
    }

    /**
     * Espera el tiempo de la ronda de cifras (40 segundos).
     * Durante este tiempo, los jugadores calculan y envían sus soluciones.
     */
    private void esperarFinRonda() {
       // System.out.println("⏳ Esperando fin de ronda...");


        System.out.println("   Tiempo de ronda: " + (VariablesConfiguracion.tiempoRondaCifras / 1000) + " segundos");

        try {
            // Esperar el tiempo configurado (40000ms = 40 segundos)
            Thread.sleep(VariablesConfiguracion.tiempoRondaCifras);

            System.out.println("\n⏰ ¡Tiempo finalizado!");

        } catch (InterruptedException e) {
            System.err.println("⚠ Error: La espera fue interrumpida");
            e.printStackTrace();
            Thread.currentThread().interrupt(); // Restaurar el estado de interrupción si es interrumpido
        }
    }

    /**
     * Envía el mensaje de finalización de la ronda a todos los jugadores.
     * Indica que el tiempo ha terminado y no se aceptan más    soluciones
     */

    private void enviarFinalizacion(AID[] jugadores) {
        //System.out.println("🏁 Enviando mensaje de FINALIZACIÓN...");

        // Verificar que hay jugadores
        if (jugadores == null || jugadores.length == 0) {
            System.out.println("⚠ No hay jugadores conectados, no se envía finalización\n");
            return;
        }

        // Crear mensaje ACL
        ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);

        // Añadir todos los jugadores como receptores
        for (AID jugador : jugadores) {
            mensaje.addReceiver(jugador);
        }

        // Establecer el contenido: solo el tipo de mensaje
        mensaje.setContent(TipoMensaje.DAVID_FINALIZAR_CIFRAS_JUGADORES.toString());
        mensaje.setConversationId("cifras-letras");

        // Enviar el mensaje
        send(mensaje);

        System.out.println("   ✓ Mensaje de FINALIZACIÓN enviado");
        System.out.println("   🔒 Ya no se aceptan más soluciones\n");
    }

    /**
     * Lee todas las soluciones enviadas por los jugadores durante la ronda.
     * Procesa la cola de mensajes, valida cada solución y las almacena.
     *
     * Solo considera mensajes de tipo JUGADOR_SOLUCION_DAVID.
     * Los demás mensajes se descartan
     */

    private void leerSoluciones() {
        System.out.println("📨 Leyendo soluciones de los jugadores...");

        // Limpiar lista de soluciones anteriores
        solucionesRecibidas.clear();

        ACLMessage mensaje;
        int mensajesLeidos = 0;
        int solucionesValidas = 0;

        // Leer todos los mensajes de la cola hasta que no haya más
        while ((mensaje = receive()) != null) {
            mensajesLeidos++;
                // Extraer nombre del jugador (del sender)
            String nombreJugador = mensaje.getSender().getLocalName();

            //System.out.println("   📥 Procesando solución de: " + nombreJugador);

            try {
                // Deserializar la solución del contenido del mensaje
                Object obj = mensaje.getContentObject();

                if (obj instanceof Solucion) { //Devuelve true si object se puede convertir a una solucion
                    Solucion solucion = (Solucion) obj; //Convierte object genérico en un solucion

                    // Validar la solución con AuxSolucion
                    Integer resultadoObtenido = AuxSolucion.calcularSolucion(
                            solucion,
                            numerosRonda,
                            valorBuscado
                    );

                    if (resultadoObtenido != null) {
                        // Solución válida
                        SolucionJugador solucionJugador = new SolucionJugador(
                                nombreJugador,
                                solucion,
                                resultadoObtenido
                    );

                        solucionesRecibidas.add(solucionJugador);
                        solucionesValidas++;

                        int distancia = Math.abs(valorBuscado - resultadoObtenido);
                        System.out.println("      ✓ Válida - Resultado: " + resultadoObtenido +
                                " (distancia: " + distancia + ")");

                    } else {
                        // Solución inválida
                         System.out.println("      ❌ Inválida - Operaciones incorrectas");
                    }

                } else {
                     System.out.println("      ⚠ Error: El objeto no es una Solución");
                }

            } catch (UnreadableException e) {
                System.err.println("      ❌ Error al deserializar solución: " + e.getMessage());
            }

        }
        System.out.println();
        System.out.println("   📊 Resumen:");
        System.out.println("      - Mensajes leídos: " + mensajesLeidos);
        System.out.println("      - Soluciones válidas: " + solucionesValidas);
        System.out.println("      - Soluciones inválidas: " + (mensajesLeidos - solucionesValidas));
        System.out.println();
    }


    /**
     * Calcula los ganadores de la ronda.
     * Encuentra el/los jugador(es) con el resultado más cercano al valor buscado
     * @return Lista con los ganadores (puede haber empates)
     */

    private List<SolucionJugador> calcularGanadores() {
        System.out.println("🏆 Calculando ganadores...");

        List<SolucionJugador> ganadores = new ArrayList<>();

        // Si no hay soluciones válidas, no hay ganadores
        if (solucionesRecibidas == null || solucionesRecibidas.isEmpty()) {
            System.out.println("   ⚠ No hay soluciones válidas, no hay ganadores\n");
            return ganadores;
        }

        // Encontrar la distancia mínima al valor buscado
        int distanciaMinima = Integer.MAX_VALUE;

        for (SolucionJugador solucion : solucionesRecibidas) {
            int distancia = solucion.calcularDistancia(valorBuscado);

            if (distancia < distanciaMinima) {
                distanciaMinima = distancia;
            }
        }

        System.out.println("   📏 Distancia mínima encontrada: " + distanciaMinima);

        // Encontrar todos los jugadores con esa distancia mínima (pueden haber empates)
        for (SolucionJugador solucion : solucionesRecibidas) {
            //TipoElemento   variable: coleccion
            int distancia = solucion.calcularDistancia(valorBuscado);

            if (distancia == distanciaMinima) {
                ganadores.add(solucion);
                System.out.println("   🏆 Ganador: " + solucion.getNombreJugador() +
                        " → Resultado: " + solucion.getResultadoObtenido() +
                        " (distancia: " + distancia + ")");
            }
        }

        System.out.println();

        if (ganadores.size() > 1) {
            System.out.println("   ⚖️  ¡EMPATE! " + ganadores.size() + " jugadores con la misma distancia");
        }

        System.out.println();
        return ganadores;
    }

    /**
     * Envía los mensajes con los ganadores de la ronda a todos los jugadores y a Aitor.
     *
     * Si no hay ganadores, envía un mensaje indicándolo.
     * Si hay ganadores (puede haber empates), envía un mensaje por cada uno.
     *
     * @param ganadores Lista con los ganadores de la ronda
     */

    private void enviarGanadores(List<SolucionJugador> ganadores) {
        System.out.println("📢 Enviando mensajes de ganadores...");

        // Obtener Aitor y jugadores del DF
        AID aitor = obtenerAitor();
        AID[] jugadores = obtenerJugadores();

        // Verificar si hay ganadores
        if (ganadores == null || ganadores.isEmpty()) {
            // No hay ganadores
            System.out.println("   ℹ️  No hay ganadores en esta ronda");
            enviarMensajeSinGanadores(aitor, jugadores);

        } else {
            // Hay uno o más ganadores
            System.out.println("   🏆 Enviando " + ganadores.size() + " ganador(es):");

            for (SolucionJugador ganador : ganadores) {
                // Convertir la solución a string legible
                String solucionTexto = AuxSolucion.cadenaOperaciones(ganador.getSolucion())
                        .replace("\n", ", ") //Reemplaza todos los saltos de línea (\n) por comas y espacios (, )
                        .trim(); //Elimina los espacios en blanco (y saltos de línea) al principio y al final del String.

                // Si el texto es muy largo, truncar
                if (solucionTexto.length() > 100) {
                    solucionTexto = solucionTexto.substring(0, 97) + "...";
                }

                // Añadir el resultado final
                solucionTexto += " → " + ganador.getResultadoObtenido();

                System.out.println("      • " + ganador.getNombreJugador() +
                        " (resultado: " + ganador.getResultadoObtenido() + ")");

                // Crear AID del ganador
                AID aidGanador = new AID(ganador.getNombreJugador(), AID.ISLOCALNAME);

                // Enviar mensaje
                enviarMensajeGanador(aidGanador, solucionTexto, aitor, jugadores);
            }

            if (ganadores.size() > 1) {
                System.out.println("      ⚖️  Hubo empate entre " + ganadores.size() + " jugadores");
            }
        }

        System.out.println("✓ Mensajes de ganadores enviados\n");
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
        System.out.println("║       🎲 GENERANDO NUEVO PROBLEMA          ║");
        System.out.println("╚════════════════════════════════════════════╝");

        if (modoDepuracion) {
            // ========== MODO DEPURACIÓN: Valores fijos ==========
            // Valores del ejemplo del enunciado
            numerosRonda = new ArrayList<>(Arrays.asList(25, 7, 4, 6, 4, 1));
            valorBuscado = 866;

        } else {
            // ========== MODO NORMAL: Valores aleatorios ==========

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
    }


    private void procesarRondaYEnviarGanadores() {

        //1. Generar nuevo problema
        generarProblema();

        //2. Obtener jugadores y Aitor
        AID [] jugadores = obtenerJugadores();
        AID aitor = obtenerAitor();

        //Verificamos si hay jugadores
        if(jugadores==null || jugadores.length==0){
            System.out.println("⚠ No hay jugadores, enviando mensaje sin ganadores\n");
            enviarMensajeSinGanadores(aitor, jugadores);
            return;
        }

        //3. Enviar nº a los jugadores
        enviarNumeros (jugadores);

        //4. Enviar valor buscado
        enviarValorBuscado(jugadores);

        //5. Enviar msg de inicio
        enviarInicio(jugadores);

        //6. Esperar 40s - tiempo de ronda (AHORA PUESTO 5s PARA DEPURAR)
        esperarFinRonda();

        //7. Enviar msg de finalización
        enviarFinalizacion(jugadores);

        //8. Leer Soluciones de la cola
        leerSoluciones();

        //9. Calcular ganadores
        List<SolucionJugador> ganadores = calcularGanadores();  //

        //10. Enviar ganadores
        System.out.println("✓ Ronda procesada\n");
        enviarGanadores(ganadores);


    }


    // Enviar mensaje de ganador a jugadores y Aitor
     private void enviarMensajeGanador (AID ganador, String solucion, AID aitor, AID[] jugadores){
        ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);

        //añadimos a aitor como destinatario
        mensaje.addReceiver(aitor);

        //añadimos a los jugadores
        if(jugadores!=null){
            for(AID jugador:jugadores)
                mensaje.addReceiver(jugador);
        }

        String contenido = TipoMensaje.DAVID_GANADOR_JUGADORES_AITOR.toString() +
                ":" + ganador.getLocalName() +
                ":" + solucion;
        mensaje.setContent(contenido);
        mensaje.setConversationId("cifras-letras");
        send(mensaje);

        // Imprimir resultado
        System.out.println("\n╔═══════════════════════════════════════════════╗");
        System.out.println("║          🏆 RESULTADO DE LA RONDA 🏆         ║");
        System.out.println("╠═══════════════════════════════════════════════╣");
        System.out.println("║  Ganador: " + String.format("%-35s", ganador.getLocalName()) + "║");
        System.out.println("║  Solución: " + String.format("%-34s",
                solucion.length() > 34 ? solucion.substring(0, 31) + "..." : solucion) + "║");
        System.out.println("╚═══════════════════════════════════════════════╝\n");
    }


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


                // Verificar si es el mensaje de turno de Aitor
                if (contenido != null && contenido.equals(TipoMensaje.AITOR_TURNO_DAVID_JUGADORES.toString())) {
                    turnoRecibido = true;

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