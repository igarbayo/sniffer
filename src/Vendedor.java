import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPAAgentManagement.*;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

public class Vendedor extends Agent {
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

    private void esperar(int segundos) {
        try {
            Thread.sleep(segundos * 1000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void setup() {
        List<DataVendedor> subastas = new ArrayList<>();
        gui = new VendedorGUI(this, subastas);
        //esperar(10);

        //anadirLibro("LibroX", 50, 10);
        //System.out.println("Vendedor listo para subastar " + libro);

    }

    @Override
    protected void takeDown() {
        // Cerrar la GUI cuando el agente se elimina
        if (gui != null) {
            gui.dispose(); // Cerrar la ventana
        }
        System.out.println("Agente Vendedor finalizado.");
    }


    private class SubastaBehaviour extends CyclicBehaviour {
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
            super(a); // Configurar el periodo de 10 segundos
            if (libro!=null) {
                this.libro = libro;
            }
            this.precio = precio;
            this.incremento = incremento;
        }

        @Override
        public void action() {

            // Obtener la lista de compradores activos (solo la primera vez)
            List<AID> compradores = obtenerCompradores();
            System.out.println("IMPORTANTE " + compradores.size() + " compradores.");
            if (compradores.isEmpty()) {
                // Si hay 1 comprador o menos, terminamos la subasta
                System.out.println("[V]\tSubasta terminada: No hay compradores.");
                eliminarLibro(libro);
                doDelete();
                return;
            }

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

            if (!ultimaRonda) {
                // Enviar "call-for-proposal" a todos los compradores
                ACLMessage cfProposal = new ACLMessage(ACLMessage.CFP);
                cfProposal.setContent("Subasta de " + libro + " por " + precio);
                for (AID comprador : compradores) {
                    cfProposal.addReceiver(comprador);
                }
                send(cfProposal);
                System.out.println("[V]\tEnviado 'call-for-proposal' a " + compradores.size() + " compradores.");

                esperar(10);

                // Procesar respuestas
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                ACLMessage reply = receive(mt);

                List<AID> respondedores = new ArrayList<>();

                while (reply != null) {
                    respondedores.add(reply.getSender());
                    if (ganador == null) {
                        ganador = reply.getSender(); // El primero en responder es el ganador
                    }
                    reply = receive(mt); // Recibir el siguiente mensaje
                }

                numRespondedores = respondedores.size();
                gui.actualizarTabla(obtenerSubastasData());

                if (numRespondedores>=2) {
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
                } else {
                    ultimaRonda = true;
                }
            } else {
                if (ganador != null) {
                    // Se informa a los no ganadores
                    ACLMessage informFinal = new ACLMessage(ACLMessage.INFORM);
                    // Establecer el contenido del mensaje
                    informFinal.setContent("FIN | " + ganador.getName() + " ha ganado " + libro + " por " + precio);
                    // Especificar los receptores del mensaje (!= ganador en DFService)
                    for (AID comprador : compradores) {
                        if (!comprador.equals(ganador)) {
                            informFinal.addReceiver(comprador);
                        }
                    }
                    // Enviar el mensaje
                    send(informFinal);

                    // Se hace un request al ganador
                    ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                    request.setContent("Has ganado " + libro + " por " + precio);
                    request.addReceiver(ganador);
                    send(request);

                    // Borramos el agente
                    esperar(10);
                    eliminarLibro(libro);
                } else {
                    eliminarLibro(libro);
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
