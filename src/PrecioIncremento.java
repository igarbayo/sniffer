public class PrecioIncremento {

    private int precio;
    private int incremento;

    public int getPrecio() {
        return precio;
    }

    public void setPrecio(int precio) {
        this.precio = precio;
    }

    public int getIncremento() {
        return incremento;
    }

    public void setIncremento(int incremento) {
        this.incremento = incremento;
    }

    public PrecioIncremento(int precio, int incremento) {
        this.precio = precio;
        this.incremento = incremento;
    }


}
