import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class Comprador extends Agent {
    private String libroDeseado = "LibroX";
    private int limiteGasto = 70;  // Limite máximo que el comprador puede gastar

    @Override
    protected void setup() {
        // Suscribir al DFService
        suscribirDF();

        //System.out.println("Comprador listo para participar en subastas.");

        // Comienza el comportamiento de escuchar las ofertas de subasta
        addBehaviour(new EscucharSubastasBehaviour(this));
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

        public EscucharSubastasBehaviour(Agent a) {
            super(a);
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
                if (libroRecibido.equals(libroDeseado) && limiteGasto >= precioRecibido) {
                    // Enviar una respuesta "propose"
                    ACLMessage mensajePropuesta = new ACLMessage(ACLMessage.PROPOSE);
                    mensajePropuesta.addReceiver(mensajeCFP.getSender()); // Enviar al vendedor
                    mensajePropuesta.setContent("Estoy interesado en el libro " + libroRecibido + " por " + precioRecibido);
                    send(mensajePropuesta);
                    System.out.println("[C]\tEnviado 'propose' al vendedor por el libro: " + libroRecibido + " | " + precioRecibido);
                }
            } else {
                block();  // Bloquea el comportamiento hasta que se reciba un mensaje
            }
        }
    }
}
