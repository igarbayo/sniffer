import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.AID;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPAAgentManagement.*;
import simplejadeabstractontology.ontology.SimpleJADEAbstractOntologyOntology;

import java.util.*;
import java.util.stream.Collectors;

public class Vendedor extends Agent {
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
            addBehaviour(behaviour);
        }
    }

    public void eliminarLibro(String libro) {
        if (libro != null) {
            libros.remove(libro);
            SubastaBehaviour behaviour = subastasActivas.get(libro);
            if (behaviour != null) {
                removeBehaviour(behaviour);
                subastasActivas.remove(libro);
            }
        }

    }

    @Override
    protected void setup() {
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
        private int numRespondedores = 0;
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
            return numRespondedores;
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

            List<AID> respondedores = new ArrayList<>();

            /* PROCESAMIENTO DE RESPUESTAS */
            if (!ultimaRonda) {
                // Procesar respuestas
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                ACLMessage reply = receive(mt);

                while (reply != null) {
                    // SOlo se procesan los propose que referidos al libro y precio fijados
                    if (libro.equals(reply.getContent().split(" ")[0]) &&
                            precio == Integer.parseInt(reply.getContent().split(" ")[1])) {
                        respondedores.add(reply.getSender());
                        if (ganador == null) {
                            ganador = reply.getSender(); // El primero en responder es el ganador
                        }
                    }
                    reply = receive(mt); // Recibir el siguiente mensaje
                }

                numRespondedores = respondedores.size();
                gui.actualizarTabla(obtenerSubastasData());

                if (numRespondedores>=2) {
                    ganador = respondedores.get(0);
                    if (ganador != null) {
                        // Enviar ACCEPT_PROPOSAL al ganador
                        ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                        accept.addReceiver(ganador);
                        accept.setContent(libro + " " + precio);
                        send(accept);
                        System.out.println("[V]\tEnviado 'accept-proposal' a " + ganador.getName());

                        // Enviar REJECT_PROPOSAL a los demás
                        for (AID comprador : respondedores) {
                            if (!comprador.equals(ganador)) {
                                ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                                reject.addReceiver(comprador);
                                reject.setContent(libro + " " + precio);
                                send(reject);
                                System.out.println("[V]\tEnviado 'reject-proposal' a " + comprador.getName());
                            }
                        }
                        gui.actualizarTabla(obtenerSubastasData());
                        // Incrementar precio para la próxima ronda
                        precio += incremento;
                        System.out.println("[V]\tPrecio incrementado a: " + precio);
                    }
                } else if (numRespondedores==1){
                    ganador = respondedores.get(0);
                    // Enviar ACCEPT_PROPOSAL al ganador
                    ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    accept.addReceiver(ganador);
                    accept.setContent(libro + " " + precio);
                    send(accept);
                    System.out.println("[V]\tEnviado 'accept-proposal' a " + ganador.getName());
                    // pasamos a la última ronda
                    ultimaRonda = true;
                } // si numRespondedores==0, se sigue lanzando la peticion sin incrementar el precio
                  // si aún no hay ganador
                else if (ganador!=null) {
                    // Reducimos al precio de la ronda anterior
                    precio -= incremento;
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
                // Enviar mensaje inicial informando el inicio de la subasta
                ACLMessage startAuctionMessage = new ACLMessage(ACLMessage.INFORM);
                startAuctionMessage.setContent("La subasta de " + libro + " está a punto de comenzar.");
                for (AID comprador : compradores) {
                    startAuctionMessage.addReceiver(comprador);
                }
                send(startAuctionMessage);
                System.out.println("[V]\tEnviado 'inform-start-of-auction'.");
                primeraRonda = false;
            }

            /* CFP GENERAL */
            if (!ultimaRonda) {
                // Enviar "call-for-proposal" a todos los compradores
                ACLMessage cfProposal = new ACLMessage(ACLMessage.CFP);
                cfProposal.setContent("Subasta de " + libro + " por " + precio);

                // Establecer fecha límite de respuesta
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.SECOND, 10);
                Date replyByDate = calendar.getTime();
                cfProposal.setReplyByDate(replyByDate);

                for (AID comprador : compradores) {
                    cfProposal.addReceiver(comprador);
                }
                send(cfProposal);
                System.out.println("[V]\tEnviado 'call-for-proposal' a " + compradores.size() + " compradores.");

            /* INFORMS Y REQUEST PARA LA ÚLTIMA RONDA */
            } else {
                if (ganador != null) {
                    // Se informa a los no ganadores
                    ACLMessage informFinal = new ACLMessage(ACLMessage.INFORM);
                    // Establecer el contenido del mensaje
                    informFinal.setContent("FIN " + ganador.getLocalName() + " " + libro + " " + precio);
                    // Especificar los receptores del mensaje (en DFService)
                    for (AID respondedor : respondedores) {
                        informFinal.addReceiver(respondedor);
                    }
                    // Enviar el mensaje
                    send(informFinal);

                    // Se hace un request al ganador
                    ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                    request.setContent(ganador.getLocalName() + " " + libro + " " + precio);
                    request.addReceiver(ganador);
                    send(request);

                    // ELiminamos la subasta
                    eliminarLibro(libro);
                    gui.actualizarTabla(obtenerSubastasData());
                    stop();
                } else {
                    eliminarLibro(libro);
                    gui.actualizarTabla(obtenerSubastasData());
                    stop();
                }
            }
        }



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
