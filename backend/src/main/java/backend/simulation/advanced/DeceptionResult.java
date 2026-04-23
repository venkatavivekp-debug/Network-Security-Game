package backend.simulation.advanced;

public class DeceptionResult {

    private int honeypotsInjected;
    private int honeypotEngagements;
    private int deceptionSuccessCount;
    private double attackerBudgetWastedOnDecoys;
    private double defenseCost;
    private double deceptionEffectiveness;

    public int getHoneypotsInjected() {
        return honeypotsInjected;
    }

    public void setHoneypotsInjected(int honeypotsInjected) {
        this.honeypotsInjected = honeypotsInjected;
    }

    public int getHoneypotEngagements() {
        return honeypotEngagements;
    }

    public void setHoneypotEngagements(int honeypotEngagements) {
        this.honeypotEngagements = honeypotEngagements;
    }

    public int getDeceptionSuccessCount() {
        return deceptionSuccessCount;
    }

    public void setDeceptionSuccessCount(int deceptionSuccessCount) {
        this.deceptionSuccessCount = deceptionSuccessCount;
    }

    public double getAttackerBudgetWastedOnDecoys() {
        return attackerBudgetWastedOnDecoys;
    }

    public void setAttackerBudgetWastedOnDecoys(double attackerBudgetWastedOnDecoys) {
        this.attackerBudgetWastedOnDecoys = attackerBudgetWastedOnDecoys;
    }

    public double getDefenseCost() {
        return defenseCost;
    }

    public void setDefenseCost(double defenseCost) {
        this.defenseCost = defenseCost;
    }

    public double getDeceptionEffectiveness() {
        return deceptionEffectiveness;
    }

    public void setDeceptionEffectiveness(double deceptionEffectiveness) {
        this.deceptionEffectiveness = deceptionEffectiveness;
    }

    public void recordEngagement(double wastedBudget, boolean successfulTrap) {
        honeypotEngagements++;
        attackerBudgetWastedOnDecoys += wastedBudget;
        if (successfulTrap) {
            deceptionSuccessCount++;
        }
    }
}
