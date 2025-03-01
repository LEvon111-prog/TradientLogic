package org.example.data.model;

public class RiskAssessment {

    private double liquidityScore;
    private double volatilityScore;
    private double feeImpact;
    private double overallRiskScore;

    public RiskAssessment(double liquidityScore, double volatilityScore, double feeImpact, double overallRiskScore) {
        this.liquidityScore = liquidityScore;
        this.volatilityScore = volatilityScore;
        this.feeImpact = feeImpact;
        this.overallRiskScore = overallRiskScore;
    }
}
