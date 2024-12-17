// Ignacio Garbayo Fernández, 2024-2025
// Prácticas 6/7. Computación Distribuida

import jade.core.AID;
import java.util.ArrayList;
import java.util.List;

public class PrecioGanadorNum {

    private int precio;
    private AID ganador;
    private List<AID> pujadores;
    private boolean primeraRonda;
    private boolean ultimaRonda;
    private Vendedor.LibroBehaviour behaviour;

    public int getPrecio() {
        return precio;
    }

    public void setPrecio(int precio) {
        this.precio = precio;
    }

    public AID getGanador() {
        return ganador;
    }

    public void setGanador(AID ganador) {
        this.ganador = ganador;
    }

    public List<AID> getPujadores() {
        return pujadores;
    }
    public void setPujadores(List<AID> pujadores) {
        this.pujadores = pujadores;
    }

    public boolean isPrimeraRonda() {
        return primeraRonda;
    }
    public void setPrimeraRonda(boolean primeraRonda) {
        this.primeraRonda = primeraRonda;
    }
    public boolean isUltimaRonda() {
        return ultimaRonda;
    }
    public void setUltimaRonda(boolean ultimaRonda) {
        this.ultimaRonda = ultimaRonda;
    }
    public Vendedor.LibroBehaviour getBehaviour() {
        return behaviour;
    }
    public void setBehaviour(Vendedor.LibroBehaviour behaviour) {
        this.behaviour = behaviour;
    }

    public PrecioGanadorNum(int precio, AID ganador, Vendedor.LibroBehaviour behaviour) {
        this.precio = precio;
        this.ganador = ganador;
        this.pujadores = new ArrayList<AID>();
        primeraRonda = true;
        ultimaRonda = false;
        this.behaviour = behaviour;
    }


}
