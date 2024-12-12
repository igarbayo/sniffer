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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Comprador extends Agent {
    private Map<String, Integer> librosDeseados = new HashMap<>();
    private Map<String, EscucharSubastasBehaviour> subastasActivas  = new HashMap<>();
    private CompradorGUI gui;

    public Map<String, Integer> getLibrosDeseados() {
        return librosDeseados;
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

    public boolean eliminarLibro(String libro) {
        if (librosDeseados.containsKey(libro)) {
            librosDeseados.remove(libro);
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
        gui = new CompradorGUI(this);
        // Suscribir al DFService
        suscribirDF();

        //System.out.println("Comprador listo para participar en subastas.");

        //anadirLibro("LibroX", 70);
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
                if (libro.equals(libroRecibido) && max >= precioRecibido) {
                    // Enviar una respuesta "propose"
                    ACLMessage mensajePropuesta = new ACLMessage(ACLMessage.PROPOSE);
                    mensajePropuesta.addReceiver(mensajeCFP.getSender()); // Enviar al vendedor
                    mensajePropuesta.setContent("Estoy interesado en el libro " + libroRecibido + " por " + precioRecibido);
                    send(mensajePropuesta);
                    System.out.println("[C]\tEnviado 'propose' al vendedor por el libro: " + libroRecibido + " | " + precioRecibido);
                } else {
                    System.out.println("nada");
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

            // Esperar un mensaje de tipo "REQUEST"
            MessageTemplate request = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage mensajeREQUEST = myAgent.receive(request);
            if (mensajeREQUEST != null) {
                String contenidoREQUEST = mensajeREQUEST.getContent();
                if (libro.equals(contenidoREQUEST.split(" ")[2])) {
                    esGanadorActual = false;
                    abandonarSubasta(libro);
                    precioActual = Integer.parseInt(contenidoREQUEST.split(" ")[4]);
                    gui.actualizarTabla(obtenerSubastasData());
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
                    precioActual = Integer.parseInt(contenidoINFORM.split(" ")[7]);
                    gui.actualizarTabla(obtenerSubastasData());
                }
            }
        }
    }
}
