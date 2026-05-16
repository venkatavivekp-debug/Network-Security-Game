package backend.service;

import backend.config.AttackSimulationProperties;
import backend.model.AlgorithmType;
import backend.model.Message;
import backend.simulation.AttackSimulationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class AttackSimulationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttackSimulationService.class);

    private final AttackSimulationProperties properties;

    public AttackSimulationService(AttackSimulationProperties properties) {
        this.properties = properties;
    }

    public AttackSimulationResult simulate(Message message) {
        long start = System.currentTimeMillis();

        AlgorithmType algorithmType = message.getAlgorithmType();
        double confidence = generateConfidence(algorithmType);

        boolean classificationSuccess = switch (algorithmType) {
            case NORMAL -> confidence >= properties.getNormalClassificationThreshold();
            case SHCS -> confidence >= properties.getShcsClassificationThreshold();
            case CPHS -> confidence >= properties.getCphsClassificationThreshold();
            case ADAPTIVE -> confidence >= adaptiveClassificationThreshold();
        };

        boolean selectiveJammingSuccess = classificationSuccess && confidence >= properties.getJammingThreshold();

        long estimatedTime = estimatedTime(algorithmType);
        long elapsed = Math.max(estimatedTime, System.currentTimeMillis() - start);

        String summary = buildSummary(algorithmType, classificationSuccess, selectiveJammingSuccess, confidence);

        LOGGER.info(
                "Attack simulation completed for message {} algorithm={} classificationSuccess={} jammingSuccess={} confidence={}",
                message.getId(), algorithmType, classificationSuccess, selectiveJammingSuccess, confidence
        );

        return new AttackSimulationResult(
                classificationSuccess,
                roundToThreeDecimals(confidence),
                selectiveJammingSuccess,
                elapsed,
                summary
        );
    }

    private double generateConfidence(AlgorithmType algorithmType) {
        return switch (algorithmType) {
            case NORMAL -> randomBetween(properties.getNormalConfidenceMin(), properties.getNormalConfidenceMax());
            case SHCS -> randomBetween(properties.getShcsConfidenceMin(), properties.getShcsConfidenceMax());
            case CPHS -> randomBetween(properties.getCphsConfidenceMin(), properties.getCphsConfidenceMax());
            case ADAPTIVE -> randomBetween(properties.getShcsConfidenceMin(), properties.getCphsConfidenceMax());
        };
    }

    private long estimatedTime(AlgorithmType algorithmType) {
        long baseTime = switch (algorithmType) {
            case NORMAL -> properties.getNormalBaseTimeMs();
            case SHCS -> properties.getShcsBaseTimeMs();
            case CPHS -> properties.getCphsBaseTimeMs();
            case ADAPTIVE -> Math.max(properties.getShcsBaseTimeMs(), properties.getCphsBaseTimeMs());
        };
        return baseTime + ThreadLocalRandom.current().nextLong(10, 55);
    }

    private double adaptiveClassificationThreshold() {
        return (properties.getShcsClassificationThreshold() + properties.getCphsClassificationThreshold()) / 2.0;
    }

    private String buildSummary(
            AlgorithmType algorithmType,
            boolean classificationSuccess,
            boolean selectiveJammingSuccess,
            double confidence
    ) {
        if (!classificationSuccess) {
            return "Attacker failed to classify packet under " + algorithmType + " (confidence=" + roundToThreeDecimals(confidence) + ")";
        }

        if (!selectiveJammingSuccess) {
            return "Attacker classified packet under " + algorithmType + " but failed selective jamming";
        }

        return "Attacker successfully classified and selectively jammed packet under " + algorithmType;
    }

    private double randomBetween(double min, double max) {
        if (max <= min) {
            return min;
        }
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    private double roundToThreeDecimals(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
