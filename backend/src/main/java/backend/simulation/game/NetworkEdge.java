package backend.simulation.game;

public class NetworkEdge {

    private final int fromNodeId;
    private final int toNodeId;
    private boolean active;
    private boolean defended;
    private boolean compromised;

    public NetworkEdge(int fromNodeId, int toNodeId) {
        this.fromNodeId = Math.min(fromNodeId, toNodeId);
        this.toNodeId = Math.max(fromNodeId, toNodeId);
        this.active = true;
        this.defended = false;
        this.compromised = false;
    }

    public int getFromNodeId() {
        return fromNodeId;
    }

    public int getToNodeId() {
        return toNodeId;
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

    public String key() {
        return fromNodeId + "-" + toNodeId;
    }
}
