package backend.simulation.advanced;

import backend.config.AdvancedSimulationProperties;
import backend.model.AlgorithmType;
import org.springframework.stereotype.Service;

@Service
public class CompromiseProbabilityService {

    private final AdvancedSimulationProperties properties;
    private final RandomStrategySupport randomSupport;

    public CompromiseProbabilityService(AdvancedSimulationProperties properties, RandomStrategySupport randomSupport) {
        this.properties = properties;
        this.randomSupport = randomSupport;
    }

    public double compromiseProbability(
            AdvancedNode node,
            AdvancedEdge edge,
            AlgorithmType algorithmType,
            double attackerPressure
    ) {
        double edgeFactor = edge == null ? 1.0 : edge.getExploitProbability();
        double vulnerabilityFactor = node.getVulnerabilityScore();
        double defenseFactor = 1.0 - (properties.getDefenseMitigationWeight() * node.getDefenseLevel());
        double pressureFactor = 1.0 + (properties.getPressureWeight() * attackerPressure);
        double algorithmFactor = algorithmMultiplier(algorithmType);
        double postureFactor = nodePostureMultiplier(node.getEncryptionModeImpact());

        double weightedBase = properties.getBaseCompromiseFloor()
                + properties.getVulnerabilityWeight() * vulnerabilityFactor
                + properties.getExploitWeight() * edgeFactor;

        // NOTE: `algorithmType` is the modeled global security posture for the run (scenario knob).
        // `node.encryptionModeImpact` is an optional per-node posture used for heterogeneity experiments.
        // When both are present, we average the two multipliers to avoid double-counting the same knob twice.
        double combinedAlgorithmFactor = combineMultipliers(algorithmFactor, postureFactor);

        double probability = weightedBase * defenseFactor * pressureFactor * combinedAlgorithmFactor;
        if (node.isHoneypot()) {
            probability *= 0.95;
        }

        return randomSupport.clamp(probability);
    }

    public double detectionProbability(AdvancedNode node) {
        double probability = properties.getBaseDetectionProbability();
        probability += properties.getDefendedDetectionBoost() * node.getDefenseLevel();
        if (node.isHoneypot()) {
            probability += properties.getHoneypotDetectionBoost();
        }
        if (node.isDetected()) {
            probability += 0.05;
        }
        return randomSupport.clamp(probability);
    }

    public boolean compromiseAttempt(
            AdvancedNode node,
            AdvancedEdge edge,
            AlgorithmType algorithmType,
            double attackerPressure,
            java.util.Random random
    ) {
        return randomSupport.chance(random, compromiseProbability(node, edge, algorithmType, attackerPressure));
    }

    public boolean detectionAttempt(AdvancedNode node, java.util.Random random) {
        return randomSupport.chance(random, detectionProbability(node));
    }

    private double algorithmMultiplier(AlgorithmType algorithmType) {
        if (algorithmType == null) {
            return properties.getNormalCompromiseMultiplier();
        }

        return switch (algorithmType) {
            case NORMAL -> properties.getNormalCompromiseMultiplier();
            case SHCS -> properties.getShcsCompromiseMultiplier();
            case CPHS -> properties.getCphsCompromiseMultiplier();
            case ADAPTIVE -> (properties.getShcsCompromiseMultiplier() + properties.getCphsCompromiseMultiplier()) / 2.0;
        };
    }

    private double nodePostureMultiplier(AlgorithmType posture) {
        if (posture == null) {
            return 1.0;
        }
        return algorithmMultiplier(posture);
    }

    private double combineMultipliers(double globalMultiplier, double nodeMultiplier) {
        if (nodeMultiplier <= 0.0) {
            return globalMultiplier;
        }
        if (globalMultiplier <= 0.0) {
            return nodeMultiplier;
        }
        return (globalMultiplier + nodeMultiplier) / 2.0;
    }
}
