package es.usal.pa.agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import es.usal.pa.agent.modelo.TipoMensaje;
import es.usal.pa.cifras.modelo.Operacion;
import es.usal.pa.cifras.modelo.Solucion;

import java.util.List;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Agente Jugador - Participante del juego Cifras y Letras
 * Escucha mensajes de Aitor y David
 *
 * @author Lidia
 */
public class AgenteJugador extends Agent {

    // Lista para almacenar los números recibidos de David
    private List<Integer> numerosRecibidos;

    //Valor buscado de la ronda actual
    private Integer valorBuscado;

    /**
     * Enumeración de estados del jugador
     * Define el flujo completo del juego
     */
    private enum EstadoJugador {
        ESPERANDO_CUENTA_ATRAS,    // Estado inicial: ignora todo hasta cuenta atrás
        EN_CUENTA_ATRAS,            // Recibiendo mensajes de tiempo
        ESPERANDO_TURNO_CIFRAS,     // Esperando mensaje de turno
        RECIBIENDO_NUMEROS,         // Recibiendo los 6 números
        ESPERANDO_VALOR_BUSCADO,    // Esperando el número objetivo
        ESPERANDO_INICIO,           // Esperando mensaje de inicio
        JUGANDO,                    // Generando/enviando solución
        ESPERANDO_FIN,              // Esperando mensaje de fin
        RECIBIENDO_GANADORES        // Recibiendo ganadores
    }

    protected void setup() {
        System.out.println("╔═══════════════════════════════════╗");
        System.out.println("║   Jugador " + getLocalName() + " conectado          ║");
        System.out.println("╚═══════════════════════════════════╝");

        // Inicializar lista de números
        numerosRecibidos = new ArrayList<>();

        //Inicializar valor buscado
        valorBuscado=null;

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

    /*
     * Buscamos al experto David en el DF
     * @return AID de David
     */
    private AID obtenerExpertoDavid() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("ExpertoCifras");
        template.addServices(sd);
        
        try {
            DFAgentDescription[] results = DFService.search(this, template);
            if (results != null && results.length > 0) {
                return results[0].getName();
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        
        // Si no se encuentra, usar nombre directo
        return new AID("ExpertoDavid", AID.ISLOCALNAME);
    }


    /**
     * CLASE INTERNA: Behaviour para recibir mensajes
     *
     * CyclicBehaviour = Se ejecuta indefinidamente (nunca termina)
     * Perfecto para estar siempre escuchando mensajes
     */
    private class ComportamientoRecibirMensajes extends CyclicBehaviour {

        //Variable de estado, empieza esperando cuenta atrás
        private EstadoJugador estado = EstadoJugador.ESPERANDO_CUENTA_ATRAS;

        @Override
        public void action() {
            // Recibir mensaje (no bloqueante)
            ACLMessage mensaje = receive();

            if (mensaje != null) {
                // Hay mensaje, procesarlo
                String contenido = mensaje.getContent();

                //Mostramos el estado actual
                String tipoMensaje = contenido.split(":")[0];
                System.out.println("[DEBUG " + myAgent.getLocalName() + "] Estado: " + estado + 
                                 " | Recibe: " + tipoMensaje);
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

            switch(estado){
                case ESPERANDO_CUENTA_ATRAS:
                    procesarEstadoEsperandoCuentaAtras(contenido);
                    break;
                case EN_CUENTA_ATRAS:
                    procesarEstadoEnCuentaAtras(contenido);
                    break;
                case ESPERANDO_TURNO_CIFRAS:
                    procesarEstadoEsperandoTurno(contenido);
                    break;
                case RECIBIENDO_NUMEROS:
                    procesarEstadoRecibiendoNumeros(contenido);
                    break;
                case ESPERANDO_VALOR_BUSCADO:
                    procesarEstadoEsperandoValorBuscado(contenido);
                    break;
                case ESPERANDO_INICIO:
                    procesarEstadoEsperandoInicio(contenido);
                    break;
                case JUGANDO:
                    procesarEstadoJugando(contenido);
                    break;
                case ESPERANDO_FIN:
                    procesarEstadoEsperandoFin(contenido);
                    break;
                case RECIBIENDO_GANADORES:
                    procesarEstadoRecibiendoGanadores(contenido);
                    break;
            }
        }

        //Metodos para cada estado
        // El contenido viene en formato: "TIPO_MENSAJE:valor"

        //Estado 1: Ignora todo hasta recibir mensaje de cuenta atras
        private void procesarEstadoEsperandoCuentaAtras(String contenido){
            if(contenido.startsWith(TipoMensaje.AITOR_TIEMPO_JUGADORES.toString())){
                System.out.println("   → [" + myAgent.getLocalName() + "] ¡Nueva ronda detectada!");
                estado = EstadoJugador.EN_CUENTA_ATRAS;
            }
            //Ignorar cualquier otro mensaje
        }

        //Estado 2: Recibiendo cuenta atras
        private void procesarEstadoEnCuentaAtras(String contenido){
            if(contenido.startsWith(TipoMensaje.AITOR_TIEMPO_JUGADORES.toString())){
                //Procesamiento silencioso, Aitor ya lo muestra
            } else if (contenido.equals(TipoMensaje.AITOR_TURNO_DAVID_JUGADORES.toString())){
                System.out.println("   • [" + myAgent.getLocalName() + "] Preparado para jugar cifras");
                numerosRecibidos.clear();
                estado = EstadoJugador.ESPERANDO_TURNO_CIFRAS;
            }
        }

        //Estado 3: Esperar confirmacion de turno
        private void procesarEstadoEsperandoTurno(String contenido){
            if(contenido.startsWith((TipoMensaje.DAVID_NUMERO_JUGADORES.toString()))){
                estado = EstadoJugador.RECIBIENDO_NUMEROS;
                System.out.println("   → [" + myAgent.getLocalName() + "] Recibiendo números...");
                procesarEstadoRecibiendoNumeros(contenido);
            }
        }

        //Estado 4: Recibiendo los 6 numeros
        private void procesarEstadoRecibiendoNumeros(String contenido){
            if(contenido.startsWith(TipoMensaje.DAVID_NUMERO_JUGADORES.toString())){
                String[] partes = contenido.split(":",2);
                if(partes.length>=2){
                    try{
                        Integer numero = Integer.parseInt(partes[1]);
                        numerosRecibidos.add(numero);
                        System.out.println("   📥 [" + myAgent.getLocalName() + "] Número " + 
                                         numerosRecibidos.size() + "/6: " + numero);

                        if(numerosRecibidos.size() == 6){
                            System.out.println("   ✓ [" + myAgent.getLocalName() + "] Números completos: " + numerosRecibidos);
                            estado = EstadoJugador.ESPERANDO_VALOR_BUSCADO;
                        } 
                    }catch (NumberFormatException e){
                            System.err.println("   ⚠ Error al parsear número");
                    }
                }
            }
        }
        
        //Estado 5: Esperando el valor buscado
        private void procesarEstadoEsperandoValorBuscado(String contenido){
            if(contenido.startsWith(TipoMensaje.DAVID_VALOR_BUSCADO_JUGADORES.toString())){
                String[] partes = contenido.split(":",2);
                if(partes.length>=2){
                    try{
                        valorBuscado = Integer.parseInt(partes[1]);
                        System.out.println("   🎯 [" + myAgent.getLocalName() + "] Objetivo: " + valorBuscado);
                        estado = EstadoJugador.ESPERANDO_INICIO;
                    } catch (NumberFormatException e){
                        System.err.println("   ⚠ Error al parsear valor buscado");
                    }
                }
            }
        }
        
        //ESTADO 6: Esperando mensaje de inicio de ronda
        private void procesarEstadoEsperandoInicio(String contenido){
            if(contenido.equals(TipoMensaje.DAVID_EMPEZAR_CIFRAS_JUGADORES.toString())){
                System.out.println("\n   🚀 [" + myAgent.getLocalName() + "] ¡EMPIEZA LA RONDA!");
                System.out.println("   📋 Números: " + numerosRecibidos);
                System.out.println("   🎯 Objetivo: " + valorBuscado + "\n");
                estado = EstadoJugador.JUGANDO;
            
                generarYEnviarSolucion();
            }
        }

        //Estado 7: Jugando (generando solucion)
        //Aqui va la logica de calculo
        private void procesarEstadoJugando(String contenido){
            if(contenido.equals(TipoMensaje.DAVID_FINALIZAR_CIFRAS_JUGADORES.toString())){
                System.out.println("   🏁 [" + myAgent.getLocalName() + "] Ronda finalizada (aún no enviamos solución)");
                estado = EstadoJugador.ESPERANDO_FIN;
            }
        }

        //Estado 8: Esperando mensaje fin
        private void procesarEstadoEsperandoFin(String contenido){
            if(contenido.startsWith(TipoMensaje.DAVID_GANADOR_JUGADORES_AITOR.toString())){
                estado = EstadoJugador.RECIBIENDO_GANADORES;
                procesarEstadoRecibiendoGanadores(contenido);
            }
        }

        //Estado 9: Recibiendo ganadores
        private void procesarEstadoRecibiendoGanadores(String contenido){
            if(contenido.startsWith(TipoMensaje.AITOR_TIEMPO_JUGADORES.toString())){
                System.out.println("\n   🔄 [" + myAgent.getLocalName() + "] Reiniciando para nueva ronda\n");
                estado = EstadoJugador.EN_CUENTA_ATRAS;
            } else if (contenido.startsWith(TipoMensaje.DAVID_GANADOR_JUGADORES_AITOR.toString())){
                String[] partes = contenido.split(":",3);
                if(partes.length>=3){
                    String nombreGanador = partes[1];
                    if(nombreGanador.equals(myAgent.getLocalName()))
                        System.out.println("   ✓ [" + myAgent.getLocalName() + "] ¡He ganado esta ronda!");
                    else if (partes.length>=2 && partes[1].equals("Ninguno"))
                        System.out.println("   • [" + myAgent.getLocalName() + "] Sin ganadores esta ronda");
                }
            }
        }

        //Metodo para limpiar la cola
        private void limpiarColaMensajes(){
            int mensajesBorrados = 0;
            ACLMessage msg;

            //leer todos los mensajes sin bloquear hasta que no haya mas
            while((msg = myAgent.receive()) != null)
                mensajesBorrados++;
            if(mensajesBorrados>0)
                System.out.println("   🗑️  [" + myAgent.getLocalName() + "] " + 
                                 mensajesBorrados + " mensaje(s) borrado(s) de la cola");
        }

        //Metodo para generar y enviar solucion
        //Por ahora se hace manual, pero hay que implementar el algoritmo de busqueda real
        private void generarYEnviarSolucion(){
            System.out.println("   💡 [" + myAgent.getLocalName() + "] Generando solución...");

            //Creamos solucion ficticia:
            //ejemplo del enunciado: 25+6=31, 7*4=28, 28*31=868, 868-1=867
            Solucion solucion = new Solucion();

            //25+6=31
            solucion.addOpereacion(new Operacion(25, 6,'+'));
            // Operación 2: 7*4=28
            solucion.addOpereacion(new Operacion(7, 4, '*'));
            
            // Operación 3: 28*31=868
            solucion.addOpereacion(new Operacion(28, 31, '*'));
            
            // Operación 4: 868-1=867
            solucion.addOpereacion(new Operacion(868, 1, '-'));
            
            System.out.println("   ✓ Solución generada: 25+6=31, 7*4=28, 28*31=868, 868-1=867");
            
            // Enviar solución a David
            enviarSolucion(solucion);
        }

        //Enviar solucion a David mediante mensaje ACL
        private void enviarSolucion(Solucion solucion){
            try{
                AID david = obtenerExpertoDavid();

                ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);
                mensaje.addReceiver(david);
                mensaje.setContentObject(solucion);
                mensaje.setConversationId("cifras-letras");

                myAgent.send(mensaje);

                System.out.println("   📤 [" + myAgent.getLocalName() + "] Solución enviada a David\n");
            }catch(IOException e){
                System.err.println("   ❌ Error al enviar solución: " + e.getMessage());
                e.printStackTrace();
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