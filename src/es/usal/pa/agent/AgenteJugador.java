package es.usal.pa.agent;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import es.usal.pa.agent.modelo.TipoMensaje;

import java.util.List;
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
                //Limpiar numeros de la ronda anterior
                numerosRecibidos.clear();
            } else if (contenido.startsWith(TipoMensaje.DAVID_NUMERO_JUGADORES.toString())) {
                // Mensaje con un número de la ronda
                String[] partes = contenido.split(":", 2);

                if (partes.length >= 2) {
                    try {
                        Integer numero = Integer.parseInt(partes[1]);
                        numerosRecibidos.add(numero);

                        System.out.println("   📥 [" + myAgent.getLocalName() + "] Número recibido: " + numero +
                                " (total: " + numerosRecibidos.size() + "/6)");

                        // Si hemos recibido los 6 números, mostrar resumen
                        if (numerosRecibidos.size() == 6) {
                            System.out.println("   ✓ [" + myAgent.getLocalName() + "] Todos los números recibidos: " +
                                    numerosRecibidos);
                        }

                    } catch (NumberFormatException e) {
                        System.err.println("   ⚠ [" + myAgent.getLocalName() + "] Error al parsear número: " + partes[1]);
                    }
                }

            } else if (contenido.startsWith(TipoMensaje.DAVID_VALOR_BUSCADO_JUGADORES.toString())) {
                // Mensaje con el valor buscado
                String[] partes = contenido.split(":", 2);

                if (partes.length >= 2) {
                    try {
                        valorBuscado = Integer.parseInt(partes[1]);

                        System.out.println("   🎯 [" + myAgent.getLocalName() + "] Valor buscado recibido: " + valorBuscado);

                    } catch (NumberFormatException e) {
                        System.err.println("   ⚠ [" + myAgent.getLocalName() + "] Error al parsear valor buscado: " + partes[1]);
                    }
                }

            } else if (contenido.equals(TipoMensaje.DAVID_EMPEZAR_CIFRAS_JUGADORES.toString())) {
                    // Mensaje de inicio de la ronda de cálculo
                    System.out.println("\n   🚀 [" + myAgent.getLocalName() + "] ¡INICIO! Calculando solución...");
                    System.out.println("   📋 Números: " + numerosRecibidos);
                    System.out.println("   🎯 Objetivo: " + valorBuscado);
                    System.out.println("   ⏱️  Tiempo: 40 segundos\n");


            } else if (contenido.equals(TipoMensaje.DAVID_FINALIZAR_CIFRAS_JUGADORES.toString())) {
                // Mensaje de finalización de la ronda
                System.out.println("\n   🏁 [" + myAgent.getLocalName() + "] ¡TIEMPO AGOTADO!");
                System.out.println("   🔒 Ya no se pueden enviar más soluciones");
                System.out.println("   ⏳ Esperando resultados...\n");


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