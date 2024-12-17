// Ignacio Garbayo Fernández, 2024-2025
// Prácticas 6/7. Computación Distribuida

public class DataVendedor {
    private String libro;
    private int precio;
    private String ganador;
    private int numRespondedores;

    public DataVendedor(String libro, int precio, String ganador, int numRespondedores) {
        this.libro = libro;
        this.precio = precio;
        this.ganador = ganador;
        this.numRespondedores = numRespondedores;
    }

    public String getLibro() {
        return libro;
    }

    public int getPrecio() {
        return precio;
    }

    public String getGanador() {
        return ganador;
    }

    public int getNumRespondedores() {
        return numRespondedores;
    }
}
