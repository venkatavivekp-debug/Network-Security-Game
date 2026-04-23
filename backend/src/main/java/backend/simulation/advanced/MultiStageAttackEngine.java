package backend.simulation.advanced;

import backend.config.AdvancedSimulationProperties;
import backend.model.AlgorithmType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class MultiStageAttackEngine {

    private final AdvancedSimulationProperties properties;
    private final CompromiseProbabilityService compromiseProbabilityService;

    public MultiStageAttackEngine(
            AdvancedSimulationProperties properties,
            CompromiseProbabilityService compromiseProbabilityService
    ) {
        this.properties = properties;
        this.compromiseProbabilityService = compromiseProbabilityService;
    }

    public AttackExecutionResult executeRound(
            AdvancedAttackGraph graph,
            AttackRoundState roundState,
            AttackerPolicy attackerPolicy,
            DefenderPolicy defenderPolicy,
            AlgorithmType algorithmType,
            Random random
    ) {
        AttackExecutionResult result = new AttackExecutionResult();

        roundState.setCurrentStage(AttackStage.RECONNAISSANCE);
        List<AdvancedNode> reconTargets = reconnaissance(graph, attackerPolicy, roundState);

        roundState.setCurrentStage(AttackStage.INITIAL_ACCESS);
        double spent = initialAccess(graph, reconTargets, attackerPolicy, algorithmType, roundState, result, random);

        roundState.setCurrentStage(AttackStage.LATERAL_MOVEMENT);
        spent += lateralMovement(graph, attackerPolicy, algorithmType, roundState, result, random);

        roundState.setCurrentStage(AttackStage.PERSISTENCE);
        spent += persistence(graph, attackerPolicy, roundState);

        roundState.setCurrentStage(AttackStage.IMPACT);
        double compromiseImpact = computeCompromiseImpact(graph);

        int maxDepth = Math.max(
                graph.computeMaxCompromisedPathDepth(),
                roundState.getAttackDepthByNode().values().stream().mapToInt(Integer::intValue).max().orElse(0)
        );

        result.setCompromiseImpact(compromiseImpact);
        result.setMaxAttackPathDepth(maxDepth);
        result.setAttackerBudgetSpent(Math.min(spent, attackerPolicy.getTotalBudget()));
        result.getTargetedNodes().addAll(roundState.getTargetedNodes());

        roundState.setCompromisedAtRoundEnd(graph.countCompromisedRealNodes());
        roundState.setAttackerBudgetRemaining(Math.max(0.0, attackerPolicy.getTotalBudget() - result.getAttackerBudgetSpent()));
        roundState.setDefenderBudgetRemaining(defenderPolicy.getTotalDefenseBudget());
        return result;
    }

    private List<AdvancedNode> reconnaissance(
            AdvancedAttackGraph graph,
            AttackerPolicy attackerPolicy,
            AttackRoundState roundState
    ) {
        int reconSlots = Math.max(1, (int) Math.round(attackerPolicy.getReconnaissanceBudget()));

        List<AdvancedNode> candidates = graph.getNodes().stream()
                .sorted(Comparator.comparingDouble(node -> -reconScore(node, attackerPolicy)))
                .toList();

        List<AdvancedNode> selected = new ArrayList<>();
        for (AdvancedNode node : candidates) {
            if (selected.size() >= reconSlots) {
                break;
            }
            selected.add(node);
            roundState.getReconnaissanceTargets().add(node.getId());
            roundState.getTargetedNodes().add(node.getId());
        }
        return selected;
    }

    private double initialAccess(
            AdvancedAttackGraph graph,
            List<AdvancedNode> reconTargets,
            AttackerPolicy attackerPolicy,
            AlgorithmType algorithmType,
            AttackRoundState roundState,
            AttackExecutionResult result,
            Random random
    ) {
        int attempts = Math.max(1, (int) Math.round(attackerPolicy.getCompromiseBudget()));
        double spent = 0.0;

        List<AdvancedNode> targetPool = reconTargets.isEmpty() ? graph.getNodes() : reconTargets;
        for (int i = 0; i < attempts; i++) {
            AdvancedNode target = targetPool.get(i % targetPool.size());
            roundState.getTargetedNodes().add(target.getId());

            result.setAttackAttempts(result.getAttackAttempts() + 1);
            spent += 0.5;

            boolean success = compromiseProbabilityService.compromiseAttempt(
                    target,
                    null,
                    algorithmType,
                    attackerPolicy.getPressure(),
                    random
            );

            if (success && !target.isCompromised()) {
                target.setCompromised(true);
                result.setNewlyCompromisedNodes(result.getNewlyCompromisedNodes() + (target.isHoneypot() ? 0 : 1));
                roundState.getAttackDepthByNode().put(target.getId(), 1);
            }

            if (target.isHoneypot()) {
                result.setHoneypotEngagements(result.getHoneypotEngagements() + 1);
                result.setDeceptionSuccessCount(result.getDeceptionSuccessCount() + 1);
                result.setAttackerBudgetWastedOnDecoys(result.getAttackerBudgetWastedOnDecoys() + 0.5);
            }

            if (compromiseProbabilityService.detectionAttempt(target, random)) {
                if (!target.isDetected()) {
                    result.setDetectedNodes(result.getDetectedNodes() + 1);
                }
                target.setDetected(true);
            }
        }
        return spent;
    }

    private double lateralMovement(
            AdvancedAttackGraph graph,
            AttackerPolicy attackerPolicy,
            AlgorithmType algorithmType,
            AttackRoundState roundState,
            AttackExecutionResult result,
            Random random
    ) {
        int attempts = Math.max(0, (int) Math.round(attackerPolicy.getLateralMovementBudget()));
        if (attempts <= 0) {
            return 0.0;
        }

        List<AdvancedNode> compromisedSources = graph.getNodes().stream()
                .filter(AdvancedNode::isCompromised)
                .filter(node -> !node.isHoneypot())
                .toList();

        if (compromisedSources.isEmpty()) {
            return 0.0;
        }

        double spent = 0.0;
        int sourceIndex = 0;

        for (int i = 0; i < attempts; i++) {
            AdvancedNode source = compromisedSources.get(sourceIndex % compromisedSources.size());
            sourceIndex++;

            List<AdvancedEdge> edges = graph.getOutgoingEdges(source.getId()).stream()
                    .sorted(Comparator.comparingDouble(AdvancedEdge::getExploitProbability).reversed())
                    .toList();
            if (edges.isEmpty()) {
                continue;
            }

            AdvancedEdge edge = pickEdge(edges, attackerPolicy, i);
            AdvancedNode target = graph.getNode(edge.getTargetNodeId());
            if (target == null) {
                continue;
            }

            roundState.getTargetedNodes().add(target.getId());
            result.setAttackAttempts(result.getAttackAttempts() + 1);
            spent += edge.getAttackCost();

            boolean success = compromiseProbabilityService.compromiseAttempt(
                    target,
                    edge,
                    algorithmType,
                    attackerPolicy.getPressure(),
                    random
            );

            if (success && !target.isCompromised()) {
                target.setCompromised(true);
                if (!target.isHoneypot()) {
                    result.setNewlyCompromisedNodes(result.getNewlyCompromisedNodes() + 1);
                }

                int sourceDepth = roundState.getAttackDepthByNode().getOrDefault(source.getId(), 1);
                roundState.getAttackDepthByNode().put(target.getId(), sourceDepth + 1);
            }

            if (target.isHoneypot()) {
                result.setHoneypotEngagements(result.getHoneypotEngagements() + 1);
                result.setDeceptionSuccessCount(result.getDeceptionSuccessCount() + (success ? 1 : 0));
                result.setAttackerBudgetWastedOnDecoys(result.getAttackerBudgetWastedOnDecoys() + edge.getAttackCost());
            }

            if (compromiseProbabilityService.detectionAttempt(target, random)) {
                if (!target.isDetected()) {
                    result.setDetectedNodes(result.getDetectedNodes() + 1);
                }
                target.setDetected(true);
            }
        }

        return spent;
    }

    private double persistence(
            AdvancedAttackGraph graph,
            AttackerPolicy attackerPolicy,
            AttackRoundState roundState
    ) {
        int attempts = Math.max(0, (int) Math.round(attackerPolicy.getPersistenceBudget()));
        if (attempts <= 0) {
            return 0.0;
        }

        List<AdvancedNode> compromised = graph.getNodes().stream()
                .filter(AdvancedNode::isCompromised)
                .toList();

        if (compromised.isEmpty()) {
            return 0.0;
        }

        double spent = 0.0;
        for (int i = 0; i < attempts; i++) {
            AdvancedNode node = compromised.get(i % compromised.size());
            double currentFoothold = roundState.getFootholdStrength().getOrDefault(node.getId(), 0.0);
            roundState.getFootholdStrength().put(node.getId(), currentFoothold + 0.2);

            double newDefense = node.getDefenseLevel() - (properties.getPersistenceDefenseDecay() * (0.5 + attackerPolicy.getPressure()));
            node.setDefenseLevel(newDefense);
            spent += 0.3;
        }

        return spent;
    }

    private AdvancedEdge pickEdge(List<AdvancedEdge> candidates, AttackerPolicy policy, int cursor) {
        if (!policy.getPreferredTargets().isEmpty()) {
            for (AdvancedEdge edge : candidates) {
                if (policy.getPreferredTargets().contains(edge.getTargetNodeId())) {
                    return edge;
                }
            }
        }

        for (AdvancedEdge edge : candidates) {
            if (!policy.getAvoidedTargets().contains(edge.getTargetNodeId())) {
                return edge;
            }
        }

        return candidates.get(cursor % candidates.size());
    }

    private double reconScore(AdvancedNode node, AttackerPolicy policy) {
        double score = node.getVulnerabilityScore() + assetPriority(node.getAssetType()) - node.getDefenseLevel();
        if (policy.getPreferredTargets().contains(node.getId())) {
            score += 0.3;
        }
        if (policy.getAvoidedTargets().contains(node.getId())) {
            score -= 0.25;
        }
        return score;
    }

    private double assetPriority(AssetType type) {
        return switch (type) {
            case DATABASE -> 1.0;
            case SERVER -> 0.8;
            case GATEWAY -> 0.75;
            case IOT_DEVICE -> 0.45;
            case HONEYPOT -> 0.55;
        };
    }

    private double computeCompromiseImpact(AdvancedAttackGraph graph) {
        Map<AssetType, Double> weightByType = Map.of(
                AssetType.SERVER, properties.getImpactServerWeight(),
                AssetType.IOT_DEVICE, properties.getImpactIotWeight(),
                AssetType.GATEWAY, properties.getImpactGatewayWeight(),
                AssetType.DATABASE, properties.getImpactDatabaseWeight(),
                AssetType.HONEYPOT, properties.getImpactHoneypotWeight()
        );

        double totalWeight = graph.getNodes().stream()
                .mapToDouble(node -> weightByType.getOrDefault(node.getAssetType(), 0.0))
                .sum();
        if (totalWeight <= 0.0) {
            return 0.0;
        }

        double compromisedWeight = graph.getNodes().stream()
                .filter(AdvancedNode::isCompromised)
                .mapToDouble(node -> weightByType.getOrDefault(node.getAssetType(), 0.0))
                .sum();

        double ratio = compromisedWeight / totalWeight;
        if (ratio < 0.0) {
            return 0.0;
        }
        return Math.min(ratio, 1.0);
    }
}
