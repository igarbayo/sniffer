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

public class Vendedor extends Agent {
    private Map<String, PrecioIncremento> libros = new HashMap<>();
    private AID ganador = null;
    private boolean primeraRonda = true; // Para enviar el mensaje inicial solo una vez
    private boolean ultimaRonda = false; // Para terminar

    private void anadirLibro(String libro, int precio, int incremento) {
        if (libro!=null) {
            libros.put(libro, new PrecioIncremento(precio, incremento));
        }
    }

    private void eliminarLibro(String libro) {
        libros.remove(libro);
    }

    @Override
    protected void setup() {
        anadirLibro("LibroX", 50, 10);
        //System.out.println("Vendedor listo para subastar " + libro);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for (String libro : libros.keySet()) {
            addBehaviour(new SubastaBehaviour(this, libro,
                    libros.get(libro).getPrecio(),
                    libros.get(libro).getIncremento())); // Comportamiento periódico
        }
    }

    private class SubastaBehaviour extends CyclicBehaviour {
        private String libro;
        private int precio;
        private int incremento;

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
            if (compradores.size() <= 1) {
                // Si hay 1 comprador o menos, terminamos la subasta
                System.out.println("[V]\tSubasta terminada: " +
                        (compradores.isEmpty() ? "No hay compradores." : "Solo queda un comprador."));
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

                try {
                    // Esperar 10 segundos para recibir respuestas
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

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

                System.out.println("[V]\tRecibidas " + respondedores.size() + " propuestas.");

                if (respondedores.size()>=2) {
                    if (ganador != null) {
                        // Enviar ACCEPT_PROPOSAL al ganador
                        ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                        accept.addReceiver(ganador);
                        accept.setContent("¡Felicidades! Ganaste esta ronda.");
                        send(accept);
                        System.out.println("[V]\tEnviado 'accept-proposal' a " + ganador.getName());

                        // Enviar REJECT_PROPOSAL a los demás
                        for (AID comprador : respondedores) {
                            if (!comprador.equals(ganador)) {
                                ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                                reject.addReceiver(comprador);
                                reject.setContent("Propuesta rechazada.");
                                send(reject);
                                System.out.println("[V]\tEnviado 'reject-proposal' a " + comprador.getName());
                            }
                        }
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
                    informFinal.setContent(ganador.getName() + " ha ganado " + libro + " por " + precio);
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
                    doDelete();
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
