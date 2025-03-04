package org.example.data.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced RiskAssessment model that captures comprehensive risk metrics
 * for arbitrage opportunities, including early warning indicators,
 * market regime information, and predictive analytics data.
 */
public class RiskAssessment {

    // Basic risk metrics
    private double liquidityScore;
    private double volatilityScore;
    private double feeImpact;
    private double overallRiskScore;
    
    // Enhanced risk metrics
    private double marketDepthScore;
    private double executionSpeedRisk;
    private double slippageRisk;
    private double marketRegimeScore;
    private double sentimentScore;
    private double anomalyScore;
    private double correlationScore;
    
    // Early warning indicators
    private boolean earlyWarningTriggered;
    private Map<String, Double> warningThresholds;
    private Map<String, Double> warningIndicators;
    
    // Predictive analytics
    private double predictedRiskScore;
    private double predictionConfidence;
    
    // Timestamp for risk assessment
    private Date assessmentTime;
    
    /**
     * Basic constructor for backward compatibility
     */
    public RiskAssessment(double liquidityScore, double volatilityScore, double feeImpact, double overallRiskScore) {
        this.liquidityScore = liquidityScore;
        this.volatilityScore = volatilityScore;
        this.feeImpact = feeImpact;
        this.overallRiskScore = overallRiskScore;
        this.assessmentTime = new Date();
        this.warningThresholds = new HashMap<>();
        this.warningIndicators = new HashMap<>();
    }
    
    /**
     * Enhanced constructor with additional risk metrics
     */
    public RiskAssessment(double liquidityScore, double volatilityScore, double feeImpact, 
                          double marketDepthScore, double executionSpeedRisk, double slippageRisk,
                          double marketRegimeScore, double sentimentScore, double anomalyScore,
                          double correlationScore, double overallRiskScore) {
        this(liquidityScore, volatilityScore, feeImpact, overallRiskScore);
        this.marketDepthScore = marketDepthScore;
        this.executionSpeedRisk = executionSpeedRisk;
        this.slippageRisk = slippageRisk;
        this.marketRegimeScore = marketRegimeScore;
        this.sentimentScore = sentimentScore;
        this.anomalyScore = anomalyScore;
        this.correlationScore = correlationScore;
    }
    
    /**
     * Sets an early warning indicator and checks if it exceeds the threshold
     * 
     * @param indicatorName The name of the warning indicator
     * @param value The current value of the indicator
     * @param threshold The threshold value that triggers a warning
     * @return true if the warning threshold is exceeded
     */
    public boolean setWarningIndicator(String indicatorName, double value, double threshold) {
        warningIndicators.put(indicatorName, value);
        warningThresholds.put(indicatorName, threshold);
        
        boolean thresholdExceeded = value > threshold;
        if (thresholdExceeded) {
            earlyWarningTriggered = true;
        }
        
        return thresholdExceeded;
    }
    
    /**
     * Sets predictive risk analytics data
     * 
     * @param predictedRisk The predicted risk score
     * @param confidence The confidence level of the prediction (0-1)
     */
    public void setPredictiveAnalytics(double predictedRisk, double confidence) {
        this.predictedRiskScore = predictedRisk;
        this.predictionConfidence = confidence;
    }
    
    // Getters and setters
    
    public double getLiquidityScore() {
        return liquidityScore;
    }
    
    public double getVolatilityScore() {
        return volatilityScore;
    }
    
    public double getFeeImpact() {
        return feeImpact;
    }
    
    public double getOverallRiskScore() {
        return overallRiskScore;
    }
    
    public double getMarketDepthScore() {
        return marketDepthScore;
    }
    
    public double getExecutionSpeedRisk() {
        return executionSpeedRisk;
    }
    
    public double getSlippageRisk() {
        return slippageRisk;
    }
    
    public double getMarketRegimeScore() {
        return marketRegimeScore;
    }
    
    public double getSentimentScore() {
        return sentimentScore;
    }
    
    public double getAnomalyScore() {
        return anomalyScore;
    }
    
    public double getCorrelationScore() {
        return correlationScore;
    }
    
    public boolean isEarlyWarningTriggered() {
        return earlyWarningTriggered;
    }
    
    public Map<String, Double> getWarningIndicators() {
        return warningIndicators;
    }
    
    public Map<String, Double> getWarningThresholds() {
        return warningThresholds;
    }
    
    public double getPredictedRiskScore() {
        return predictedRiskScore;
    }
    
    public double getPredictionConfidence() {
        return predictionConfidence;
    }
    
    public Date getAssessmentTime() {
        return assessmentTime;
    }
    
    public void setOverallRiskScore(double overallRiskScore) {
        this.overallRiskScore = overallRiskScore;
    }
}
