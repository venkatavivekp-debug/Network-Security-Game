package backend.simulation.advanced;

import backend.config.AdvancedSimulationProperties;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class MovingTargetDefenseService {

    private final AdvancedSimulationProperties properties;

    public MovingTargetDefenseService(AdvancedSimulationProperties properties) {
        this.properties = properties;
    }

    public MtdActionResult apply(
            AdvancedAttackGraph graph,
            DefenderPolicy defenderPolicy,
            Random random
    ) {
        double budget = defenderPolicy.getMtdBudget();
        if (budget <= 0.0) {
            return new MtdActionResult();
        }

        double riskBefore = averageEnabledExploitProbability(graph);
        MtdActionResult result = new MtdActionResult();

        if (budget >= properties.getMtdEdgeShuffleCost()) {
            int maxSwaps = Math.max(1, Math.min(properties.getMtdMaxEdgeSwaps(), (int) Math.floor(budget / properties.getMtdEdgeShuffleCost())));
            int swaps = graph.shuffleEdgeConnectivity(random, maxSwaps);
            if (swaps > 0) {
                double spent = swaps * properties.getMtdEdgeShuffleCost();
                budget -= spent;
                result.setEdgeRewirings(swaps);
                result.setDefenseCost(result.getDefenseCost() + spent);
            }
        }

        if (budget >= properties.getMtdIdentityRotationCost()) {
            // This is an abstract MTD analog: it permutes scalar node attributes (vulnerability/defense)
            // without modeling real asset identity systems (DNS, PKI, cloud IDs, etc.).
            int rotated = graph.rotateLogicalNodeIdentities(random);
            if (rotated > 0) {
                budget -= properties.getMtdIdentityRotationCost();
                result.setIdentityRotations(rotated);
                result.setDefenseCost(result.getDefenseCost() + properties.getMtdIdentityRotationCost());
            }
        }

        if (budget >= properties.getMtdEncryptionRotationCost()) {
            int rotatedEncryption = graph.rotateEncryptionModeImpacts(random);
            if (rotatedEncryption > 0) {
                budget -= properties.getMtdEncryptionRotationCost();
                result.setEncryptionRotations(rotatedEncryption);
                result.setDefenseCost(result.getDefenseCost() + properties.getMtdEncryptionRotationCost());
            }
        }

        if (budget >= properties.getMtdDisablePathCost()) {
            int maxDisable = Math.max(1, Math.min(properties.getMtdMaxPathDisables(), (int) Math.floor(budget / properties.getMtdDisablePathCost())));
            int disabled = graph.disableHighRiskEdges(random, maxDisable);
            if (disabled > 0) {
                double spent = disabled * properties.getMtdDisablePathCost();
                budget -= spent;
                result.setPathsDisabled(disabled);
                result.setDefenseCost(result.getDefenseCost() + spent);
            }
        }

        double riskAfter = averageEnabledExploitProbability(graph);
        double reduction = Math.max(0.0, riskBefore - riskAfter);
        double weightedActions = (result.getEdgeRewirings() * 0.05)
                + (result.getIdentityRotations() > 0 ? 0.05 : 0.0)
                + (result.getEncryptionRotations() > 0 ? 0.05 : 0.0)
                + (result.getPathsDisabled() * 0.08);

        result.setEffectiveness(clamp(reduction + weightedActions));
        return result;
    }

    private double averageEnabledExploitProbability(AdvancedAttackGraph graph) {
        if (graph.getEnabledEdges().isEmpty()) {
            return 0.0;
        }
        double sum = graph.getEnabledEdges().stream().mapToDouble(AdvancedEdge::getExploitProbability).sum();
        return sum / graph.getEnabledEdges().size();
    }

    private double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        return Math.min(value, 1.0);
    }

    public static class MtdActionResult {

        private int edgeRewirings;
        private int identityRotations;
        private int encryptionRotations;
        private int pathsDisabled;
        private double defenseCost;
        private double effectiveness;

        public int getEdgeRewirings() {
            return edgeRewirings;
        }

        public void setEdgeRewirings(int edgeRewirings) {
            this.edgeRewirings = edgeRewirings;
        }

        public int getIdentityRotations() {
            return identityRotations;
        }

        public void setIdentityRotations(int identityRotations) {
            this.identityRotations = identityRotations;
        }

        public int getEncryptionRotations() {
            return encryptionRotations;
        }

        public void setEncryptionRotations(int encryptionRotations) {
            this.encryptionRotations = encryptionRotations;
        }

        public int getPathsDisabled() {
            return pathsDisabled;
        }

        public void setPathsDisabled(int pathsDisabled) {
            this.pathsDisabled = pathsDisabled;
        }

        public double getDefenseCost() {
            return defenseCost;
        }

        public void setDefenseCost(double defenseCost) {
            this.defenseCost = defenseCost;
        }

        public double getEffectiveness() {
            return effectiveness;
        }

        public void setEffectiveness(double effectiveness) {
            this.effectiveness = effectiveness;
        }
    }
}
