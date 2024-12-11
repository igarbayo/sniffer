public class DataComprador {
    private String libro;
    private int precioRonda;
    private boolean soyGanadorActual;

    // Constructor
    public DataComprador(String libro, int precioRonda, boolean soyGanadorActual) {
        this.libro = libro;
        this.precioRonda = precioRonda;
        this.soyGanadorActual = soyGanadorActual;
    }

    // Getters y setters
    public String getLibro() {
        return libro;
    }

    public void setLibro(String libro) {
        this.libro = libro;
    }

    public int getPrecioRonda() {
        return precioRonda;
    }

    public void setPrecioRonda(int precioRonda) {
        this.precioRonda = precioRonda;
    }

    public boolean isSoyGanadorActual() {
        return soyGanadorActual;
    }

    public void setSoyGanadorActual(boolean soyGanadorActual) {
        this.soyGanadorActual = soyGanadorActual;
    }
}
