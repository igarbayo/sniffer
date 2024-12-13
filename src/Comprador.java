import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import simplejadeabstractontology.ontology.SimpleJADEAbstractOntologyOntology;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Comprador extends Agent {
    // Atributos para la ontologia
    private ContentManager manager = (ContentManager) getContentManager();
    private Codec codec = new SLCodec();
    private Ontology ontology = SimpleJADEAbstractOntologyOntology.getInstance();

    // Atributos
    private Map<String, Integer> librosDeseados = new HashMap<>();
    private Map<String, Integer> librosComprados = new HashMap<>();
    private Map<String, EscucharSubastasBehaviour> subastasActivas  = new HashMap<>();
    private CompradorGUI gui;

    public Map<String, Integer> getLibrosDeseados() {
        return librosDeseados;
    }

    public Map<String, Integer> getLibrosComprados() {
        return librosComprados;
    }

    public List<DataComprador> obtenerSubastasData() {
        // Convertir las subastas activas de SubastaBehaviour a SubastaData
        return subastasActivas.values().stream()
                .map(behaviour -> new DataComprador(
                        behaviour.getLibro(),
                        behaviour.getPrecioActual(),
                        behaviour.isEsGanadorActual()))
                .collect(Collectors.toList());
    }

    public void anadirLibro(String libro, int max) {
        if (libro != null && max!= 0) {
            librosDeseados.put(libro, max);
        }
        EscucharSubastasBehaviour behaviour = new EscucharSubastasBehaviour(this, libro, max);
        subastasActivas.put(libro, behaviour);
        // Comienza el comportamiento de escuchar las ofertas de subasta
        addBehaviour(behaviour);
    }

    public void comprarLibro(String libro, int precio) {
        if (libro != null && precio != 0) {
            librosComprados.put(libro, precio);
        }
    }

    public boolean eliminarLibro(String libro) {
        if (librosDeseados.containsKey(libro)) {
            librosDeseados.remove(libro);
            removeBehaviour(subastasActivas.get(libro));
            subastasActivas.remove(libro);
            return true;
        }
        return false;
    }

    public boolean abandonarSubasta(String libro) {
        if (libro != null) {
            eliminarLibro(libro);
            EscucharSubastasBehaviour behaviour = subastasActivas.get(libro);
            if (behaviour != null) {
                if (!behaviour.isEsGanadorActual()) {
                    removeBehaviour(behaviour);
                    subastasActivas.remove(libro);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void setup() {
        // Registro de la ontología
        manager.registerLanguage(codec);
        manager.registerOntology(ontology);

        gui = new CompradorGUI(this);
        // Suscribir al DFService
        suscribirDF();
    }

    @Override
    protected void takeDown() {
        // Cerrar la GUI cuando el agente se elimina
        desuscribirDF();
        if (gui != null) {
            gui.dispose(); // Cerrar la ventana
        }
        System.out.println("Agente Comprador finalizado.");
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

    private void desuscribirDF() {
        // Crear una descripción del agente para el DFService
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID()); // Nombre del agente (se obtiene usando getAID())

        try {
            // Eliminar el comprador del DFService
            DFService.deregister(this, dfd);
            System.out.println("Agente desuscrito del DFService.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }


    private class EscucharSubastasBehaviour extends CyclicBehaviour {
        private String libro;
        private int max;
        private int precioActual = 0;
        private boolean esGanadorActual = false;

        public String getLibro() {
            return libro;
        }

        public void setLibro(String libro) {
            this.libro = libro;
        }

        public int getPrecioActual() {
            return precioActual;
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
                if (libro.equals(libroRecibido) && max >= precioRecibido &&
                        System.currentTimeMillis() < mensajeCFP.getReplyByDate().getTime()) {
                    // Enviar una respuesta "propose"
                    ACLMessage mensajePropuesta = new ACLMessage(ACLMessage.PROPOSE);
                    mensajePropuesta.addReceiver(mensajeCFP.getSender()); // Enviar al vendedor
                    mensajePropuesta.setContent(libroRecibido + " " + precioRecibido);
                    send(mensajePropuesta);
                    System.out.println("[C]\tEnviado 'propose' al vendedor por el libro: " + libroRecibido + " | " + precioRecibido);
                } else {
                    if (System.currentTimeMillis() < mensajeCFP.getReplyByDate().getTime()) {
                        ACLMessage mensajeNU = new ACLMessage(ACLMessage.NOT_UNDERSTOOD);
                        mensajeNU.addReceiver(mensajeCFP.getSender()); // Enviar al vendedor
                        mensajeNU.setContent(libroRecibido);
                        send(mensajeNU);
                    }
                    System.out.println("NU");
                }
            }

            // Esperar un mensaje de tipo "ACCEPT_PROPOSAL"
            MessageTemplate acp = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage mensajeACP = myAgent.receive(acp);
            if (mensajeACP != null) {
                String contenidoACP = mensajeACP.getContent();
                if (libro.equals(contenidoACP.split(" ")[0])) {
                    esGanadorActual = true;
                    precioActual = Integer.parseInt(contenidoACP.split(" ")[1]);
                    gui.actualizarTabla(obtenerSubastasData());
                }
            }

            // Esperar un mensaje de tipo "REJECT_PROPOSAL"
            MessageTemplate rjp = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);
            ACLMessage mensajeRJP = myAgent.receive(rjp);
            if (mensajeRJP != null) {
                String contenidoRJP = mensajeRJP.getContent();
                if (libro.equals(contenidoRJP.split(" ")[0])) {
                    esGanadorActual = false;
                    precioActual = Integer.parseInt(contenidoRJP.split(" ")[1]);
                    gui.actualizarTabla(obtenerSubastasData());
                }
            }

            // Esperar un mensaje de tipo "INFORM"
            MessageTemplate inform = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage mensajeINFORM = myAgent.receive(inform);
            if (mensajeINFORM != null) {
                System.out.println("Recibido INFORM");
                System.out.println(mensajeINFORM.getContent());
                String contenidoINFORM = mensajeINFORM.getContent();
                if (contenidoINFORM.split(" ")[0].equals("FIN")) {
                    if (!contenidoINFORM.split(" ")[1].equals(getLocalName())) {
                        abandonarSubasta(libro);
                    }
                    precioActual = Integer.parseInt(contenidoINFORM.split(" ")[3]);
                    gui.actualizarTabla(obtenerSubastasData());
                    System.out.println(contenidoINFORM.split(" ")[1]);
                    System.out.println(getLocalName());
                }
            }

            // Esperar un mensaje de tipo "REQUEST"
            MessageTemplate request = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            if (myAgent != null) {
                ACLMessage mensajeREQUEST = myAgent.receive(request);
                if (mensajeREQUEST != null) {
                    System.out.println("Recibido REQUEST");
                    String contenidoREQUEST = mensajeREQUEST.getContent();
                    if (libro.equals(contenidoREQUEST.split(" ")[1]) && getLocalName().equals(contenidoREQUEST.split(" ")[0])) {
                        esGanadorActual = false;
                        abandonarSubasta(libro);
                        precioActual = Integer.parseInt(contenidoREQUEST.split(" ")[2]);
                        librosComprados.put(libro, precioActual);
                        librosDeseados.remove(libro);
                        gui.actualizarTabla(obtenerSubastasData());
                        gui.actualizarTablaLibrosDeseados(getLibrosDeseados());
                        gui.actualizarTablaComprados(getLibrosComprados());
                        return;
                    }
                }
            }




        }
    }
}
