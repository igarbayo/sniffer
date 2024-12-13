package simplejadeabstractontology.ontology.impl;


import simplejadeabstractontology.ontology.*;

/**
* Protege name: Mensaje
* @author OntologyBeanGenerator v4.1
* @version 2024/12/13, 20:43:24
*/
public class DefaultMensaje implements Mensaje {

  private static final long serialVersionUID = 4001257119321240738L;

  private String _internalInstanceName = null;

  public DefaultMensaje() {
    this._internalInstanceName = "";
  }

  public DefaultMensaje(String instance_name) {
    this._internalInstanceName = instance_name;
  }

  public String toString() {
    return _internalInstanceName;
  }

   /**
   * Protege name: precio
   */
   private int precio;
   public void setPrecio(int value) { 
    this.precio=value;
   }
   public int getPrecio() {
     return this.precio;
   }

   /**
   * Protege name: ganador
   */
   private String ganador;
   public void setGanador(String value) { 
    this.ganador=value;
   }
   public String getGanador() {
     return this.ganador;
   }

   /**
   * Protege name: fin
   */
   private boolean fin;
   public void setFin(boolean value) { 
    this.fin=value;
   }
   public boolean getFin() {
     return this.fin;
   }

   /**
   * Protege name: libro
   */
   private String libro;
   public void setLibro(String value) { 
    this.libro=value;
   }
   public String getLibro() {
     return this.libro;
   }

   /**
   * Protege name: fecha
   */
   private String fecha;
   public void setFecha(String value) { 
    this.fecha=value;
   }
   public String getFecha() {
     return this.fecha;
   }

   /**
   * Protege name: destinatario
   */
   private String destinatario;
   public void setDestinatario(String value) { 
    this.destinatario=value;
   }
   public String getDestinatario() {
     return this.destinatario;
   }

}
