package backend.simulation.advanced;

import backend.config.AdvancedSimulationProperties;
import backend.exception.BadRequestException;
import backend.model.AlgorithmType;
import backend.util.SeedUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class AdaptiveStrategyEngine {

    private final AdvancedSimulationProperties properties;
    private final RandomStrategySupport randomStrategySupport;
    private final MultiStageAttackEngine multiStageAttackEngine;
    private final MovingTargetDefenseService movingTargetDefenseService;
    private final HoneypotService honeypotService;

    public AdaptiveStrategyEngine(
            AdvancedSimulationProperties properties,
            RandomStrategySupport randomStrategySupport,
            MultiStageAttackEngine multiStageAttackEngine,
            MovingTargetDefenseService movingTargetDefenseService,
            HoneypotService honeypotService
    ) {
        this.properties = properties;
        this.randomStrategySupport = randomStrategySupport;
        this.multiStageAttackEngine = multiStageAttackEngine;
        this.movingTargetDefenseService = movingTargetDefenseService;
        this.honeypotService = honeypotService;
    }

    public AdvancedSimulationMetrics runSimulation(
            int numNodes,
            int numEdges,
            int attackBudget,
            int defenseBudget,
            int recoveryBudget,
            int rounds,
            boolean enableMtd,
            boolean enableDeception,
            AlgorithmType algorithmType,
            Long requestedSeed
    ) {
        validateInputs(numNodes, numEdges, attackBudget, defenseBudget, recoveryBudget, rounds);

        long deterministicFallback = SeedUtil.stableHash64(
                "advanced-sim-v2",
                String.valueOf(numNodes),
                String.valueOf(numEdges),
                String.valueOf(attackBudget),
                String.valueOf(defenseBudget),
                String.valueOf(recoveryBudget),
                String.valueOf(rounds),
                String.valueOf(enableMtd),
                String.valueOf(enableDeception),
                String.valueOf(algorithmType)
        );

        long seedUsed = randomStrategySupport.resolveSeed(requestedSeed, deterministicFallback);
        AdvancedAttackGraph graph = AdvancedAttackGraph.generateRandomGraph(
                numNodes,
                numEdges,
                algorithmType,
                seedUsed,
                randomStrategySupport
        );

        AdvancedSimulationMetrics metrics = new AdvancedSimulationMetrics();
        metrics.setSeedUsed(seedUsed);

        Map<Integer, Integer> targetedFrequency = new HashMap<>();
        Map<Integer, Integer> trappedFrequency = new HashMap<>();

        double totalResilience = 0.0;
        double totalAttackEfficiency = 0.0;
        double totalDefenseEfficiency = 0.0;
        double totalDetectionRate = 0.0;
        double totalRecoveryContribution = 0.0;
        double totalDeceptionEffectiveness = 0.0;
        double totalMtdEffectiveness = 0.0;
        double totalAttackerUtility = 0.0;
        double totalDefenderUtility = 0.0;

        int totalRecoveredNodes = 0;
        int totalNewCompromisedNodes = 0;
        int weightedCompromiseTime = 0;

        RoundMetrics previousRoundMetrics = null;

        for (int round = 1; round <= rounds; round++) {
            Random roundRandom = randomStrategySupport.deriveRandom(seedUsed, "advanced-round", round);

            DefenderPolicy defenderPolicy = buildDefenderPolicy(
                    defenseBudget,
                    recoveryBudget,
                    enableMtd,
                    enableDeception,
                    previousRoundMetrics,
                    targetedFrequency
            );

            AttackerPolicy attackerPolicy = buildAttackerPolicy(
                    attackBudget,
                    previousRoundMetrics,
                    targetedFrequency,
                    trappedFrequency
            );

            double hardeningCost = applyHardening(graph, defenderPolicy, targetedFrequency);

            DeceptionResult deceptionResult = new DeceptionResult();
            if (enableDeception) {
                deceptionResult = honeypotService.deployHoneypots(graph, defenderPolicy, algorithmType, roundRandom);
            }

            MovingTargetDefenseService.MtdActionResult mtdActionResult = new MovingTargetDefenseService.MtdActionResult();
            if (enableMtd) {
                mtdActionResult = movingTargetDefenseService.apply(graph, defenderPolicy, roundRandom);
            }

            AttackRoundState roundState = new AttackRoundState();
            roundState.setRoundNumber(round);
            roundState.setCompromisedAtRoundStart(graph.countCompromisedRealNodes());

            AttackExecutionResult attackExecutionResult = multiStageAttackEngine.executeRound(
                    graph,
                    roundState,
                    attackerPolicy,
                    defenderPolicy,
                    algorithmType,
                    roundRandom
            );

            mergeDeceptionEngagements(deceptionResult, attackExecutionResult);

            RecoveryOutcome recoveryOutcome = recoverCompromisedNodes(graph, defenderPolicy.getRecoveryBudget(), roundRandom);

            for (Integer targetedNode : attackExecutionResult.getTargetedNodes()) {
                targetedFrequency.merge(targetedNode, 1, Integer::sum);
                AdvancedNode node = graph.getNode(targetedNode);
                if (node != null && node.isHoneypot()) {
                    trappedFrequency.merge(targetedNode, 1, Integer::sum);
                }
            }

            RoundMetrics roundMetrics = buildRoundMetrics(
                    graph,
                    round,
                    numNodes,
                    hardeningCost,
                    deceptionResult,
                    mtdActionResult,
                    attackExecutionResult,
                    recoveryOutcome
            );

            metrics.getCompromiseTimeline().add(roundValue(roundMetrics.getCompromiseRatio()));
            metrics.getCompromisedNodeCountPerRound().add(roundMetrics.getCompromisedNodeCount());
            metrics.getRoundMetrics().add(roundMetrics);

            totalResilience += roundMetrics.getResilienceScore();
            totalAttackEfficiency += roundMetrics.getAttackEfficiency();
            totalDefenseEfficiency += roundMetrics.getDefenseEfficiency();
            totalDetectionRate += roundMetrics.getDetectionRate();
            totalRecoveryContribution += roundMetrics.getRecoveredNodes();
            totalDeceptionEffectiveness += roundMetrics.getDeceptionEffectiveness();
            totalMtdEffectiveness += roundMetrics.getMtdEffectiveness();
            totalAttackerUtility += roundMetrics.getAttackerUtility();
            totalDefenderUtility += roundMetrics.getDefenderUtility();

            totalRecoveredNodes += roundMetrics.getRecoveredNodes();
            totalNewCompromisedNodes += roundMetrics.getNewlyCompromisedNodes();
            weightedCompromiseTime += round * roundMetrics.getNewlyCompromisedNodes();

            previousRoundMetrics = roundMetrics;
        }

        double roundsAsDouble = rounds;
        metrics.setMeanTimeToCompromise(totalNewCompromisedNodes == 0
                ? roundsAsDouble
                : roundValue((double) weightedCompromiseTime / totalNewCompromisedNodes));

        metrics.setMaxAttackPathDepth(metrics.getRoundMetrics().stream()
                .mapToInt(RoundMetrics::getMaxAttackPathDepth)
                .max()
                .orElse(0));

        metrics.setResilienceScore(roundValue(clamp(totalResilience / roundsAsDouble)));
        metrics.setDefenseEfficiency(roundValue(clamp(totalDefenseEfficiency / roundsAsDouble)));
        metrics.setAttackEfficiency(roundValue(clamp(totalAttackEfficiency / roundsAsDouble)));
        metrics.setDeceptionEffectiveness(roundValue(clamp(totalDeceptionEffectiveness / roundsAsDouble)));
        metrics.setMtdEffectiveness(roundValue(clamp(totalMtdEffectiveness / roundsAsDouble)));
        metrics.setDetectionRate(roundValue(clamp(totalDetectionRate / roundsAsDouble)));

        double recoveryContribution = totalNewCompromisedNodes == 0
                ? 0.0
                : (double) totalRecoveredNodes / totalNewCompromisedNodes;
        metrics.setRecoveryContribution(roundValue(clamp(recoveryContribution)));

        metrics.setFinalCompromisedNodes(Math.min(graph.countCompromisedRealNodes(), numNodes));
        metrics.setFinalProtectedNodes(Math.min(graph.countProtectedNodes(0.7), numNodes));
        metrics.setAttackerUtility(roundValue(totalAttackerUtility));
        metrics.setDefenderUtility(roundValue(totalDefenderUtility));

        validateMetrics(metrics, numNodes, rounds);
        return metrics;
    }

    private DefenderPolicy buildDefenderPolicy(
            int defenseBudget,
            int recoveryBudget,
            boolean enableMtd,
            boolean enableDeception,
            RoundMetrics previousRoundMetrics,
            Map<Integer, Integer> targetedFrequency
    ) {
        DefenderPolicy policy = new DefenderPolicy();
        policy.setTotalDefenseBudget(defenseBudget);

        double hardeningShare = 0.45;
        double mtdShare = enableMtd ? 0.25 : 0.0;
        double deceptionShare = enableDeception ? 0.2 : 0.0;

        if (previousRoundMetrics != null) {
            if (previousRoundMetrics.getTargetedNodes() > 0 && previousRoundMetrics.getDetectionRate() < 0.3) {
                hardeningShare += 0.1;
            }
            if (previousRoundMetrics.getDeceptionEffectiveness() > 0.3 && enableDeception) {
                deceptionShare += 0.1;
                hardeningShare -= 0.05;
            }
            if (previousRoundMetrics.getCompromiseRatio() > 0.4 && enableMtd) {
                mtdShare += 0.1;
            }
        }

        hardeningShare = clamp(hardeningShare);
        mtdShare = clamp(mtdShare);
        deceptionShare = clamp(deceptionShare);

        double shareSum = hardeningShare + mtdShare + deceptionShare;
        if (shareSum > 1.0) {
            hardeningShare /= shareSum;
            mtdShare /= shareSum;
            deceptionShare /= shareSum;
        }

        policy.setHardeningBudget(defenseBudget * hardeningShare);
        policy.setMtdBudget(defenseBudget * mtdShare);
        policy.setDeceptionBudget(defenseBudget * deceptionShare);
        policy.setRecoveryBudget(recoveryBudget);

        targetedFrequency.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .forEach(policy.getPriorityAssets()::add);

        return policy;
    }

    private AttackerPolicy buildAttackerPolicy(
            int attackBudget,
            RoundMetrics previousRoundMetrics,
            Map<Integer, Integer> targetedFrequency,
            Map<Integer, Integer> trappedFrequency
    ) {
        AttackerPolicy policy = new AttackerPolicy();
        policy.setTotalBudget(attackBudget);

        double reconShare = 0.25;
        double compromiseShare = 0.3;
        double lateralShare = 0.3;
        double persistenceShare = 0.15;

        if (previousRoundMetrics != null) {
            if (previousRoundMetrics.getDeceptionEffectiveness() > 0.3) {
                reconShare += 0.08;
                lateralShare -= 0.05;
                compromiseShare -= 0.03;
            }
            if (previousRoundMetrics.getCompromiseImpact() > 0.45) {
                lateralShare += 0.07;
                persistenceShare += 0.05;
                reconShare -= 0.05;
            }
        }

        double total = reconShare + compromiseShare + lateralShare + persistenceShare;
        reconShare /= total;
        compromiseShare /= total;
        lateralShare /= total;
        persistenceShare /= total;

        policy.setReconnaissanceBudget(attackBudget * reconShare);
        policy.setCompromiseBudget(attackBudget * compromiseShare);
        policy.setLateralMovementBudget(attackBudget * lateralShare);
        policy.setPersistenceBudget(attackBudget * persistenceShare);

        double pressure = 0.6;
        if (previousRoundMetrics != null) {
            pressure += (previousRoundMetrics.getCompromiseImpact() - previousRoundMetrics.getDeceptionEffectiveness());
        }
        policy.setPressure(clamp(pressure));

        targetedFrequency.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .forEach(policy.getPreferredTargets()::add);

        trappedFrequency.entrySet().stream()
                .filter(entry -> entry.getValue() >= 2)
                .map(Map.Entry::getKey)
                .forEach(policy.getAvoidedTargets()::add);

        return policy;
    }

    private double applyHardening(AdvancedAttackGraph graph, DefenderPolicy policy, Map<Integer, Integer> targetedFrequency) {
        double budget = policy.getHardeningBudget();
        if (budget < properties.getHardeningNodeCost()) {
            return 0.0;
        }

        List<AdvancedNode> candidates = graph.getNodes().stream()
                .filter(node -> !node.isHoneypot())
                .sorted(Comparator
                        .comparingInt((AdvancedNode node) -> targetedFrequency.getOrDefault(node.getId(), 0)).reversed()
                        .thenComparing(AdvancedNode::getVulnerabilityScore, Comparator.reverseOrder()))
                .toList();

        int hardened = 0;
        for (AdvancedNode node : candidates) {
            if (budget + 1e-9 < properties.getHardeningNodeCost()) {
                break;
            }
            double before = node.getDefenseLevel();
            node.setDefenseLevel(before + properties.getHardeningDefenseBoost());
            if (node.getDefenseLevel() > before) {
                budget -= properties.getHardeningNodeCost();
                hardened++;
            }
        }

        return hardened * properties.getHardeningNodeCost();
    }

    private RecoveryOutcome recoverCompromisedNodes(AdvancedAttackGraph graph, double recoveryBudget, Random random) {
        if (recoveryBudget < properties.getRecoveryNodeCost()) {
            return new RecoveryOutcome(0, 0.0);
        }

        List<AdvancedNode> compromised = graph.getNodes().stream()
                .filter(AdvancedNode::isCompromised)
                .filter(node -> !node.isHoneypot())
                .sorted(Comparator.comparing(AdvancedNode::isDetected).reversed())
                .toList();

        if (compromised.isEmpty()) {
            return new RecoveryOutcome(0, 0.0);
        }

        int recovered = 0;
        double spent = 0.0;
        for (AdvancedNode node : compromised) {
            if (recoveryBudget + 1e-9 < properties.getRecoveryNodeCost()) {
                break;
            }

            double recoveryProbability = clamp(properties.getRecoveryProbabilityBoost() + node.getDefenseLevel() * 0.5);
            if (randomStrategySupport.chance(random, recoveryProbability)) {
                node.setCompromised(false);
                node.setDetected(true);
                recovered++;
            }

            recoveryBudget -= properties.getRecoveryNodeCost();
            spent += properties.getRecoveryNodeCost();
        }

        return new RecoveryOutcome(recovered, spent);
    }

    private RoundMetrics buildRoundMetrics(
            AdvancedAttackGraph graph,
            int round,
            int totalNodes,
            double hardeningCost,
            DeceptionResult deceptionResult,
            MovingTargetDefenseService.MtdActionResult mtdActionResult,
            AttackExecutionResult attackExecutionResult,
            RecoveryOutcome recoveryOutcome
    ) {
        int compromisedCount = Math.min(graph.countCompromisedRealNodes(), totalNodes);
        double compromiseRatio = totalNodes == 0 ? 0.0 : (double) compromisedCount / totalNodes;
        double connectivity = graph.computeLargestConnectedComponentRatio();
        double resilience = clamp((1.0 - compromiseRatio) * 0.7 + connectivity * 0.3);

        double detectionRate = attackExecutionResult.getAttackAttempts() == 0
                ? 0.0
                : (double) attackExecutionResult.getDetectedNodes() / attackExecutionResult.getAttackAttempts();

        double deceptionEffectiveness = clamp(
                deceptionResult.getDeceptionEffectiveness()
                        + deceptionResult.getHoneypotEngagements() * 0.08
        );

        double defenseCost = hardeningCost + deceptionResult.getDefenseCost() + mtdActionResult.getDefenseCost();
        double defenseEfficiency = defenseCost + recoveryOutcome.cost() <= 0.0
                ? 0.0
                : clamp((resilience + detectionRate + deceptionEffectiveness + mtdActionResult.getEffectiveness())
                / (defenseCost + recoveryOutcome.cost()));

        double attackEfficiency = attackExecutionResult.getAttackerBudgetSpent() <= 0.0
                ? 0.0
                : clamp(attackExecutionResult.getCompromiseImpact() / attackExecutionResult.getAttackerBudgetSpent());

        double attackerUtility = attackExecutionResult.getCompromiseImpact()
                + properties.getAttackerPathDepthBenefitWeight() * attackExecutionResult.getMaxAttackPathDepth()
                - attackExecutionResult.getAttackerBudgetSpent()
                - properties.getAttackerDeceptionPenaltyWeight() * deceptionResult.getAttackerBudgetWastedOnDecoys();

        double defenderUtility = resilience
                + detectionRate
                + deceptionEffectiveness
                - defenseCost
                - recoveryOutcome.cost();

        RoundMetrics metrics = new RoundMetrics();
        metrics.setRoundNumber(round);
        metrics.setTargetedNodes(attackExecutionResult.getTargetedNodes().size());
        metrics.setNewlyCompromisedNodes(attackExecutionResult.getNewlyCompromisedNodes());
        metrics.setCompromisedNodeCount(compromisedCount);
        metrics.setRecoveredNodes(recoveryOutcome.recoveredNodes());
        metrics.setDetectedNodes(attackExecutionResult.getDetectedNodes());
        metrics.setHoneypotEngagements(deceptionResult.getHoneypotEngagements());
        metrics.setDeceptionSuccessCount(deceptionResult.getDeceptionSuccessCount());
        metrics.setMaxAttackPathDepth(attackExecutionResult.getMaxAttackPathDepth());

        metrics.setCompromiseRatio(roundValue(compromiseRatio));
        metrics.setCompromiseImpact(roundValue(attackExecutionResult.getCompromiseImpact()));
        metrics.setAttackerBudgetSpent(roundValue(attackExecutionResult.getAttackerBudgetSpent()));
        metrics.setDefenderBudgetSpent(roundValue(defenseCost));
        metrics.setRecoveryCost(roundValue(recoveryOutcome.cost()));

        metrics.setResilienceScore(roundValue(resilience));
        metrics.setDefenseEfficiency(roundValue(defenseEfficiency));
        metrics.setAttackEfficiency(roundValue(attackEfficiency));
        metrics.setDeceptionEffectiveness(roundValue(deceptionEffectiveness));
        metrics.setMtdEffectiveness(roundValue(clamp(mtdActionResult.getEffectiveness())));
        metrics.setDetectionRate(roundValue(clamp(detectionRate)));

        metrics.setAttackerUtility(roundValue(attackerUtility));
        metrics.setDefenderUtility(roundValue(defenderUtility));

        return metrics;
    }

    private void mergeDeceptionEngagements(DeceptionResult deceptionResult, AttackExecutionResult attackExecutionResult) {
        deceptionResult.setHoneypotEngagements(
                deceptionResult.getHoneypotEngagements() + attackExecutionResult.getHoneypotEngagements()
        );
        deceptionResult.setDeceptionSuccessCount(
                deceptionResult.getDeceptionSuccessCount() + attackExecutionResult.getDeceptionSuccessCount()
        );
        deceptionResult.setAttackerBudgetWastedOnDecoys(
                deceptionResult.getAttackerBudgetWastedOnDecoys() + attackExecutionResult.getAttackerBudgetWastedOnDecoys()
        );
    }

    private void validateInputs(
            int numNodes,
            int numEdges,
            int attackBudget,
            int defenseBudget,
            int recoveryBudget,
            int rounds
    ) {
        if (numNodes < 2) {
            throw new BadRequestException("numNodes must be at least 2");
        }
        if (numEdges < 0 || numEdges > AdvancedAttackGraph.maxDirectedEdgeCount(numNodes)) {
            throw new BadRequestException("numEdges is invalid for numNodes");
        }
        if (attackBudget < 0 || defenseBudget < 0 || recoveryBudget < 0) {
            throw new BadRequestException("Budgets cannot be negative");
        }
        if (rounds < 1) {
            throw new BadRequestException("rounds must be at least 1");
        }
    }

    private void validateMetrics(AdvancedSimulationMetrics metrics, int totalNodes, int rounds) {
        if (metrics.getFinalCompromisedNodes() > totalNodes) {
            throw new IllegalStateException("finalCompromisedNodes exceeds total nodes");
        }
        if (metrics.getCompromisedNodeCountPerRound().stream().anyMatch(value -> value < 0 || value > totalNodes)) {
            throw new IllegalStateException("Compromise counts are inconsistent");
        }
        if (metrics.getRoundMetrics().size() != rounds || metrics.getRoundMetrics().stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalStateException("Round metrics collection is invalid");
        }
        if (metrics.getResilienceScore() < 0.0 || metrics.getResilienceScore() > 1.0) {
            throw new IllegalStateException("resilienceScore must be bounded in [0,1]");
        }
    }

    private double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        return Math.min(value, 1.0);
    }

    private double roundValue(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private record RecoveryOutcome(int recoveredNodes, double cost) {
    }
}
