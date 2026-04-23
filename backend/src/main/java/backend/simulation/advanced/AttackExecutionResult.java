package backend.simulation.advanced;

import java.util.LinkedHashSet;
import java.util.Set;

public class AttackExecutionResult {

    private int newlyCompromisedNodes;
    private int detectedNodes;
    private int attackAttempts;
    private int maxAttackPathDepth;
    private int honeypotEngagements;
    private int deceptionSuccessCount;
    private double attackerBudgetSpent;
    private double attackerBudgetWastedOnDecoys;
    private double compromiseImpact;
    private final Set<Integer> targetedNodes = new LinkedHashSet<>();

    public int getNewlyCompromisedNodes() {
        return newlyCompromisedNodes;
    }

    public void setNewlyCompromisedNodes(int newlyCompromisedNodes) {
        this.newlyCompromisedNodes = newlyCompromisedNodes;
    }

    public int getDetectedNodes() {
        return detectedNodes;
    }

    public void setDetectedNodes(int detectedNodes) {
        this.detectedNodes = detectedNodes;
    }

    public int getAttackAttempts() {
        return attackAttempts;
    }

    public void setAttackAttempts(int attackAttempts) {
        this.attackAttempts = attackAttempts;
    }

    public int getMaxAttackPathDepth() {
        return maxAttackPathDepth;
    }

    public void setMaxAttackPathDepth(int maxAttackPathDepth) {
        this.maxAttackPathDepth = maxAttackPathDepth;
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

    public double getAttackerBudgetSpent() {
        return attackerBudgetSpent;
    }

    public void setAttackerBudgetSpent(double attackerBudgetSpent) {
        this.attackerBudgetSpent = attackerBudgetSpent;
    }

    public double getAttackerBudgetWastedOnDecoys() {
        return attackerBudgetWastedOnDecoys;
    }

    public void setAttackerBudgetWastedOnDecoys(double attackerBudgetWastedOnDecoys) {
        this.attackerBudgetWastedOnDecoys = attackerBudgetWastedOnDecoys;
    }

    public double getCompromiseImpact() {
        return compromiseImpact;
    }

    public void setCompromiseImpact(double compromiseImpact) {
        this.compromiseImpact = compromiseImpact;
    }

    public Set<Integer> getTargetedNodes() {
        return targetedNodes;
    }
}
