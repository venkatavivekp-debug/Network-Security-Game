package backend.simulation.advanced;

import backend.model.AlgorithmType;

public class AdvancedNode {

    private final int id;
    private AssetType assetType;
    private double vulnerabilityScore;
    private double defenseLevel;
    private boolean compromised;
    private boolean detected;
    private boolean honeypot;
    private AlgorithmType encryptionModeImpact;

    public AdvancedNode(
            int id,
            AssetType assetType,
            double vulnerabilityScore,
            double defenseLevel,
            boolean compromised,
            boolean detected,
            boolean honeypot,
            AlgorithmType encryptionModeImpact
    ) {
        this.id = id;
        this.assetType = assetType;
        this.vulnerabilityScore = clamp(vulnerabilityScore);
        this.defenseLevel = clamp(defenseLevel);
        this.compromised = compromised;
        this.detected = detected;
        this.honeypot = honeypot;
        this.encryptionModeImpact = encryptionModeImpact;
    }

    public AdvancedNode(AdvancedNode other) {
        this(
                other.id,
                other.assetType,
                other.vulnerabilityScore,
                other.defenseLevel,
                other.compromised,
                other.detected,
                other.honeypot,
                other.encryptionModeImpact
        );
    }

    public int getId() {
        return id;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }

    public double getVulnerabilityScore() {
        return vulnerabilityScore;
    }

    public void setVulnerabilityScore(double vulnerabilityScore) {
        this.vulnerabilityScore = clamp(vulnerabilityScore);
    }

    public double getDefenseLevel() {
        return defenseLevel;
    }

    public void setDefenseLevel(double defenseLevel) {
        this.defenseLevel = clamp(defenseLevel);
    }

    public boolean isCompromised() {
        return compromised;
    }

    public void setCompromised(boolean compromised) {
        this.compromised = compromised;
    }

    public boolean isDetected() {
        return detected;
    }

    public void setDetected(boolean detected) {
        this.detected = detected;
    }

    public boolean isHoneypot() {
        return honeypot;
    }

    public void setHoneypot(boolean honeypot) {
        this.honeypot = honeypot;
    }

    public AlgorithmType getEncryptionModeImpact() {
        return encryptionModeImpact;
    }

    public void setEncryptionModeImpact(AlgorithmType encryptionModeImpact) {
        this.encryptionModeImpact = encryptionModeImpact;
    }

    private double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        return Math.min(value, 1.0);
    }
}
