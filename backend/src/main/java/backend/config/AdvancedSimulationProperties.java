package backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.advanced")
public class AdvancedSimulationProperties {

    private double baseCompromiseFloor;
    private double vulnerabilityWeight;
    private double exploitWeight;
    private double defenseMitigationWeight;
    private double pressureWeight;

    private double normalCompromiseMultiplier;
    private double shcsCompromiseMultiplier;
    private double cphsCompromiseMultiplier;

    private double baseDetectionProbability;
    private double defendedDetectionBoost;
    private double honeypotDetectionBoost;

    private double hardeningNodeCost;
    private double hardeningDefenseBoost;
    private double recoveryNodeCost;
    private double recoveryProbabilityBoost;

    private double mtdEdgeShuffleCost;
    private double mtdIdentityRotationCost;
    private double mtdEncryptionRotationCost;
    private double mtdDisablePathCost;

    private int mtdMaxEdgeSwaps;
    private int mtdMaxPathDisables;

    private double deceptionHoneypotCost;
    private double deceptionMaxHoneypotFraction;
    private double honeypotLinkProbability;

    private double persistenceDefenseDecay;

    private double impactServerWeight;
    private double impactIotWeight;
    private double impactGatewayWeight;
    private double impactDatabaseWeight;
    private double impactHoneypotWeight;

    private double attackerPathDepthBenefitWeight;
    private double attackerDeceptionPenaltyWeight;

    public double getBaseCompromiseFloor() {
        return baseCompromiseFloor;
    }

    public void setBaseCompromiseFloor(double baseCompromiseFloor) {
        this.baseCompromiseFloor = baseCompromiseFloor;
    }

    public double getVulnerabilityWeight() {
        return vulnerabilityWeight;
    }

    public void setVulnerabilityWeight(double vulnerabilityWeight) {
        this.vulnerabilityWeight = vulnerabilityWeight;
    }

    public double getExploitWeight() {
        return exploitWeight;
    }

    public void setExploitWeight(double exploitWeight) {
        this.exploitWeight = exploitWeight;
    }

    public double getDefenseMitigationWeight() {
        return defenseMitigationWeight;
    }

    public void setDefenseMitigationWeight(double defenseMitigationWeight) {
        this.defenseMitigationWeight = defenseMitigationWeight;
    }

    public double getPressureWeight() {
        return pressureWeight;
    }

    public void setPressureWeight(double pressureWeight) {
        this.pressureWeight = pressureWeight;
    }

    public double getNormalCompromiseMultiplier() {
        return normalCompromiseMultiplier;
    }

    public void setNormalCompromiseMultiplier(double normalCompromiseMultiplier) {
        this.normalCompromiseMultiplier = normalCompromiseMultiplier;
    }

    public double getShcsCompromiseMultiplier() {
        return shcsCompromiseMultiplier;
    }

    public void setShcsCompromiseMultiplier(double shcsCompromiseMultiplier) {
        this.shcsCompromiseMultiplier = shcsCompromiseMultiplier;
    }

    public double getCphsCompromiseMultiplier() {
        return cphsCompromiseMultiplier;
    }

    public void setCphsCompromiseMultiplier(double cphsCompromiseMultiplier) {
        this.cphsCompromiseMultiplier = cphsCompromiseMultiplier;
    }

    public double getBaseDetectionProbability() {
        return baseDetectionProbability;
    }

    public void setBaseDetectionProbability(double baseDetectionProbability) {
        this.baseDetectionProbability = baseDetectionProbability;
    }

    public double getDefendedDetectionBoost() {
        return defendedDetectionBoost;
    }

    public void setDefendedDetectionBoost(double defendedDetectionBoost) {
        this.defendedDetectionBoost = defendedDetectionBoost;
    }

    public double getHoneypotDetectionBoost() {
        return honeypotDetectionBoost;
    }

    public void setHoneypotDetectionBoost(double honeypotDetectionBoost) {
        this.honeypotDetectionBoost = honeypotDetectionBoost;
    }

    public double getHardeningNodeCost() {
        return hardeningNodeCost;
    }

    public void setHardeningNodeCost(double hardeningNodeCost) {
        this.hardeningNodeCost = hardeningNodeCost;
    }

    public double getHardeningDefenseBoost() {
        return hardeningDefenseBoost;
    }

    public void setHardeningDefenseBoost(double hardeningDefenseBoost) {
        this.hardeningDefenseBoost = hardeningDefenseBoost;
    }

    public double getRecoveryNodeCost() {
        return recoveryNodeCost;
    }

    public void setRecoveryNodeCost(double recoveryNodeCost) {
        this.recoveryNodeCost = recoveryNodeCost;
    }

    public double getRecoveryProbabilityBoost() {
        return recoveryProbabilityBoost;
    }

    public void setRecoveryProbabilityBoost(double recoveryProbabilityBoost) {
        this.recoveryProbabilityBoost = recoveryProbabilityBoost;
    }

    public double getMtdEdgeShuffleCost() {
        return mtdEdgeShuffleCost;
    }

    public void setMtdEdgeShuffleCost(double mtdEdgeShuffleCost) {
        this.mtdEdgeShuffleCost = mtdEdgeShuffleCost;
    }

    public double getMtdIdentityRotationCost() {
        return mtdIdentityRotationCost;
    }

    public void setMtdIdentityRotationCost(double mtdIdentityRotationCost) {
        this.mtdIdentityRotationCost = mtdIdentityRotationCost;
    }

    public double getMtdEncryptionRotationCost() {
        return mtdEncryptionRotationCost;
    }

    public void setMtdEncryptionRotationCost(double mtdEncryptionRotationCost) {
        this.mtdEncryptionRotationCost = mtdEncryptionRotationCost;
    }

    public double getMtdDisablePathCost() {
        return mtdDisablePathCost;
    }

    public void setMtdDisablePathCost(double mtdDisablePathCost) {
        this.mtdDisablePathCost = mtdDisablePathCost;
    }

    public int getMtdMaxEdgeSwaps() {
        return mtdMaxEdgeSwaps;
    }

    public void setMtdMaxEdgeSwaps(int mtdMaxEdgeSwaps) {
        this.mtdMaxEdgeSwaps = mtdMaxEdgeSwaps;
    }

    public int getMtdMaxPathDisables() {
        return mtdMaxPathDisables;
    }

    public void setMtdMaxPathDisables(int mtdMaxPathDisables) {
        this.mtdMaxPathDisables = mtdMaxPathDisables;
    }

    public double getDeceptionHoneypotCost() {
        return deceptionHoneypotCost;
    }

    public void setDeceptionHoneypotCost(double deceptionHoneypotCost) {
        this.deceptionHoneypotCost = deceptionHoneypotCost;
    }

    public double getDeceptionMaxHoneypotFraction() {
        return deceptionMaxHoneypotFraction;
    }

    public void setDeceptionMaxHoneypotFraction(double deceptionMaxHoneypotFraction) {
        this.deceptionMaxHoneypotFraction = deceptionMaxHoneypotFraction;
    }

    public double getHoneypotLinkProbability() {
        return honeypotLinkProbability;
    }

    public void setHoneypotLinkProbability(double honeypotLinkProbability) {
        this.honeypotLinkProbability = honeypotLinkProbability;
    }

    public double getPersistenceDefenseDecay() {
        return persistenceDefenseDecay;
    }

    public void setPersistenceDefenseDecay(double persistenceDefenseDecay) {
        this.persistenceDefenseDecay = persistenceDefenseDecay;
    }

    public double getImpactServerWeight() {
        return impactServerWeight;
    }

    public void setImpactServerWeight(double impactServerWeight) {
        this.impactServerWeight = impactServerWeight;
    }

    public double getImpactIotWeight() {
        return impactIotWeight;
    }

    public void setImpactIotWeight(double impactIotWeight) {
        this.impactIotWeight = impactIotWeight;
    }

    public double getImpactGatewayWeight() {
        return impactGatewayWeight;
    }

    public void setImpactGatewayWeight(double impactGatewayWeight) {
        this.impactGatewayWeight = impactGatewayWeight;
    }

    public double getImpactDatabaseWeight() {
        return impactDatabaseWeight;
    }

    public void setImpactDatabaseWeight(double impactDatabaseWeight) {
        this.impactDatabaseWeight = impactDatabaseWeight;
    }

    public double getImpactHoneypotWeight() {
        return impactHoneypotWeight;
    }

    public void setImpactHoneypotWeight(double impactHoneypotWeight) {
        this.impactHoneypotWeight = impactHoneypotWeight;
    }

    public double getAttackerPathDepthBenefitWeight() {
        return attackerPathDepthBenefitWeight;
    }

    public void setAttackerPathDepthBenefitWeight(double attackerPathDepthBenefitWeight) {
        this.attackerPathDepthBenefitWeight = attackerPathDepthBenefitWeight;
    }

    public double getAttackerDeceptionPenaltyWeight() {
        return attackerDeceptionPenaltyWeight;
    }

    public void setAttackerDeceptionPenaltyWeight(double attackerDeceptionPenaltyWeight) {
        this.attackerDeceptionPenaltyWeight = attackerDeceptionPenaltyWeight;
    }
}
