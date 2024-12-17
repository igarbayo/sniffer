// Ignacio Garbayo Fernández, 2024-2025
// Prácticas 6/7. Computación Distribuida

public class PrecioGanador {

    private int precio;
    private boolean ganador;

    public int getPrecio() {
        return precio;
    }

    public void setPrecio(int precio) {
        this.precio = precio;
    }

    public boolean getGanador() {
        return ganador;
    }

    public void setGanador(boolean ganador) {
        this.ganador = ganador;
    }

    public PrecioGanador(int precio, boolean ganador) {
        this.precio = precio;
        this.ganador = ganador;
    }


}
