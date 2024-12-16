import jade.content.ContentElement;
import jade.content.ContentManager;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
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
import simplejadeabstractontology.ontology.impl.DefaultMensaje;

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

    /**
     * Añado al comprador al DFService
     */
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

    /**
     * Elimina al comprador del DFService
     */
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
                try {
                    ContentElement contenido = manager.extractContent(mensajeCFP);
                    if (contenido instanceof Action) {
                        Action action = (Action) contenido;
                        Object objeto = action.getAction();
                        if (objeto instanceof DefaultMensaje) {
                            DefaultMensaje mensaje = (DefaultMensaje) objeto;

                            if (System.currentTimeMillis() < mensajeCFP.getReplyByDate().getTime() &&
                                    getLocalName().equals(mensaje.getDestinatario()) &&
                                    libro.equals(mensaje.getLibro())) {
                                if (max >= mensaje.getPrecio()) {
                                    // Creamos el mensaje ontológico
                                    DefaultMensaje mensajeP = new DefaultMensaje();
                                    mensajeP.setLibro(mensaje.getLibro());
                                    mensajeP.setPrecio(mensaje.getPrecio());

                                    // Enviar una respuesta "propose"
                                    ACLMessage mensajePropuesta = new ACLMessage(ACLMessage.PROPOSE);
                                    mensajePropuesta.addReceiver(mensajeCFP.getSender()); // Enviar al vendedor
                                    mensajePropuesta.setOntology(ontology.getName());
                                    mensajePropuesta.setLanguage(codec.getName());
                                    Action actionP = new Action(getAID(), mensajeP);
                                    manager.fillContent(mensajePropuesta, actionP);
                                    send(mensajePropuesta);

                                    // Print para debug
                                    System.out.println("[C]\tEnviado 'propose' al vendedor por el libro: "
                                            + mensaje.getLibro() + " | " + mensaje.getPrecio());
                                } else {
                                    DefaultMensaje mensajeP = new DefaultMensaje();
                                    mensajeP.setLibro(mensaje.getLibro());
                                    ACLMessage mensajeNU = new ACLMessage(ACLMessage.NOT_UNDERSTOOD);
                                    mensajeNU.addReceiver(mensajeCFP.getSender()); // Enviar al vendedor
                                    mensajeNU.setOntology(ontology.getName());
                                    mensajeNU.setLanguage(codec.getName());
                                    Action actionP = new Action(getAID(), mensajeP);
                                    manager.fillContent(mensajeNU, actionP);
                                    send(mensajeNU);
                                    // Print para debug
                                    System.out.println("NU");
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Esperar un mensaje de tipo "ACCEPT_PROPOSAL"
            MessageTemplate acp = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage mensajeACP = myAgent.receive(acp);
            if (mensajeACP != null) {
                try {
                    ContentElement contenido = manager.extractContent(mensajeACP);
                    if (contenido instanceof Action) {
                        Action action = (Action) contenido;
                        Object objeto = action.getAction();
                        if (objeto instanceof DefaultMensaje) {
                            DefaultMensaje mensaje = (DefaultMensaje) objeto;
                            if (libro.equals(mensaje.getLibro()) && getLocalName().equals(mensaje.getDestinatario())) {
                                esGanadorActual = true;
                                precioActual = mensaje.getPrecio();

                                // Actualizamos la tabla de subastas
                                gui.actualizarTabla(obtenerSubastasData());
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Esperar un mensaje de tipo "REJECT_PROPOSAL"
            MessageTemplate rjp = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);
            ACLMessage mensajeRJP = myAgent.receive(rjp);
            if (mensajeRJP != null) {
                try {
                    ContentElement contenido = manager.extractContent(mensajeRJP);
                    if (contenido instanceof Action) {
                        Action action = (Action) contenido;
                        Object objeto = action.getAction();
                        if (objeto instanceof DefaultMensaje) {
                            DefaultMensaje mensaje = (DefaultMensaje) objeto;
                            if (libro.equals(mensaje.getLibro()) && getLocalName().equals(mensaje.getDestinatario())) {
                                esGanadorActual = false;
                                precioActual = mensaje.getPrecio();

                                // Actualizamos la tabla de subastas
                                gui.actualizarTabla(obtenerSubastasData());
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Esperar un mensaje de tipo "INFORM"
            MessageTemplate inform = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage mensajeINFORM = myAgent.receive(inform);
            if (mensajeINFORM != null) {
                // Print para debug
                System.out.println("Recibido INFORM");
                // Print para debug
                System.out.println(mensajeINFORM.getContent());

                try {
                    ContentElement contenido = manager.extractContent(mensajeINFORM);
                    if (contenido instanceof Action) {
                        Action action = (Action) contenido;
                        Object objeto = action.getAction();
                        if (objeto instanceof DefaultMensaje) {
                            DefaultMensaje mensaje = (DefaultMensaje) objeto;
                            if (mensaje.getFin() && libro.equals(mensaje.getLibro()) &&
                                getLocalName().equals(mensaje.getDestinatario())) {
                                if (!mensaje.getGanador().equals(getLocalName())) {
                                    subastasActivas.remove(libro);

                                    // Print para debug
                                    System.out.println("Elimino subasta");
                                }
                                precioActual = mensaje.getPrecio();

                                // Actualizamos la tabla de subastas
                                gui.actualizarTabla(obtenerSubastasData());

                                // Print para debug
                                System.out.println(mensaje.getGanador());
                                // Print para debug
                                System.out.println(getLocalName());
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Esperar un mensaje de tipo "REQUEST"
            MessageTemplate request = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            if (myAgent != null) {
                ACLMessage mensajeREQUEST = myAgent.receive(request);
                if (mensajeREQUEST != null) {
                    try {
                        ContentElement contenido = manager.extractContent(mensajeREQUEST);
                        if (contenido instanceof Action) {
                            Action action = (Action) contenido;
                            Object objeto = action.getAction();
                            if (objeto instanceof DefaultMensaje) {
                                DefaultMensaje mensaje = (DefaultMensaje) objeto;

                                // Print para debug
                                System.out.println("Recibido REQUEST");

                                if (libro.equals(mensaje.getLibro()) && getLocalName().equals(mensaje.getGanador())) {
                                    // Actualizaciones necesarias
                                    esGanadorActual = false;
                                    precioActual = mensaje.getPrecio();
                                    comprarLibro(libro, precioActual);

                                    // Finalizamos el comportamiento
                                    abandonarSubasta(libro);

                                    // Actualizamos las tablas de la gui
                                    gui.actualizarTabla(obtenerSubastasData());
                                    gui.actualizarTablaLibrosDeseados(getLibrosDeseados());
                                    gui.actualizarTablaComprados(getLibrosComprados());
                                    return;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
