package backend.simulation.advanced;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class AttackRoundState {

    private int roundNumber;
    private AttackStage currentStage;
    private double attackerBudgetRemaining;
    private double defenderBudgetRemaining;
    private int compromisedAtRoundStart;
    private int compromisedAtRoundEnd;
    private final Set<Integer> reconnaissanceTargets = new LinkedHashSet<>();
    private final Set<Integer> targetedNodes = new LinkedHashSet<>();
    private final Map<Integer, Integer> attackDepthByNode = new HashMap<>();
    private final Map<Integer, Double> footholdStrength = new HashMap<>();

    public int getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public AttackStage getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(AttackStage currentStage) {
        this.currentStage = currentStage;
    }

    public double getAttackerBudgetRemaining() {
        return attackerBudgetRemaining;
    }

    public void setAttackerBudgetRemaining(double attackerBudgetRemaining) {
        this.attackerBudgetRemaining = attackerBudgetRemaining;
    }

    public double getDefenderBudgetRemaining() {
        return defenderBudgetRemaining;
    }

    public void setDefenderBudgetRemaining(double defenderBudgetRemaining) {
        this.defenderBudgetRemaining = defenderBudgetRemaining;
    }

    public int getCompromisedAtRoundStart() {
        return compromisedAtRoundStart;
    }

    public void setCompromisedAtRoundStart(int compromisedAtRoundStart) {
        this.compromisedAtRoundStart = compromisedAtRoundStart;
    }

    public int getCompromisedAtRoundEnd() {
        return compromisedAtRoundEnd;
    }

    public void setCompromisedAtRoundEnd(int compromisedAtRoundEnd) {
        this.compromisedAtRoundEnd = compromisedAtRoundEnd;
    }

    public Set<Integer> getReconnaissanceTargets() {
        return reconnaissanceTargets;
    }

    public Set<Integer> getTargetedNodes() {
        return targetedNodes;
    }

    public Map<Integer, Integer> getAttackDepthByNode() {
        return attackDepthByNode;
    }

    public Map<Integer, Double> getFootholdStrength() {
        return footholdStrength;
    }
}
