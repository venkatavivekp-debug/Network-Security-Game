package backend.simulation.game;

public class NetworkNode {

    private final int id;
    private boolean active;
    private boolean defended;
    private boolean compromised;

    public NetworkNode(int id) {
        this.id = id;
        this.active = true;
        this.defended = false;
        this.compromised = false;
    }

    public int getId() {
        return id;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isDefended() {
        return defended;
    }

    public void setDefended(boolean defended) {
        this.defended = defended;
    }

    public boolean isCompromised() {
        return compromised;
    }

    public void setCompromised(boolean compromised) {
        this.compromised = compromised;
    }
}
