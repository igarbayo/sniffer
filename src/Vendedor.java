import jade.content.ContentElement;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.AID;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPAAgentManagement.*;
import simplejadeabstractontology.ontology.Mensaje;
import simplejadeabstractontology.ontology.SimpleJADEAbstractOntologyOntology;
import simplejadeabstractontology.ontology.impl.DefaultMensaje;

import java.util.*;
import java.util.stream.Collectors;

public class Vendedor extends Agent {
    // Atributo para múltiples comportamientos paralelos
    private ParallelBehaviour parallelBehaviour;
    // Crear un comportamiento vacío que nunca termine
    Behaviour comportamientoVacio = new CyclicBehaviour() {
        @Override
        public void action() {
            // El comportamiento simplemente se bloquea para que no termine nunca
            block(); // Se bloquea indefinidamente
        }
    };

    // Atributos para la ontologia
    private ContentManager manager = (ContentManager) getContentManager();
    private Codec codec = new SLCodec();
    private Ontology ontology = SimpleJADEAbstractOntologyOntology.getInstance();

    // Atributos
    private Map<String, PrecioIncremento> libros = new HashMap<>();
    private Map<String, SubastaBehaviour> subastasActivas = new HashMap<>();
    private VendedorGUI gui;

    public List<DataVendedor> obtenerSubastasData() {
        // Convertir las subastas activas de SubastaBehaviour a SubastaData
        return subastasActivas.values().stream()
                .map(behaviour -> new DataVendedor(
                        behaviour.getLibro(),
                        behaviour.getPrecio(),
                        (behaviour.getGanador() != null) ? behaviour.getGanador().getLocalName() : " ",
                        behaviour.getNumRespondedores()))
                .collect(Collectors.toList());
    }

    public void anadirLibro(String libro, int precio, int incremento) {
        if (libro!=null) {
            libros.put(libro, new PrecioIncremento(precio, incremento));
            SubastaBehaviour behaviour = new SubastaBehaviour(this, libro, precio, incremento);
            subastasActivas.put(libro, behaviour);
            parallelBehaviour.addSubBehaviour(behaviour);
        }
    }

    public void eliminarLibro(String libro) {
        if (libro != null) {
            libros.remove(libro);
            SubastaBehaviour behaviour = subastasActivas.get(libro);
            if (behaviour != null) {
                parallelBehaviour.removeSubBehaviour(behaviour);
                subastasActivas.remove(libro);
            }
        }

    }

    @Override
    protected void setup() {
        // Crear un ParallelBehaviour para ejecutar múltiples comportamientos en paralelo
        parallelBehaviour = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL) {
            @Override
            public int onEnd() {
                System.out.println("[" + getLocalName() + "] Todos los subcomportamientos han terminado.");
                return super.onEnd();
            }
        };
        // Agregar el ParallelBehaviour al agente
        addBehaviour(parallelBehaviour);

        // Agregar el comportamiento vacío al ParallelBehaviour
        parallelBehaviour.addSubBehaviour(comportamientoVacio);

        // Registro de la ontología
        manager.registerLanguage(codec);
        manager.registerOntology(ontology);

        List<DataVendedor> subastas = new ArrayList<>();
        gui = new VendedorGUI(this, subastas);
    }

    @Override
    protected void takeDown() {
        // Cerrar la GUI cuando el agente se elimina
        if (gui != null) {
            gui.dispose(); // Cerrar la ventana
        }
        System.out.println("Agente Vendedor finalizado.");
    }


    private class SubastaBehaviour extends TickerBehaviour {
        // Atributos
        private String libro;
        private int precio;
        private int incremento;
        private AID ganador = null;
        List<AID> listaRespondedores = new ArrayList<>();
        private boolean primeraRonda = true; // Para enviar el mensaje inicial solo una vez
        private boolean ultimaRonda = false; // Para terminar

        public String getLibro() {
            return libro;
        }

        public int getPrecio() {
            return precio;
        }

        public AID getGanador() {
            return ganador;
        }

        public int getNumRespondedores() {
            return listaRespondedores.size();
        }

        public SubastaBehaviour(Agent a, String libro, int precio, int incremento) {
            super(a, 10000); // Configurar el periodo de 10 segundos
            if (libro!=null) {
                this.libro = libro;
            }
            this.precio = precio;
            this.incremento = incremento;
        }

        @Override
        protected void onTick() {

            listaRespondedores.clear();

            /* PROCESAMIENTO DE RESPUESTAS */
            if (!ultimaRonda) {
                // Procesar respuestas
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                ACLMessage reply = receive(mt);

                while (reply != null) {
                    // Comprobaciones de la ontología
                    try {
                        ContentElement contenido = manager.extractContent(reply);
                        if (contenido instanceof Action) {
                            Action action = (Action) contenido;
                            Object objeto = action.getAction();
                            if (objeto instanceof DefaultMensaje) {
                                Mensaje mensajeRecibido = (Mensaje) objeto;
                                // Solo se procesan los propose que referidos al libro y precio fijados
                                if (libro.equals(mensajeRecibido.getLibro()) &&
                                        precio == mensajeRecibido.getPrecio()) {
                                    listaRespondedores.add(reply.getSender());
                                    if (ganador == null) {
                                        ganador = reply.getSender(); // El primero en responder es el ganador
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // Recibir el siguiente mensaje
                    reply = receive(mt);
                }

                // Actualizamos la tabla de subastas
                gui.actualizarTabla(obtenerSubastasData());

                if (listaRespondedores.size()>=2) {
                    ganador = listaRespondedores.get(0);
                    if (ganador != null) {
                        try {
                            // Creación del mensaje ontológico
                            DefaultMensaje mensaje = new DefaultMensaje();
                            mensaje.setDestinatario(ganador.getLocalName());
                            mensaje.setLibro(libro);
                            mensaje.setPrecio(precio);
                            mensaje.setFin(false);

                            // Enviar ACCEPT_PROPOSAL al ganador
                            ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                            accept.addReceiver(ganador);
                            accept.setLanguage(codec.getName());
                            accept.setOntology(ontology.getName());

                            // Envolver el objeto mensaje en una acción
                            Action action = new Action(getAID(), mensaje);

                            // Serializar el contenido ontológico en el mensaje
                            manager.fillContent(accept, action);
                            send(accept);

                            // Print de debug
                            //System.out.println("[V]\tEnviado 'accept-proposal' a " + ganador.getName());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        try {
                            // Enviar REJECT_PROPOSAL a los demás
                            for (AID comprador : listaRespondedores) {
                                // Creación del mensaje ontológico
                                DefaultMensaje mensaje = new DefaultMensaje();
                                mensaje.setLibro(libro);
                                mensaje.setPrecio(precio);
                                mensaje.setFin(false);
                                mensaje.setDestinatario(comprador.getLocalName());

                                if (!comprador.equals(ganador)) {
                                    ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                                    reject.addReceiver(comprador);
                                    reject.setLanguage(codec.getName());
                                    reject.setOntology(ontology.getName());
                                    Action action = new Action(getAID(), mensaje);
                                    manager.fillContent(reject, action);
                                    send(reject);
                                    //System.out.println("[V]\tEnviado 'reject-proposal' a " + comprador.getName());
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // Actualizamos la tabla de subastas
                        gui.actualizarTabla(obtenerSubastasData());

                        // Incrementar precio para la próxima ronda
                        precio += incremento;

                        // Print de debug
                        //System.out.println("[V]\tPrecio incrementado a: " + precio);
                    }
                // Si solo ha respondido 1 persona
                } else if (listaRespondedores.size()==1){
                    ganador = listaRespondedores.get(0);
                    if (ganador!=null) {
                        try {
                            // Creación del mensaje ontológico
                            DefaultMensaje mensaje = new DefaultMensaje();
                            mensaje.setDestinatario(ganador.getLocalName());
                            mensaje.setLibro(libro);
                            mensaje.setPrecio(precio);
                            mensaje.setFin(false);

                            // Enviar ACCEPT_PROPOSAL al ganador
                            ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                            accept.addReceiver(ganador);
                            accept.setLanguage(codec.getName());
                            accept.setOntology(ontology.getName());

                            // Envolver el objeto mensaje en una acción
                            Action action = new Action(getAID(), mensaje);

                            // Serializar el contenido ontológico en el mensaje
                            manager.fillContent(accept, action);
                            send(accept);

                            // Print de debug
                            //System.out.println("[V]\tEnviado 'accept-proposal' a " + ganador.getName());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    // pasamos a la última ronda
                    ultimaRonda = true;
                } // si numRespondedores==0, se sigue lanzando la peticion sin incrementar el precio
                  // si aún no hay ganador
                // Si, por el contrario, ya hay ganador
                else if (ganador!=null) {
                    // Reducimos al precio de la ronda anterior
                    precio -= incremento;
                    // Pasamos a la última ronda
                    ultimaRonda = true;
                }
            }

            /* OBTENCIÓN DE COMPRADORES ACTIVOS */
            List<AID> compradores = obtenerCompradores();
            System.out.println("IMPORTANTE " + compradores.size() + " compradores.");
            if (compradores.isEmpty()) {
                // Si hay 1 comprador o menos, terminamos la subasta
                System.out.println("[V]\tSubasta terminada: No hay compradores.");
                eliminarLibro(libro);
                doDelete();
                return;
            }

            /* INFORM PARA LA PRIMERA RONDA */
            if (primeraRonda) {
                try {
                    // Contenido ontológico
                    DefaultMensaje mensaje = new DefaultMensaje();
                    mensaje.setLibro(libro);

                    // Enviar mensaje inicial informando el inicio de la subasta
                    ACLMessage startAuctionMessage = new ACLMessage(ACLMessage.INFORM);
                    for (AID comprador : compradores) {
                        startAuctionMessage.addReceiver(comprador);
                    }
                    startAuctionMessage.setLanguage(codec.getName());
                    startAuctionMessage.setOntology(ontology.getName());
                    Action action = new Action(getAID(), mensaje);
                    manager.fillContent(startAuctionMessage, action);
                    send(startAuctionMessage);

                    // Print para debug
                    //System.out.println("[V]\tEnviado 'inform-start-of-auction'.");

                    // Terminamos la primera ronda
                    primeraRonda = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            /* CFP GENERAL */
            if (!ultimaRonda) {
                try {
                    // Añadimos a todos los compradores
                    for (AID comprador : compradores) {
                        DefaultMensaje mensaje = new DefaultMensaje();
                        mensaje.setLibro(libro);
                        mensaje.setPrecio(precio);
                        mensaje.setDestinatario(comprador.getLocalName());

                        // Enviar "call-for-proposal" a todos los compradores
                        ACLMessage cfProposal = new ACLMessage(ACLMessage.CFP);
                        cfProposal.setOntology(ontology.getName());
                        cfProposal.setLanguage(codec.getName());

                        Action action = new Action(getAID(), mensaje);
                        manager.fillContent(cfProposal, action);

                        // Establecer fecha límite de respuesta
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.SECOND, 10);
                        Date replyByDate = calendar.getTime();
                        cfProposal.setReplyByDate(replyByDate);

                        cfProposal.addReceiver(comprador);

                        // Print para debug
                        System.out.println("[" + getLocalName() + "] CFP de " + mensaje.getLibro());

                        send(cfProposal);
                    }

                    // Print para debug
                    //System.out.println("[V]\tEnviado 'call-for-proposal' a " + compradores.size() + " compradores.");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            /* INFORMS Y REQUEST PARA LA ÚLTIMA RONDA */
            } else {
                if (ganador != null) {
                    try {
                        // Especificar los receptores del mensaje (en DFService)
                        for (AID comprador : compradores) {
                            DefaultMensaje mensaje = new DefaultMensaje();
                            mensaje.setLibro(libro);
                            mensaje.setPrecio(precio);
                            mensaje.setGanador(ganador.getLocalName());
                            mensaje.setFin(true);
                            mensaje.setDestinatario(comprador.getLocalName());

                            // Se informa a los no ganadores
                            ACLMessage informFinal = new ACLMessage(ACLMessage.INFORM);

                            informFinal.addReceiver(comprador);
                            informFinal.setLanguage(codec.getName());
                            informFinal.setOntology(ontology.getName());
                            Action action = new Action(getAID(), mensaje);
                            manager.fillContent(informFinal, action);
                            // Enviar el mensaje
                            send(informFinal);

                            // Print para debug
                            System.out.println("[" + getLocalName() + "] Enviado INFORM final");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        DefaultMensaje mensaje = new DefaultMensaje();
                        mensaje.setLibro(libro);
                        mensaje.setPrecio(precio);
                        mensaje.setGanador(ganador.getLocalName());

                        // Se hace un request al ganador
                        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                        request.addReceiver(ganador);
                        request.setLanguage(codec.getName());
                        request.setOntology(ontology.getName());
                        Action action = new Action(getAID(), mensaje);
                        manager.fillContent(request, action);
                        send(request);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Eliminamos la subasta
                    eliminarLibro(libro);

                    // Actualizamos la tabla de subastas
                    gui.actualizarTabla(obtenerSubastasData());
                    stop();
                } else {
                    // Eliminamos la subasta
                    eliminarLibro(libro);

                    // Actualizamos la tabla de subastas
                    gui.actualizarTabla(obtenerSubastasData());
                }
            }
            parallelBehaviour.addSubBehaviour(comportamientoVacio);
        }

        /**
         * Obtener la lista de compradores suscritos al DFService
         * @return lista de compradores
         */
        private List<AID> obtenerCompradores() {
            // Buscar compradores en el DFService
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("comprador");
            template.addServices(sd);

            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                List<AID> compradores = new ArrayList<>();
                for (DFAgentDescription dfd : result) {
                    compradores.add(dfd.getName());
                }
                return compradores;
            } catch (FIPAException fe) {
                fe.printStackTrace();
                return new ArrayList<>();
            }
        }
    }
}
