package backend.simulation.advanced;

import java.util.LinkedHashSet;
import java.util.Set;

public class DefenderPolicy {

    private double totalDefenseBudget;
    private double hardeningBudget;
    private double mtdBudget;
    private double deceptionBudget;
    private double recoveryBudget;
    private final Set<Integer> priorityAssets = new LinkedHashSet<>();

    public double getTotalDefenseBudget() {
        return totalDefenseBudget;
    }

    public void setTotalDefenseBudget(double totalDefenseBudget) {
        this.totalDefenseBudget = totalDefenseBudget;
    }

    public double getHardeningBudget() {
        return hardeningBudget;
    }

    public void setHardeningBudget(double hardeningBudget) {
        this.hardeningBudget = hardeningBudget;
    }

    public double getMtdBudget() {
        return mtdBudget;
    }

    public void setMtdBudget(double mtdBudget) {
        this.mtdBudget = mtdBudget;
    }

    public double getDeceptionBudget() {
        return deceptionBudget;
    }

    public void setDeceptionBudget(double deceptionBudget) {
        this.deceptionBudget = deceptionBudget;
    }

    public double getRecoveryBudget() {
        return recoveryBudget;
    }

    public void setRecoveryBudget(double recoveryBudget) {
        this.recoveryBudget = recoveryBudget;
    }

    public Set<Integer> getPriorityAssets() {
        return priorityAssets;
    }
}
