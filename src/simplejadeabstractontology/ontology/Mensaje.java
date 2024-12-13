package simplejadeabstractontology.ontology;



/**
* Protege name: Mensaje
* @author OntologyBeanGenerator v4.1
* @version 2024/12/13, 20:43:24
*/
public interface Mensaje extends jade.content.Concept {

   /**
   * Protege name: precio
   */
   public void setPrecio(int value);
   public int getPrecio();

   /**
   * Protege name: ganador
   */
   public void setGanador(String value);
   public String getGanador();

   /**
   * Protege name: fin
   */
   public void setFin(boolean value);
   public boolean getFin();

   /**
   * Protege name: libro
   */
   public void setLibro(String value);
   public String getLibro();

   /**
   * Protege name: fecha
   */
   public void setFecha(String value);
   public String getFecha();

   /**
   * Protege name: destinatario
   */
   public void setDestinatario(String value);
   public String getDestinatario();

}
