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
import es.usal.pa.cifras.controlador.CallableSolucionAutomatica.NivelJugador; // ← NUEVO IMPORT

/**
 * Agente Jugador - Participante del juego Cifras y Letras
 * Con soporte para diferentes niveles de habilidad
 *
 * @author Lidia & Carolina
 */
public class AgenteJugador extends Agent {

    private List<Integer> numerosRecibidos;
    private Integer valorBuscado;
    private boolean modoAutomatico;
    private NivelJugador nivelJugador;

    private enum EstadoJugador {
        ESPERANDO_CUENTA_ATRAS,
        EN_CUENTA_ATRAS,
        ESPERANDO_TURNO_CIFRAS,
        RECIBIENDO_NUMEROS,
        ESPERANDO_VALOR_BUSCADO,
        ESPERANDO_INICIO,
        JUGANDO,
        ESPERANDO_FIN,
        RECIBIENDO_GANADORES
    }


    @Override
    protected void setup() {
        Object[] args = getArguments();

        if (args != null && args.length > 0) {
            modoAutomatico = (Boolean) args[0]; //Extraemos primer argumento y convierte a Boolean

            // Obtener nivel si se pasa como segundo argumento
            if (args.length > 1 && args[1] instanceof NivelJugador) {
                nivelJugador = (NivelJugador) args[1];
            } else {
                nivelJugador = asignarNivelAleatorio();
            }
        } else {
            modoAutomatico = true;
            nivelJugador = asignarNivelAleatorio();
        }

        numerosRecibidos = new ArrayList<>();
        valorBuscado = null;

        registrarseEnDF();
        addBehaviour(new ComportamientoRecibirMensajes());
    }


    /**
     * Asigna un nivel aleatorio al jugador con probabilidades ponderadas
     * 20% Experto, 40% Intermedio, 30% Principiante, 10% Aleatorio
     */

    private NivelJugador asignarNivelAleatorio() {
        double rand = Math.random();

        if (rand < 0.20) {
            return NivelJugador.EXPERTO;
        } else if (rand < 0.60) {
            return NivelJugador.INTERMEDIO;
        } else if (rand < 0.90) {
            return NivelJugador.PRINCIPIANTE;
        } else {
            return NivelJugador.ALEATORIO;
        }
    }


    private void registrarseEnDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("Jugador");
        sd.setName("Servicio-Jugador-" + getLocalName());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            System.err.println("❌ Error al registrarse en el DF:");
            e.printStackTrace();
        }
    }

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

        return new AID("ExpertoDavid", AID.ISLOCALNAME);
    }

    // ========== CLASE INTERNA: BEHAVIOUR ==========

    private class ComportamientoRecibirMensajes extends CyclicBehaviour {

        private EstadoJugador estado = EstadoJugador.ESPERANDO_CUENTA_ATRAS;

        @Override
        public void action() {
            ACLMessage mensaje = receive();

            if (mensaje != null) {
                procesarMensaje(mensaje);
            } else {
                block();
            }
        }

        private void procesarMensaje(ACLMessage mensaje) {
            if (mensaje == null) {
                return;
            }

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

        private void procesarEstadoEsperandoCuentaAtras(ACLMessage mensaje) {
            if (esMensajeTiempo(mensaje)) {
                estado = EstadoJugador.EN_CUENTA_ATRAS;
                procesarEstadoEnCuentaAtras(mensaje);
            }
        }

        private void procesarEstadoEnCuentaAtras(ACLMessage mensaje) {
            if (esMensajeTiempo(mensaje)) {
                Integer tiempo = extraerValorNumerico(mensaje);
                if (tiempo != null && tiempo <= 5) {
                    // Opcional: mostrar cuenta atrás
                }
            } else if (esMensajeTurnoDavid(mensaje)) {
                numerosRecibidos.clear();
                valorBuscado = null;
                estado = EstadoJugador.ESPERANDO_TURNO_CIFRAS;
            }
        }

        private void procesarEstadoEsperandoTurno(ACLMessage mensaje) {
            if (esMensajeNumero(mensaje)) {
                estado = EstadoJugador.RECIBIENDO_NUMEROS;
                procesarEstadoRecibiendoNumeros(mensaje);
            }
        }

        private void procesarEstadoRecibiendoNumeros(ACLMessage mensaje) {
            if (esMensajeNumero(mensaje)) {
                Integer numero = extraerValorNumerico(mensaje);

                if (numero != null) {
                    numerosRecibidos.add(numero);

                    if (numerosRecibidos.size() == 6) {
                        estado = EstadoJugador.ESPERANDO_VALOR_BUSCADO;
                    }
                } else {
                    System.err.println("   ⚠ [" + myAgent.getLocalName() + "] Error al parsear número");
                }
            }
        }

        private void procesarEstadoEsperandoValorBuscado(ACLMessage mensaje) {
            if (esMensajeValorBuscado(mensaje)) {
                valorBuscado = extraerValorNumerico(mensaje);

                if (valorBuscado != null) {
                    estado = EstadoJugador.ESPERANDO_INICIO;
                } else {
                    System.err.println("   ⚠ [" + myAgent.getLocalName() +
                            "] Error al parsear valor buscado");
                }
            }
        }

        private void procesarEstadoEsperandoInicio(ACLMessage mensaje) {
            if (esMensajeEmpezar(mensaje)) {
                estado = EstadoJugador.JUGANDO;
                generarYEnviarSolucion();
            }
        }

        private void procesarEstadoJugando(ACLMessage mensaje) {
            if (esMensajeFinalizar(mensaje)) {
               // System.out.println("   🏁 [" + myAgent.getLocalName() + "] Ronda finalizada");
                estado = EstadoJugador.ESPERANDO_FIN;
            }
        }

        private void procesarEstadoEsperandoFin(ACLMessage mensaje) {
            if (esMensajeGanador(mensaje)) {
                estado = EstadoJugador.RECIBIENDO_GANADORES;
                procesarEstadoRecibiendoGanadores(mensaje);
            }
        }

        private void procesarEstadoRecibiendoGanadores(ACLMessage mensaje) {
            if (esMensajeTiempo(mensaje)) {
                System.out.println("\n   🔄 [" + myAgent.getLocalName() + "] Reiniciando para nueva ronda\n");
                limpiarColaMensajes();
                estado = EstadoJugador.EN_CUENTA_ATRAS;
                procesarEstadoEnCuentaAtras(mensaje);

            } else if (esMensajeGanador(mensaje)) {
                String nombreGanador = extraerNombreGanador(mensaje);
                String solucion = extraerSolucionGanador(mensaje);

                /*if (nombreGanador != null) {
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
                }*/


            }
        }

        private void limpiarColaMensajes() {
            int mensajesBorrados = 0;
            ACLMessage msg;

            while ((msg = myAgent.receive()) != null) {
                mensajesBorrados++;
            }

            if (mensajesBorrados > 0) {
                System.out.println("   🗑️  [" + myAgent.getLocalName() + "] " +
                        mensajesBorrados + " mensaje(s) borrado(s) de la cola");
            }
        }

        // ========== MÉTODO MODIFICADO: generarYEnviarSolucion ==========

        private void generarYEnviarSolucion() {
            //System.out.println("   💡 [" + myAgent.getLocalName() + "] Generando solución...");

            Solucion solucion = null;

            if (modoAutomatico) {
                solucion = generarSolucionAutomatica();
            } else {
                solucion = generarSolucionTeclado();
            }

            if (solucion != null && !solucion.getListaOperacion().isEmpty()) {
                enviarSolucion(solucion);
            } else {
                System.out.println("   ⚠ [" + myAgent.getLocalName() + "] No se envía solución (vacía o timeout)");
            }
        }


        //Genera solución de forma automática usando el nivel del jugador

        private Solucion generarSolucionAutomatica() {

            CallableSolucionAutomatica callable = new CallableSolucionAutomatica(
                    numerosRecibidos,
                    valorBuscado,
                    nivelJugador
            );
            //FutureTask es un objecto que envuelve un Callable y permite ejecutarlo en un hilo separado,
            // esperar su resultado con timeout y cancelarlo si tarda demasiado
            FutureTask<Solucion> task = new FutureTask<>(callable);
            //ExecutorService es un gestor de hilos que se encarga de crear hilos, ejecutar tareas en ellos y gestionar su cliclo de vida
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(task);

            Solucion solucion = null;

            try {
                solucion = task.get(38, TimeUnit.SECONDS);
               // System.out.println("   ✓ [" + myAgent.getLocalName() + "] Solución generada exitosamente");

            } catch (TimeoutException e) {
               // System.out.println("   ⏱️  [" + myAgent.getLocalName() + "] Timeout - usando mejor solución encontrada");
                task.cancel(true);
                solucion = callable.getMejorSolucion();

            } catch (InterruptedException | ExecutionException e) {
                System.err.println("   ❌ [" + myAgent.getLocalName() + "] Error: " + e.getMessage());
                task.cancel(true);
            }

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

        private Solucion generarSolucionTeclado() {
           // System.out.println("   ⏱️  Tienes 40 segundos para introducir tu solución\n");

            CallableSolucionTeclado callable = new CallableSolucionTeclado(
                    numerosRecibidos,
                    valorBuscado
            );

            FutureTask<Solucion> task = new FutureTask<>(callable);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(task);

            Solucion solucion = null;

            try {
                solucion = task.get(38, TimeUnit.SECONDS);
                System.out.println("   ✓ [" + myAgent.getLocalName() + "] Solución registrada");

            } catch (TimeoutException e) {
               // System.out.println("   ⏱️  [" + myAgent.getLocalName() + "] Tiempo agotado");
                System.out.println("   ⚠ No se enviará solución incompleta");
                task.cancel(true);
                solucion = null;

            } catch (InterruptedException | ExecutionException e) {
                System.err.println("   ❌ [" + myAgent.getLocalName() + "] Error: " + e.getMessage());
                task.cancel(true);
            }

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

        // ========== MÉTODOS DE VERIFICACIÓN (SIN CAMBIOS) ==========

        private boolean esMensajeTiempo(ACLMessage msg) {
            if (msg == null || msg.getContent() == null) return false;
            return msg.getContent().startsWith(TipoMensaje.AITOR_TIEMPO_JUGADORES.toString());
        }

        private boolean esMensajeTurnoDavid(ACLMessage msg) {
            if (msg == null || msg.getContent() == null) return false;
            return msg.getContent().equals(TipoMensaje.AITOR_TURNO_DAVID_JUGADORES.toString());
        }

        private boolean esMensajeNumero(ACLMessage msg) {
            if (msg == null || msg.getContent() == null) return false;
            return msg.getContent().startsWith(TipoMensaje.DAVID_NUMERO_JUGADORES.toString());
        }

        private boolean esMensajeValorBuscado(ACLMessage msg) {
            if (msg == null || msg.getContent() == null) return false;
            return msg.getContent().startsWith(TipoMensaje.DAVID_VALOR_BUSCADO_JUGADORES.toString());
        }

        private boolean esMensajeEmpezar(ACLMessage msg) {
            if (msg == null || msg.getContent() == null) return false;
            return msg.getContent().equals(TipoMensaje.DAVID_EMPEZAR_CIFRAS_JUGADORES.toString());
        }

        private boolean esMensajeFinalizar(ACLMessage msg) {
            if (msg == null || msg.getContent() == null) return false;
            return msg.getContent().equals(TipoMensaje.DAVID_FINALIZAR_CIFRAS_JUGADORES.toString());
        }

        private boolean esMensajeGanador(ACLMessage msg) {
            if (msg == null || msg.getContent() == null) return false;
            return msg.getContent().startsWith(TipoMensaje.DAVID_GANADOR_JUGADORES_AITOR.toString());
        }

        private Integer extraerValorNumerico(ACLMessage msg) {
            if (msg == null || msg.getContent() == null) return null;

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

        private String extraerNombreGanador(ACLMessage msg) {
            if (msg == null || msg.getContent() == null) return null;

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

        private String extraerSolucionGanador(ACLMessage msg) {
            if (msg == null || msg.getContent() == null) return null;

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
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println("Jugador " + getLocalName() + " des-registrado del DF");
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        System.out.println("Jugador " + getLocalName() + " finalizando...");
    }
}