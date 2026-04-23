package backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.attack")
public class AttackSimulationProperties {

    private double normalConfidenceMin;
    private double normalConfidenceMax;
    private double normalClassificationThreshold;
    private double shcsConfidenceMin;
    private double shcsConfidenceMax;
    private double shcsClassificationThreshold;
    private double cphsConfidenceMin;
    private double cphsConfidenceMax;
    private double cphsClassificationThreshold;
    private double jammingThreshold;
    private long normalBaseTimeMs;
    private long shcsBaseTimeMs;
    private long cphsBaseTimeMs;

    public double getNormalConfidenceMin() {
        return normalConfidenceMin;
    }

    public void setNormalConfidenceMin(double normalConfidenceMin) {
        this.normalConfidenceMin = normalConfidenceMin;
    }

    public double getNormalConfidenceMax() {
        return normalConfidenceMax;
    }

    public void setNormalConfidenceMax(double normalConfidenceMax) {
        this.normalConfidenceMax = normalConfidenceMax;
    }

    public double getNormalClassificationThreshold() {
        return normalClassificationThreshold;
    }

    public void setNormalClassificationThreshold(double normalClassificationThreshold) {
        this.normalClassificationThreshold = normalClassificationThreshold;
    }

    public double getShcsConfidenceMin() {
        return shcsConfidenceMin;
    }

    public void setShcsConfidenceMin(double shcsConfidenceMin) {
        this.shcsConfidenceMin = shcsConfidenceMin;
    }

    public double getShcsConfidenceMax() {
        return shcsConfidenceMax;
    }

    public void setShcsConfidenceMax(double shcsConfidenceMax) {
        this.shcsConfidenceMax = shcsConfidenceMax;
    }

    public double getShcsClassificationThreshold() {
        return shcsClassificationThreshold;
    }

    public void setShcsClassificationThreshold(double shcsClassificationThreshold) {
        this.shcsClassificationThreshold = shcsClassificationThreshold;
    }

    public double getCphsConfidenceMin() {
        return cphsConfidenceMin;
    }

    public void setCphsConfidenceMin(double cphsConfidenceMin) {
        this.cphsConfidenceMin = cphsConfidenceMin;
    }

    public double getCphsConfidenceMax() {
        return cphsConfidenceMax;
    }

    public void setCphsConfidenceMax(double cphsConfidenceMax) {
        this.cphsConfidenceMax = cphsConfidenceMax;
    }

    public double getCphsClassificationThreshold() {
        return cphsClassificationThreshold;
    }

    public void setCphsClassificationThreshold(double cphsClassificationThreshold) {
        this.cphsClassificationThreshold = cphsClassificationThreshold;
    }

    public double getJammingThreshold() {
        return jammingThreshold;
    }

    public void setJammingThreshold(double jammingThreshold) {
        this.jammingThreshold = jammingThreshold;
    }

    public long getNormalBaseTimeMs() {
        return normalBaseTimeMs;
    }

    public void setNormalBaseTimeMs(long normalBaseTimeMs) {
        this.normalBaseTimeMs = normalBaseTimeMs;
    }

    public long getShcsBaseTimeMs() {
        return shcsBaseTimeMs;
    }

    public void setShcsBaseTimeMs(long shcsBaseTimeMs) {
        this.shcsBaseTimeMs = shcsBaseTimeMs;
    }

    public long getCphsBaseTimeMs() {
        return cphsBaseTimeMs;
    }

    public void setCphsBaseTimeMs(long cphsBaseTimeMs) {
        this.cphsBaseTimeMs = cphsBaseTimeMs;
    }
}
