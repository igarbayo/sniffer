// Ignacio Garbayo Fernández, 2024-2025
// Prácticas 6/7. Computación Distribuida

import jade.content.ContentElement;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.AID;
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
    // Atributos para la ontologia
    private ContentManager manager = (ContentManager) getContentManager();
    private Codec codec = new SLCodec();
    private Ontology ontology = SimpleJADEAbstractOntologyOntology.getInstance();

    // Atributos
    private Map<String, PrecioIncremento> libros = new HashMap<>();
    private Map<String, PrecioGanadorNum> subastasActivas = new HashMap<>();
    private VendedorGUI gui;

    public List<DataVendedor> obtenerSubastasData() {
        // Convertir las subastas activas a DataVendedor
        return subastasActivas.keySet().stream()
                .map(libro -> new DataVendedor(
                        libro, // La clave del mapa representa el campo libro
                        subastasActivas.get(libro).getPrecio(), // Precio actual de la subasta
                        (subastasActivas.get(libro).getGanador() != null)
                                ? subastasActivas.get(libro).getGanador().getLocalName()
                                : " ", // Nombre del ganador si existe, de lo contrario espacio en blanco
                        subastasActivas.get(libro).getPujadores().size() // Número de pujadores
                ))
                .collect(Collectors.toList());
    }


    public void anadirLibro(String libro, int precio, int incremento) {
        if (libro!=null) {
            libros.put(libro, new PrecioIncremento(precio, incremento));
            LibroBehaviour behaviour = new LibroBehaviour(this, libro);
            subastasActivas.put(libro, new PrecioGanadorNum(precio, null, behaviour));
            addBehaviour(behaviour);
        }
    }

    public void eliminarLibro(String libro) {
        if (libro != null) {
            libros.remove(libro);
            removeBehaviour(subastasActivas.get(libro).getBehaviour());
            subastasActivas.remove(libro);
        }
    }

    @Override
    protected void setup() {
        // Registro de la ontología
        manager.registerLanguage(codec);
        manager.registerOntology(ontology);

        List<DataVendedor> subastas = new ArrayList<>();
        gui = new VendedorGUI(this, subastas);

        addBehaviour(new ProposeBehaviour(this));
    }

    @Override
    protected void takeDown() {
        // Cerrar la GUI cuando el agente se elimina
        if (gui != null) {
            gui.dispose(); // Cerrar la ventana
        }
        System.out.println("Agente Vendedor finalizado.");
    }

    /* DEFINICIÓN DE BEHAVIOURS PARA CADA TIPO DE MENSAJE */
    /* El mismo tipo de behaviour engloba todas las subastas activas */
    private class ProposeBehaviour extends CyclicBehaviour {

        public ProposeBehaviour(Agent a) {
            super(a); // Configurar el periodo de 10 segundos
        }

        @Override
        public void action() {

            /* PROCESAMIENTO DE PROPOSE */
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
                            for (String libro : subastasActivas.keySet()) {
                                if (libro.equals(mensajeRecibido.getLibro()) &&
                                        subastasActivas.get(libro).getPrecio() == mensajeRecibido.getPrecio()) {
                                    subastasActivas.get(libro).getPujadores().add(reply.getSender());
                                    if (subastasActivas.get(libro).getGanador() == null) {
                                        subastasActivas.get(libro).setGanador(reply.getSender());
                                        // El primero en responder es el ganador
                                    }
                                } else {
                                    // Print para debug
                                    System.out.println("[" + getLocalName() + "] El libro o el precio varían");
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
        }
    }

    // Para gestionar el envío de CFP, INFORM y REQUEST
    public class LibroBehaviour extends TickerBehaviour {

        private String libro = null;

        public LibroBehaviour(Agent a, String libro) {
            super(a, 10000);
            if (libro != null) {
                this.libro = libro;
            }
        }

        @Override
        protected void onTick() {

            System.out.println("\n[Primera ronda]: " + subastasActivas.get(libro).isPrimeraRonda());
            System.out.println("[Última ronda]: " + subastasActivas.get(libro).isUltimaRonda() + "\n");

            /* ACCEPT y REJECT */
            if (!subastasActivas.get(libro).isUltimaRonda()) {

                System.out.println("\n" + subastasActivas.get(libro).getPujadores().size() + "\n");

                if (subastasActivas.get(libro).getPujadores().size()>=2) {
                    subastasActivas.get(libro).setGanador(subastasActivas.get(libro).getPujadores().get(0));
                    if (subastasActivas.get(libro).getGanador() != null) {
                        try {
                            // Creación del mensaje ontológico
                            DefaultMensaje mensaje = new DefaultMensaje();
                            mensaje.setDestinatario(subastasActivas.get(libro).getGanador().getLocalName());
                            mensaje.setLibro(libro);
                            mensaje.setPrecio(subastasActivas.get(libro).getPrecio());
                            mensaje.setFin(false);

                            // Enviar ACCEPT_PROPOSAL al ganador
                            ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                            accept.addReceiver(subastasActivas.get(libro).getGanador());
                            accept.setLanguage(codec.getName());
                            accept.setOntology(ontology.getName());

                            // Envolver el objeto mensaje en una acción
                            Action action = new Action(getAID(), mensaje);

                            // Serializar el contenido ontológico en el mensaje
                            manager.fillContent(accept, action);
                            send(accept);

                            // Print de debug
                            System.out.println("[V]\tEnviado 'accept-proposal' a ");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        try {
                            // Enviar REJECT_PROPOSAL a los demás
                            for (AID comprador : subastasActivas.get(libro).getPujadores()) {
                                // Creación del mensaje ontológico
                                DefaultMensaje mensaje = new DefaultMensaje();
                                mensaje.setLibro(libro);
                                mensaje.setPrecio(subastasActivas.get(libro).getPrecio());
                                mensaje.setFin(false);
                                mensaje.setDestinatario(comprador.getLocalName());

                                if (!comprador.equals(subastasActivas.get(libro).getGanador())) {
                                    ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                                    reject.addReceiver(comprador);
                                    reject.setLanguage(codec.getName());
                                    reject.setOntology(ontology.getName());
                                    Action action = new Action(getAID(), mensaje);
                                    manager.fillContent(reject, action);
                                    send(reject);
                                    System.out.println("[V]\tEnviado 'reject-proposal' a " + comprador.getName());
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        // Actualizamos la tabla de subastas
                        gui.actualizarTabla(obtenerSubastasData());

                        // Incrementar precio para la próxima ronda
                        subastasActivas.get(libro).setPrecio(subastasActivas.get(libro).getPrecio() +
                                libros.get(libro).getIncremento());

                        // Print de debug
                        System.out.println("[V]\tPrecio incrementado a: ");
                    }
                    // Si solo ha respondido 1 persona
                } else if (subastasActivas.get(libro).getPujadores().size()==1){
                    subastasActivas.get(libro).setGanador(subastasActivas.get(libro).getPujadores().get(0));
                    if (subastasActivas.get(libro).getGanador()!=null) {
                        try {
                            // Creación del mensaje ontológico
                            DefaultMensaje mensaje = new DefaultMensaje();
                            mensaje.setDestinatario(subastasActivas.get(libro).getGanador().getLocalName());
                            mensaje.setLibro(libro);
                            mensaje.setPrecio(subastasActivas.get(libro).getPrecio());
                            mensaje.setFin(false);

                            // Enviar ACCEPT_PROPOSAL al ganador
                            ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                            accept.addReceiver(subastasActivas.get(libro).getGanador());
                            accept.setLanguage(codec.getName());
                            accept.setOntology(ontology.getName());

                            // Envolver el objeto mensaje en una acción
                            Action action = new Action(getAID(), mensaje);

                            // Serializar el contenido ontológico en el mensaje
                            manager.fillContent(accept, action);
                            send(accept);

                            // Print de debug
                            System.out.println("[V]\tEnviado 'accept-proposal' a ");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    // pasamos a la última ronda
                    subastasActivas.get(libro).setUltimaRonda(true);
                } // si numRespondedores==0, se sigue lanzando la peticion sin incrementar el precio
                // si aún no hay ganador
                // Si, por el contrario, ya hay ganador
                else if (subastasActivas.get(libro).getGanador()!=null) {
                    // Reducimos al precio de la ronda anterior
                    subastasActivas.get(libro).setPrecio(subastasActivas.get(libro).getPrecio() -
                            libros.get(libro).getIncremento());
                    // Pasamos a la última ronda
                    subastasActivas.get(libro).setUltimaRonda(true);
                }
            }

            subastasActivas.get(libro).getPujadores().clear();

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
            if (subastasActivas.get(libro).isPrimeraRonda()) {
                try {
                    // Contenido ontológico
                    DefaultMensaje mensaje = new DefaultMensaje();
                    mensaje.setLibro(libro);
                    mensaje.setFin(false);

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
                    System.out.println("[V]\tEnviado 'inform-start-of-auction'.");

                    // Terminamos la primera ronda
                    subastasActivas.get(libro).setPrimeraRonda(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            System.out.println("\n[Primera ronda]: " + subastasActivas.get(libro).isPrimeraRonda());
            System.out.println("[Última ronda]: " + subastasActivas.get(libro).isUltimaRonda() + "\n");

            /* CFP GENERAL */
            if (!subastasActivas.get(libro).isUltimaRonda()) {
                try {
                    // Añadimos a todos los compradores
                    for (AID comprador : compradores) {
                        DefaultMensaje mensaje = new DefaultMensaje();
                        mensaje.setLibro(libro);
                        mensaje.setPrecio(subastasActivas.get(libro).getPrecio());
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
                    System.out.println("[V]\tEnviado 'call-for-proposal' a " + compradores.size() + " compradores.");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                /* INFORMS Y REQUEST PARA LA ÚLTIMA RONDA */
            } else {
                if (subastasActivas.get(libro).getGanador() != null) {
                    try {
                        // Especificar los receptores del mensaje (en DFService)
                        for (AID comprador : compradores) {
                            DefaultMensaje mensaje = new DefaultMensaje();
                            mensaje.setLibro(libro);
                            mensaje.setPrecio(subastasActivas.get(libro).getPrecio());
                            mensaje.setGanador(subastasActivas.get(libro).getGanador().getLocalName());
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
                        mensaje.setPrecio(subastasActivas.get(libro).getPrecio());
                        mensaje.setGanador(subastasActivas.get(libro).getGanador().getLocalName());

                        // Se hace un request al ganador
                        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                        request.addReceiver(subastasActivas.get(libro).getGanador());
                        request.setLanguage(codec.getName());
                        request.setOntology(ontology.getName());
                        Action action = new Action(getAID(), mensaje);
                        manager.fillContent(request, action);
                        send(request);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Eliminamos la subasta
                    libros.remove(libro);
                    subastasActivas.remove(libro);

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
