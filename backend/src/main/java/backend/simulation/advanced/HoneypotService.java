package backend.simulation.advanced;

import backend.config.AdvancedSimulationProperties;
import backend.model.AlgorithmType;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class HoneypotService {

    private final AdvancedSimulationProperties properties;
    private final RandomStrategySupport randomSupport;

    public HoneypotService(AdvancedSimulationProperties properties, RandomStrategySupport randomSupport) {
        this.properties = properties;
        this.randomSupport = randomSupport;
    }

    public DeceptionResult deployHoneypots(
            AdvancedAttackGraph graph,
            DefenderPolicy defenderPolicy,
            AlgorithmType algorithmType,
            Random random
    ) {
        DeceptionResult result = new DeceptionResult();
        double budget = defenderPolicy.getDeceptionBudget();
        if (budget < properties.getDeceptionHoneypotCost()) {
            return result;
        }

        int realNodes = graph.countRealNodes();
        int maxHoneypots = Math.max(1, (int) Math.floor(realNodes * properties.getDeceptionMaxHoneypotFraction()));
        int existingHoneypots = graph.countHoneypots();
        int capacity = Math.max(0, maxHoneypots - existingHoneypots);
        int affordable = (int) Math.floor(budget / properties.getDeceptionHoneypotCost());
        int toCreate = Math.min(capacity, affordable);

        if (toCreate <= 0) {
            return result;
        }

        int currentMaxNodeId = graph.getNodes().stream().map(AdvancedNode::getId).max(Comparator.naturalOrder()).orElse(0);

        List<AdvancedNode> realTargets = graph.getNodes().stream()
                .filter(node -> !node.isHoneypot())
                .sorted(Comparator.comparingDouble(AdvancedNode::getVulnerabilityScore).reversed())
                .toList();

        for (int i = 0; i < toCreate; i++) {
            int newId = currentMaxNodeId + i + 1;
            double vulnerability = randomSupport.boundedDouble(random, 0.45, 0.9);
            double defense = randomSupport.boundedDouble(random, 0.55, 0.95);
            AdvancedNode honeypot = new AdvancedNode(
                    newId,
                    AssetType.HONEYPOT,
                    vulnerability,
                    defense,
                    false,
                    false,
                    true,
                    algorithmType
            );
            graph.addNode(honeypot);

            // Avoid fully connecting honeypots to the entire graph (that dominates topology/metrics).
            // Instead, place a small stochastic set of inbound/outbound lure edges.
            int maxLinksPerDirection = honeypotDegreeBudget(realTargets.size());
            placeHoneypotEdges(graph, honeypot, realTargets, random, maxLinksPerDirection, true);
            placeHoneypotEdges(graph, honeypot, realTargets, random, maxLinksPerDirection, false);
        }

        result.setHoneypotsInjected(toCreate);
        result.setDefenseCost(toCreate * properties.getDeceptionHoneypotCost());
        result.setDeceptionEffectiveness(clamp(0.05 * toCreate));
        return result;
    }

    private int honeypotDegreeBudget(int realNodeCount) {
        if (realNodeCount <= 1) {
            return 1;
        }
        // Log-scaled cap: ~3 at n=20, grows slowly.
        int cap = (int) Math.round(1.6 + Math.log(realNodeCount) / Math.log(1.8));
        return Math.max(2, Math.min(8, cap));
    }

    private void placeHoneypotEdges(
            AdvancedAttackGraph graph,
            AdvancedNode honeypot,
            List<AdvancedNode> realTargets,
            Random random,
            int maxLinks,
            boolean outboundFromHoneypot
    ) {
        List<AdvancedNode> shuffled = new ArrayList<>(realTargets);
        java.util.Collections.shuffle(shuffled, random);

        int placed = 0;
        for (AdvancedNode candidate : shuffled) {
            if (placed >= maxLinks) {
                break;
            }
            if (!randomSupport.chance(random, properties.getHoneypotLinkProbability())) {
                continue;
            }

            int honeypotId = honeypot.getId();
            int targetId = candidate.getId();
            int sourceId = outboundFromHoneypot ? honeypotId : targetId;
            int destId = outboundFromHoneypot ? targetId : honeypotId;

            if (graph.getEdge(sourceId, destId) != null) {
                continue;
            }

            graph.addEdge(sourceId, destId, 0.8, randomSupport.boundedDouble(random, 0.55, 0.92));
            placed++;
        }
    }

    private double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        return Math.min(value, 1.0);
    }
}
