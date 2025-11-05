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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import es.usal.pa.cifras.controlador.CallableSolucionTeclado;
import es.usal.pa.cifras.controlador.CallableSolucionAutomatica;

/**
 * Agente Jugador - Participante del juego Cifras y Letras
 * Escucha mensajes de Aitor y David
 *
 * VERSIÓN REFACTORIZADA con métodos auxiliares de verificación
 *
 * @author Lidia
 */
public class AgenteJugador extends Agent {

    // ========== VARIABLES DE INSTANCIA ==========

    // Lista para almacenar los números recibidos de David
    private List<Integer> numerosRecibidos;

    // Valor buscado de la ronda actual
    private Integer valorBuscado;

    //Modo de juego: true=automática, false=manual (teclado)
    private boolean modoAutomatico;

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

    // ========== MÉTODO SETUP ==========

    @Override
    protected void setup() {
        //System.out.println("Jugador " + getLocalName() + "conectado");

        // Obtener argumentos (si se pasaron)
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            modoAutomatico = (Boolean) args[0];
        } else {
            // Por defecto: modo automático
            modoAutomatico = true;
        }


        // Inicializar lista de números
        numerosRecibidos = new ArrayList<>();

        // Inicializar valor buscado
        valorBuscado = null;

        // Registrarse en el Directory Facilitator (DF) como "Jugador"
        registrarseEnDF();

        //System.out.println("Jugador " + getLocalName() + " listo para jugar\n");

        // Añadir behaviour para recibir mensajes
        addBehaviour(new ComportamientoRecibirMensajes());
    }

    // ========== MÉTODO DE REGISTRO EN DF ==========

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
            //System.out.println("✅ " + getLocalName() + " registrado en el DF como 'Jugador'");
        } catch (FIPAException e) {
            System.err.println("❌ Error al registrarse en el DF:");
            e.printStackTrace();
        }
    }

    // ========== MÉTODO PARA OBTENER A DAVID ==========

    /**
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

    // ========== CLASE INTERNA: BEHAVIOUR ==========

    /**
     * CLASE INTERNA: Behaviour para recibir mensajes
     *
     * CyclicBehaviour = Se ejecuta indefinidamente (nunca termina)
     * Perfecto para estar siempre escuchando mensajes
     */
    private class ComportamientoRecibirMensajes extends CyclicBehaviour {

        // Variable de estado, empieza esperando cuenta atrás
        private EstadoJugador estado = EstadoJugador.ESPERANDO_CUENTA_ATRAS;

        @Override
        public void action() {
            // Recibir mensaje (no bloqueante)
            ACLMessage mensaje = receive();

            if (mensaje != null) {
                // Hay mensaje, procesarlo

                // OPCIONAL: Para depuración detallada, descomentar la siguiente línea
                // debugMensaje(mensaje);

                procesarMensaje(mensaje);
            } else {
                // No hay mensaje, bloquear hasta que llegue uno
                block();
            }
        }

        // ========== MÉTODO PRINCIPAL DE PROCESAMIENTO ==========

        /**
         * Procesa el mensaje recibido según su tipo
         * Versión REFACTORIZADA usando métodos auxiliares
         */
        private void procesarMensaje(ACLMessage mensaje) {
            // Verificar que el mensaje es válido
            if (mensaje == null) {
                return;
            }

            // Procesar según el estado actual
            switch(estado) {
                case ESPERANDO_CUENTA_ATRAS:
                    procesarEstadoEsperandoCuentaAtras(mensaje);
                    break;
                case EN_CUENTA_ATRAS:
                    procesarEstadoEnCuentaAtras(mensaje);
                    break;
                case ESPERANDO_TURNO_CIFRAS:
                    procesarEstadoEsperandoTurno(mensaje);
                    break;
                case RECIBIENDO_NUMEROS:
                    procesarEstadoRecibiendoNumeros(mensaje);
                    break;
                case ESPERANDO_VALOR_BUSCADO:
                    procesarEstadoEsperandoValorBuscado(mensaje);
                    break;
                case ESPERANDO_INICIO:
                    procesarEstadoEsperandoInicio(mensaje);
                    break;
                case JUGANDO:
                    procesarEstadoJugando(mensaje);
                    break;
                case ESPERANDO_FIN:
                    procesarEstadoEsperandoFin(mensaje);
                    break;
                case RECIBIENDO_GANADORES:
                    procesarEstadoRecibiendoGanadores(mensaje);
                    break;
            }
        }

        // ========== MÉTODOS DE PROCESAMIENTO POR ESTADO ==========

        /**
         * Estado 1: Ignora todo hasta recibir mensaje de cuenta atrás
         */
        private void procesarEstadoEsperandoCuentaAtras(ACLMessage mensaje) {
            if (esMensajeTiempo(mensaje)) {
               // System.out.println("   → [" + myAgent.getLocalName() + "] ¡Nueva ronda detectada!");
                estado = EstadoJugador.EN_CUENTA_ATRAS;
                procesarEstadoEnCuentaAtras(mensaje); // Procesar este mismo mensaje
            }
            // Ignorar cualquier otro mensaje
        }

        /**
         * Estado 2: Recibiendo cuenta atrás
         */
        private void procesarEstadoEnCuentaAtras(ACLMessage mensaje) {
            if (esMensajeTiempo(mensaje)) {
                // Procesamiento silencioso, Aitor ya lo muestra
                // Opcional: mostrar solo valores específicos
                Integer tiempo = extraerValorNumerico(mensaje);
                if (tiempo != null && tiempo <= 5) {
                    //System.out.println("   ⏱️ [" + myAgent.getLocalName() + "] " + tiempo + "...");
                }

            } else if (esMensajeTurnoDavid(mensaje)) {
               // System.out.println("   • [" + myAgent.getLocalName() + "] Preparado para jugar cifras");
                numerosRecibidos.clear();
                valorBuscado = null;
                estado = EstadoJugador.ESPERANDO_TURNO_CIFRAS;
            }
        }

        /**
         * Estado 3: Esperar confirmación de turno
         */
        private void procesarEstadoEsperandoTurno(ACLMessage mensaje) {
            if (esMensajeNumero(mensaje)) {
                estado = EstadoJugador.RECIBIENDO_NUMEROS;
                //System.out.println("   → [" + myAgent.getLocalName() + "] Recibiendo números...");
                procesarEstadoRecibiendoNumeros(mensaje); // Procesar este número
            }
        }

        /**
         * Estado 4: Recibiendo los 6 números
         */
        private void procesarEstadoRecibiendoNumeros(ACLMessage mensaje) {
            if (esMensajeNumero(mensaje)) {
                Integer numero = extraerValorNumerico(mensaje);

                if (numero != null) {
                    numerosRecibidos.add(numero);
                    //System.out.println("   📥 [" + myAgent.getLocalName() + "] Número " +
                           // numerosRecibidos.size() + "/6: " + numero);

                    if (numerosRecibidos.size() == 6) {
                        //System.out.println("   ✓ [" + myAgent.getLocalName() + "] Números completos: " +
                            //    numerosRecibidos);
                        estado = EstadoJugador.ESPERANDO_VALOR_BUSCADO;
                    }
                } else {
                    System.err.println("   ⚠ [" + myAgent.getLocalName() + "] Error al parsear número");
                }
            }
        }

        /**
         * Estado 5: Esperando el valor buscado
         */
        private void procesarEstadoEsperandoValorBuscado(ACLMessage mensaje) {
            if (esMensajeValorBuscado(mensaje)) {
                valorBuscado = extraerValorNumerico(mensaje);

                if (valorBuscado != null) {
                   // System.out.println("   🎯 [" + myAgent.getLocalName() + "] Objetivo: " + valorBuscado);
                    estado = EstadoJugador.ESPERANDO_INICIO;
                } else {
                    System.err.println("   ⚠ [" + myAgent.getLocalName() +
                            "] Error al parsear valor buscado");
                }
            }
        }

        /**
         * Estado 6: Esperando mensaje de inicio de ronda
         */
        private void procesarEstadoEsperandoInicio(ACLMessage mensaje) {

            if (esMensajeEmpezar(mensaje)) {
                 /*
                System.out.println("\n   🚀 [" + myAgent.getLocalName() + "] ¡EMPIEZA LA RONDA!");
                System.out.println("   📋 Números: " + numerosRecibidos);
                System.out.println("   🎯 Objetivo: " + valorBuscado + "\n");

             */

                estado = EstadoJugador.JUGANDO;
                generarYEnviarSolucion();
            }
        }

        /**
         * Estado 7: Jugando (generando solución)
         */
        private void procesarEstadoJugando(ACLMessage mensaje) {
            if (esMensajeFinalizar(mensaje)) {
                System.out.println("   🏁 [" + myAgent.getLocalName() + "] Ronda finalizada");
                estado = EstadoJugador.ESPERANDO_FIN;
            }
        }

        /**
         * Estado 8: Esperando mensaje fin
         */
        private void procesarEstadoEsperandoFin(ACLMessage mensaje) {
            if (esMensajeGanador(mensaje)) {
                estado = EstadoJugador.RECIBIENDO_GANADORES;
                procesarEstadoRecibiendoGanadores(mensaje); // Procesar este mismo mensaje
            }
        }

        /**
         * Estado 9: Recibiendo ganadores
         */
        private void procesarEstadoRecibiendoGanadores(ACLMessage mensaje) {
            if (esMensajeTiempo(mensaje)) {
                System.out.println("\n   🔄 [" + myAgent.getLocalName() + "] Reiniciando para nueva ronda\n");
                limpiarColaMensajes();
                estado = EstadoJugador.EN_CUENTA_ATRAS;
                procesarEstadoEnCuentaAtras(mensaje); // Procesar este mensaje de tiempo

            } else if (esMensajeGanador(mensaje)) {
                String nombreGanador = extraerNombreGanador(mensaje);
                String solucion = extraerSolucionGanador(mensaje);

                if (nombreGanador != null) {
                    if (nombreGanador.equals(myAgent.getLocalName())) {
                        System.out.println("\n╔═══════════════════════════════════════════╗");
                        System.out.println("║   🏆 ¡HE GANADO ESTA RONDA! 🏆           ║");
                        System.out.println("╚═══════════════════════════════════════════╝");
                        System.out.println("   Solución: " + (solucion != null ? solucion : "N/A"));
                        System.out.println();

                    } else if (nombreGanador.equals("Ninguno")) {
                        System.out.println("   • [" + myAgent.getLocalName() + "] Sin ganadores esta ronda");

                    } else {
                        System.out.println("   🏆 [" + myAgent.getLocalName() + "] Ganador: " + nombreGanador);
                        if (solucion != null) {
                            System.out.println("      Solución: " + solucion);
                        }
                    }
                }
            }
        }

        // ========== MÉTODOS AUXILIARES DE LIMPIEZA Y SOLUCIÓN ==========

        /**
         * Método para limpiar la cola de mensajes
         */
        private void limpiarColaMensajes() {
            int mensajesBorrados = 0;
            ACLMessage msg;

            // Leer todos los mensajes sin bloquear hasta que no haya más
            while ((msg = myAgent.receive()) != null) {
                mensajesBorrados++;
            }

            if (mensajesBorrados > 0) {
                System.out.println("   🗑️  [" + myAgent.getLocalName() + "] " +
                        mensajesBorrados + " mensaje(s) borrado(s) de la cola");
            }
        }

        /**
         * Método para generar y enviar solución
         * Por ahora usa solución ficticia del ejemplo del enunciado
         * TODO: Implementar algoritmo de búsqueda automática
         */
        private void generarYEnviarSolucion() {
            System.out.println("   💡 [" + myAgent.getLocalName() + "] Generando solución...");

            Solucion solucion = null;

            if (modoAutomatico) {
                // ========== MODO AUTOMÁTICO ==========
                solucion = generarSolucionAutomatica();
            } else {
                // ========== MODO MANUAL (TECLADO) ==========
                solucion = generarSolucionTeclado();
            }

            // Enviar solución si existe
            if (solucion != null && !solucion.getListaOperacion().isEmpty()) {
                enviarSolucion(solucion);
            } else {
                System.out.println("   ⚠ [" + myAgent.getLocalName() + "] No se envía solución (vacía o timeout)");
            }
        }

        /**
         * Genera solución de forma automática usando búsqueda exhaustiva
         */
        private Solucion generarSolucionAutomatica() {
            System.out.println("   🤖 [" + myAgent.getLocalName() + "] Modo AUTOMÁTICO activado");

            CallableSolucionAutomatica callable = new CallableSolucionAutomatica(
                    numerosRecibidos,
                    valorBuscado
            );

            FutureTask<Solucion> task = new FutureTask<>(callable);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(task);

            Solucion solucion = null;

            try {
                // Esperar máximo 38 segundos (dejamos margen antes de que David finalice)
                solucion = task.get(38, TimeUnit.SECONDS);
                System.out.println("   ✓ [" + myAgent.getLocalName() + "] Solución generada exitosamente");

            } catch (TimeoutException e) {
                System.out.println("   ⏱️  [" + myAgent.getLocalName() + "] Timeout - usando mejor solución encontrada");
                task.cancel(true);
                solucion = callable.getMejorSolucion();

            } catch (InterruptedException | ExecutionException e) {
                System.err.println("   ❌ [" + myAgent.getLocalName() + "] Error: " + e.getMessage());
                task.cancel(true);
            }

            // Limpiar executor
            executor.shutdown();
            try {
                if (!executor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }

            return solucion;
        }

        /**
         * Genera solución mediante entrada por teclado
         */
        private Solucion generarSolucionTeclado() {
            System.out.println("   📝 [" + myAgent.getLocalName() + "] Modo MANUAL activado");
            System.out.println("   ⏱️  Tienes 40 segundos para introducir tu solución\n");

            CallableSolucionTeclado callable = new CallableSolucionTeclado(
                    numerosRecibidos,
                    valorBuscado
            );

            FutureTask<Solucion> task = new FutureTask<>(callable);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(task);

            Solucion solucion = null;

            try {
                // Esperar máximo 38 segundos
                solucion = task.get(38, TimeUnit.SECONDS);
                System.out.println("   ✓ [" + myAgent.getLocalName() + "] Solución registrada");

            } catch (TimeoutException e) {
                System.out.println("   ⏱️  [" + myAgent.getLocalName() + "] Tiempo agotado");
                System.out.println("   ⚠ No se enviará solución incompleta");
                task.cancel(true);
                solucion = null; // No enviar solución parcial

            } catch (InterruptedException | ExecutionException e) {
                System.err.println("   ❌ [" + myAgent.getLocalName() + "] Error: " + e.getMessage());
                task.cancel(true);
            }

            // Limpiar executor
            executor.shutdown();
            try {
                if (!executor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }

            return solucion;
        }

        /**
         * Enviar solución a David mediante mensaje ACL
         */
        private void enviarSolucion(Solucion solucion) {
            try {
                AID david = obtenerExpertoDavid();

                ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);
                mensaje.addReceiver(david);
                mensaje.setContentObject(solucion);
                mensaje.setConversationId("cifras-letras");

                myAgent.send(mensaje);

                System.out.println("   📤 [" + myAgent.getLocalName() + "] Solución enviada a David\n");
            } catch(IOException e) {
                System.err.println("   ❌ Error al enviar solución: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // ========== MÉTODOS AUXILIARES DE VERIFICACIÓN DE MENSAJES ==========

        /**
         * Verifica si el mensaje es de tipo AITOR_TIEMPO_JUGADORES (cuenta atrás)
         *
         * @param msg Mensaje ACL a verificar
         * @return true si es un mensaje de tiempo, false en caso contrario
         */
        private boolean esMensajeTiempo(ACLMessage msg) {
            if (msg == null || msg.getContent() == null) {
                return false;
            }

            String contenido = msg.getContent();
            return contenido.startsWith(TipoMensaje.AITOR_TIEMPO_JUGADORES.toString());
        }

        /**
         * Verifica si el mensaje es de tipo AITOR_TURNO_DAVID_JUGADORES
         * Indica que comienza la ronda de cifras
         *
         * @param msg Mensaje ACL a verificar
         * @return true si es un mensaje de turno, false en caso contrario
         */
        private boolean esMensajeTurnoDavid(ACLMessage msg) {
            if (msg == null || msg.getContent() == null) {
                return false;
            }

            String contenido = msg.getContent();
            return contenido.equals(TipoMensaje.AITOR_TURNO_DAVID_JUGADORES.toString());
        }

        /**
         * Verifica si el mensaje es de tipo DAVID_NUMERO_JUGADORES
         * David envía los 6 números uno por uno
         *
         * @param msg Mensaje ACL a verificar
         * @return true si es un mensaje con número, false en caso contrario
         */
        private boolean esMensajeNumero(ACLMessage msg) {
            if (msg == null || msg.getContent() == null) {
                return false;
            }

            String contenido = msg.getContent();
            return contenido.startsWith(TipoMensaje.DAVID_NUMERO_JUGADORES.toString());
        }

        /**
         * Verifica si el mensaje es de tipo DAVID_VALOR_BUSCADO_JUGADORES
         * David envía el número objetivo a alcanzar
         *
         * @param msg Mensaje ACL a verificar
         * @return true si es el mensaje del valor buscado, false en caso contrario
         */
        private boolean esMensajeValorBuscado(ACLMessage msg) {
            if (msg == null || msg.getContent() == null) {
                return false;
            }

            String contenido = msg.getContent();
            return contenido.startsWith(TipoMensaje.DAVID_VALOR_BUSCADO_JUGADORES.toString());
        }

        /**
         * Verifica si el mensaje es de tipo DAVID_EMPEZAR_CIFRAS_JUGADORES
         * Indica que el jugador puede empezar a calcular su solución
         *
         * @param msg Mensaje ACL a verificar
         * @return true si es mensaje de empezar, false en caso contrario
         */
        private boolean esMensajeEmpezar(ACLMessage msg) {
            if (msg == null || msg.getContent() == null) {
                return false;
            }

            String contenido = msg.getContent();
            return contenido.equals(TipoMensaje.DAVID_EMPEZAR_CIFRAS_JUGADORES.toString());
        }

        /**
         * Verifica si el mensaje es de tipo DAVID_FINALIZAR_CIFRAS_JUGADORES
         * Indica que el tiempo ha terminado y no se aceptan más soluciones
         *
         * @param msg Mensaje ACL a verificar
         * @return true si es mensaje de finalización, false en caso contrario
         */
        private boolean esMensajeFinalizar(ACLMessage msg) {
            if (msg == null || msg.getContent() == null) {
                return false;
            }

            String contenido = msg.getContent();
            return contenido.equals(TipoMensaje.DAVID_FINALIZAR_CIFRAS_JUGADORES.toString());
        }

        /**
         * Verifica si el mensaje es de tipo DAVID_GANADOR_JUGADORES_AITOR
         * Anuncia al ganador o ganadores de la ronda
         *
         * @param msg Mensaje ACL a verificar
         * @return true si es mensaje de ganador, false en caso contrario
         */
        private boolean esMensajeGanador(ACLMessage msg) {
            if (msg == null || msg.getContent() == null) {
                return false;
            }

            String contenido = msg.getContent();
            return contenido.startsWith(TipoMensaje.DAVID_GANADOR_JUGADORES_AITOR.toString());
        }

        // ========== MÉTODOS AUXILIARES DE EXTRACCIÓN ==========

        /**
         * Extrae el valor numérico de un mensaje de tipo TIEMPO o NUMERO
         * Formato esperado: "TIPO_MENSAJE:valor"
         *
         * @param msg Mensaje del que extraer el valor
         * @return El valor numérico, o null si hay error
         */
        private Integer extraerValorNumerico(ACLMessage msg) {
            if (msg == null || msg.getContent() == null) {
                return null;
            }

            try {
                String contenido = msg.getContent();
                String[] partes = contenido.split(":", 2);

                if (partes.length >= 2) {
                    return Integer.parseInt(partes[1].trim());
                }
            } catch (NumberFormatException e) {
                System.err.println("   ⚠ Error al extraer valor numérico: " + e.getMessage());
            }

            return null;
        }

        /**
         * Extrae el nombre del ganador de un mensaje de tipo GANADOR
         * Formato esperado: "DAVID_GANADOR_JUGADORES_AITOR:NombreGanador:Solución"
         *
         * @param msg Mensaje del que extraer el nombre
         * @return Nombre del ganador, o null si hay error
         */
        private String extraerNombreGanador(ACLMessage msg) {
            if (msg == null || msg.getContent() == null) {
                return null;
            }

            try {
                String contenido = msg.getContent();
                String[] partes = contenido.split(":", 3);

                if (partes.length >= 2) {
                    return partes[1].trim();
                }
            } catch (Exception e) {
                System.err.println("   ⚠ Error al extraer nombre ganador: " + e.getMessage());
            }

            return null;
        }

        /**
         * Extrae la solución del ganador de un mensaje de tipo GANADOR
         * Formato esperado: "DAVID_GANADOR_JUGADORES_AITOR:NombreGanador:Solución"
         *
         * @param msg Mensaje del que extraer la solución
         * @return Solución como String, o null si hay error
         */
        private String extraerSolucionGanador(ACLMessage msg) {
            if (msg == null || msg.getContent() == null) {
                return null;
            }

            try {
                String contenido = msg.getContent();
                String[] partes = contenido.split(":", 3);

                if (partes.length >= 3) {
                    return partes[2].trim();
                }
            } catch (Exception e) {
                System.err.println("   ⚠ Error al extraer solución: " + e.getMessage());
            }

            return null;
        }

        // ========== MÉTODO DE DEPURACIÓN (OPCIONAL) ==========

        /**
         * Método para depuración: imprime información detallada del mensaje
         * USAR SOLO PARA DEBUGGING - Comentar en producción
         *
         * @param msg Mensaje a analizar
         */
        @SuppressWarnings("unused")
        private void debugMensaje(ACLMessage msg) {
            if (msg == null) {
                System.out.println("[DEBUG] Mensaje null");
                return;
            }

            System.out.println("[DEBUG " + myAgent.getLocalName() + "] Análisis de mensaje:");
            System.out.println("   - Performative: " + ACLMessage.getPerformative(msg.getPerformative()));
            System.out.println("   - Sender: " + (msg.getSender() != null ? msg.getSender().getLocalName() : "null"));
            System.out.println("   - Content: " + msg.getContent());
            System.out.println("   - ConversationId: " + msg.getConversationId());
            System.out.println("   - Tipo detectado:");
            System.out.println("      • esMensajeTiempo: " + esMensajeTiempo(msg));
            System.out.println("      • esMensajeTurnoDavid: " + esMensajeTurnoDavid(msg));
            System.out.println("      • esMensajeNumero: " + esMensajeNumero(msg));
            System.out.println("      • esMensajeValorBuscado: " + esMensajeValorBuscado(msg));
            System.out.println("      • esMensajeEmpezar: " + esMensajeEmpezar(msg));
            System.out.println("      • esMensajeFinalizar: " + esMensajeFinalizar(msg));
            System.out.println("      • esMensajeGanador: " + esMensajeGanador(msg));
        }

    } // Fin de ComportamientoRecibirMensajes

    // ========== MÉTODO TAKEDOWN ==========

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