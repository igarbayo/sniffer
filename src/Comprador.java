import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;
import java.util.Map;

public class Comprador extends Agent {
    private Map<String, Integer> librosDeseados = new HashMap<>();
    private Map<String, EscucharSubastasBehaviour> subastasActivas  = new HashMap<>();

    private void anadirLibro(String libro, int max) {
        if (libro != null && max!= 0) {
            librosDeseados.put(libro, max);
        }
        EscucharSubastasBehaviour behaviour = new EscucharSubastasBehaviour(this, libro, max);
        subastasActivas.put(libro, behaviour);
        // Comienza el comportamiento de escuchar las ofertas de subasta
        addBehaviour(behaviour);
    }

    private void eliminarLibro(String libro) {
        librosDeseados.remove(libro);
    }

    private void abandonarSubasta(String libro) {
        if (libro != null) {
            eliminarLibro(libro);
            EscucharSubastasBehaviour behaviour = subastasActivas.get(libro);
            if (behaviour != null) {
                if (!behaviour.isEsGanadorActual()) {
                    removeBehaviour(behaviour);
                    subastasActivas.remove(libro);
                }
            }
        }
    }

    @Override
    protected void setup() {
        // Suscribir al DFService
        suscribirDF();

        //System.out.println("Comprador listo para participar en subastas.");

        anadirLibro("LibroX", 70);


    }

    private void suscribirDF() {
        // Crear una descripción del agente para el DFService
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        // Crear un servicio para registrar al comprador
        ServiceDescription sd = new ServiceDescription();
        sd.setType("comprador");  // Tipo de servicio
        sd.setName("subasta-libros");  // Nombre del servicio

        dfd.addServices(sd);

        try {
            // Registrar el comprador en el DFService
            DFService.register(this, dfd);
            //System.out.println("Comprador registrado en el DFService.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private class EscucharSubastasBehaviour extends CyclicBehaviour {
        private String libro;
        private int max;
        private boolean esGanadorActual = false;

        public String getLibro() {
            return libro;
        }

        public void setLibro(String libro) {
            this.libro = libro;
        }

        public boolean isEsGanadorActual() {
            return esGanadorActual;
        }

        public EscucharSubastasBehaviour(Agent a, String libro, int max) {
            super(a);
            if (libro!=null) {
                this.libro = libro;
            }
            this.max = max;
        }

        @Override
        public void action() {
            // Esperar un mensaje de tipo "call-for-proposal"
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage mensajeCFP = myAgent.receive(mt);

            if (mensajeCFP != null) {
                String contenido = mensajeCFP.getContent();
                //System.out.println("Recibido call-for-proposal: " + contenido);

                // Extraer libro y precio del contenido del mensaje
                String[] partes = contenido.split(" ");  // Asumiendo que el contenido es: "Subasta de LibroX por 50"
                String libroRecibido = partes[2];  // El nombre del libro
                int precioRecibido = Integer.parseInt(partes[4]);  // El precio de la subasta

                // Verificar si el libro está en la lista de compras y si el precio está dentro del presupuesto
                if (libro.equals(libroRecibido) && max >= precioRecibido) {
                    // Enviar una respuesta "propose"
                    ACLMessage mensajePropuesta = new ACLMessage(ACLMessage.PROPOSE);
                    mensajePropuesta.addReceiver(mensajeCFP.getSender()); // Enviar al vendedor
                    mensajePropuesta.setContent("Estoy interesado en el libro " + libroRecibido + " por " + precioRecibido);
                    send(mensajePropuesta);
                    System.out.println("[C]\tEnviado 'propose' al vendedor por el libro: " + libroRecibido + " | " + precioRecibido);
                }
            }

            // Esperar un mensaje de tipo "ACCEPT_PROPOSAL"
            MessageTemplate acp = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage mensajeACP = myAgent.receive(acp);
            if (mensajeACP != null) {
                String contenidoACP = mensajeACP.getContent();
                if (libro.equals(contenidoACP)) {
                    esGanadorActual = true;
                }
            }

            // Esperar un mensaje de tipo "REJECT_PROPOSAL"
            MessageTemplate rjp = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);
            ACLMessage mensajeRJP = myAgent.receive(rjp);
            if (mensajeRJP != null) {
                String contenidoRJP = mensajeRJP.getContent();
                if (libro.equals(contenidoRJP)) {
                    esGanadorActual = false;
                }
            }

            // Esperar un mensaje de tipo "REQUEST"
            MessageTemplate request = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage mensajeREQUEST = myAgent.receive(request);
            if (mensajeREQUEST != null) {
                String contenidoREQUEST = mensajeREQUEST.getContent();
                if (libro.equals(contenidoREQUEST.split(" ")[2])) {
                    esGanadorActual = false;
                    abandonarSubasta(libro);
                    return;
                }
            }

            // Esperar un mensaje de tipo "INFORM"
            MessageTemplate inform = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage mensajeINFORM = myAgent.receive(inform);
            if (mensajeINFORM != null) {
                String contenidoINFORM = mensajeINFORM.getContent();
                if (contenidoINFORM.split(" ")[0].equals("FIN")) {
                    abandonarSubasta(libro);
                }
            }
        }
    }
}
