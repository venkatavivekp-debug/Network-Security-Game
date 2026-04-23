package backend.simulation.advanced;

import java.util.LinkedHashSet;
import java.util.Set;

public class AttackerPolicy {

    private double totalBudget;
    private double reconnaissanceBudget;
    private double compromiseBudget;
    private double lateralMovementBudget;
    private double persistenceBudget;
    private double pressure;
    private final Set<Integer> preferredTargets = new LinkedHashSet<>();
    private final Set<Integer> avoidedTargets = new LinkedHashSet<>();

    public double getTotalBudget() {
        return totalBudget;
    }

    public void setTotalBudget(double totalBudget) {
        this.totalBudget = totalBudget;
    }

    public double getReconnaissanceBudget() {
        return reconnaissanceBudget;
    }

    public void setReconnaissanceBudget(double reconnaissanceBudget) {
        this.reconnaissanceBudget = reconnaissanceBudget;
    }

    public double getCompromiseBudget() {
        return compromiseBudget;
    }

    public void setCompromiseBudget(double compromiseBudget) {
        this.compromiseBudget = compromiseBudget;
    }

    public double getLateralMovementBudget() {
        return lateralMovementBudget;
    }

    public void setLateralMovementBudget(double lateralMovementBudget) {
        this.lateralMovementBudget = lateralMovementBudget;
    }

    public double getPersistenceBudget() {
        return persistenceBudget;
    }

    public void setPersistenceBudget(double persistenceBudget) {
        this.persistenceBudget = persistenceBudget;
    }

    public double getPressure() {
        return pressure;
    }

    public void setPressure(double pressure) {
        this.pressure = pressure;
    }

    public Set<Integer> getPreferredTargets() {
        return preferredTargets;
    }

    public Set<Integer> getAvoidedTargets() {
        return avoidedTargets;
    }
}
