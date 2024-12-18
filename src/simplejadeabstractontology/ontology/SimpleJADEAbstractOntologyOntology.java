// file: SimpleJADEAbstractOntologyOntology.java generated by ontology bean generator.  DO NOT EDIT, UNLESS YOU ARE REALLY SURE WHAT YOU ARE DOING!
package simplejadeabstractontology.ontology;

import jade.content.onto.*;
import jade.content.schema.*;

/** file: SimpleJADEAbstractOntologyOntology.java
 * @author OntologyBeanGenerator v4.1
 * @version 2024/12/13, 20:43:24
 */
public class SimpleJADEAbstractOntologyOntology extends jade.content.onto.Ontology  {

  private static final long serialVersionUID = 4001257119321240738L;

  //NAME
  public static final String ONTOLOGY_NAME = "SimpleJADEAbstractOntology";
  // The singleton instance of this ontology
  private static Ontology theInstance = new SimpleJADEAbstractOntologyOntology();
  public static Ontology getInstance() {
     return theInstance;
  }


   // VOCABULARY
    public static final String MENSAJE_DESTINATARIO="destinatario";
    public static final String MENSAJE_FECHA="fecha";
    public static final String MENSAJE_LIBRO="libro";
    public static final String MENSAJE_FIN="fin";
    public static final String MENSAJE_GANADOR="ganador";
    public static final String MENSAJE_PRECIO="precio";
    public static final String MENSAJE="Mensaje";

  /**
   * Constructor
  */
  private SimpleJADEAbstractOntologyOntology(){ 
    super(ONTOLOGY_NAME, BasicOntology.getInstance());
    try { 

    // adding Concept(s)
    ConceptSchema mensajeSchema = new ConceptSchema(MENSAJE);
    add(mensajeSchema, simplejadeabstractontology.ontology.impl.DefaultMensaje.class);

    // adding AgentAction(s)

    // adding AID(s)

    // adding Predicate(s)


    // adding fields
    mensajeSchema.add(MENSAJE_PRECIO, (TermSchema)getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
    mensajeSchema.add(MENSAJE_GANADOR, (TermSchema)getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
    mensajeSchema.add(MENSAJE_FIN, (TermSchema)getSchema(BasicOntology.BOOLEAN), ObjectSchema.MANDATORY);
    mensajeSchema.add(MENSAJE_LIBRO, (TermSchema)getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
    mensajeSchema.add(MENSAJE_FECHA, (TermSchema)getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
    mensajeSchema.add(MENSAJE_DESTINATARIO, (TermSchema)getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);

    // adding name mappings

    // adding inheritance

   }catch (java.lang.Exception e) {e.printStackTrace();}
  }
}
