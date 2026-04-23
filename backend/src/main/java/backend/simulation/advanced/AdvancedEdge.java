package backend.simulation.advanced;

public class AdvancedEdge {

    private int sourceNodeId;
    private int targetNodeId;
    private double attackCost;
    private double exploitProbability;
    private boolean enabled;

    public AdvancedEdge(int sourceNodeId, int targetNodeId, double attackCost, double exploitProbability, boolean enabled) {
        this.sourceNodeId = sourceNodeId;
        this.targetNodeId = targetNodeId;
        this.attackCost = attackCost;
        this.exploitProbability = clamp(exploitProbability);
        this.enabled = enabled;
    }

    public AdvancedEdge(AdvancedEdge other) {
        this(other.sourceNodeId, other.targetNodeId, other.attackCost, other.exploitProbability, other.enabled);
    }

    public int getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(int sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }

    public int getTargetNodeId() {
        return targetNodeId;
    }

    public void setTargetNodeId(int targetNodeId) {
        this.targetNodeId = targetNodeId;
    }

    public double getAttackCost() {
        return attackCost;
    }

    public void setAttackCost(double attackCost) {
        this.attackCost = attackCost;
    }

    public double getExploitProbability() {
        return exploitProbability;
    }

    public void setExploitProbability(double exploitProbability) {
        this.exploitProbability = clamp(exploitProbability);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String key() {
        return key(sourceNodeId, targetNodeId);
    }

    public static String key(int sourceNodeId, int targetNodeId) {
        return sourceNodeId + "->" + targetNodeId;
    }

    private double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        return Math.min(value, 1.0);
    }
}
