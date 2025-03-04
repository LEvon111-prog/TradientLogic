package org.example.domain.risk;

import org.example.data.model.ArbitrageOpportunity;
import org.example.data.model.RiskAssessment;
import org.example.data.model.Ticker;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RiskCalculator {

    // Magic numbers extracted as configurable constants
    private static final double VOLUME_NORMALIZATION_SHORT = 1000.0;
    private static final double VOLUME_NORMALIZATION_LONG = 2000.0;
    private static final double SPREAD_MULTIPLIER = 100.0;
    private static final double DEFAULT_SENTIMENT_SCORE = 0.65;
    private static final double DEFAULT_PRICE_STABILITY = 0.7;
    private static final double PREDICTIVE_RISK_FACTOR = 0.95;
    private static final double PREDICTIVE_CONFIDENCE = 0.75;

    // Minimum profit required to consider an opportunity acceptable (as a ratio)
    private double minProfitThreshold;

    // Risk thresholds for early warning system
    private Map<String, Double> warningThresholds;

    // Weights for risk factors
    private double liquidityWeight = 0.3;
    private double volatilityWeight = 0.3;
    private double feeWeight = 0.4;
    private double marketDepthWeight = 0.2;
    private double executionSpeedWeight = 0.2;
    private double slippageWeight = 0.2;
    private double marketRegimeWeight = 0.1;
    private double sentimentWeight = 0.1;
    private double anomalyWeight = 0.2;
    private double correlationWeight = 0.1;

    /**
     * Constructor for RiskCalculator.
     *
     * @param minProfitThreshold The minimum profit threshold as a decimal (e.g., 0.005 for 0.5%)
     */
    public RiskCalculator(double minProfitThreshold) {
        this.minProfitThreshold = minProfitThreshold;
        initializeWarningThresholds();
    }

    /**
     * Initialize default warning thresholds for early warning system.
     */
    private void initializeWarningThresholds() {
        warningThresholds = new HashMap<>();
        warningThresholds.put("liquidityMinimum", 0.3);
        warningThresholds.put("volatilityMaximum", 0.7);
        warningThresholds.put("slippageMaximum", 0.05);
        warningThresholds.put("marketDepthMinimum", 0.4);
        warningThresholds.put("anomalyThreshold", 0.8);
    }

    /**
     * Calculates comprehensive risk factors for an arbitrage opportunity.
     *
     * @param buyTicker Ticker data for the buy exchange (must not be null)
     * @param sellTicker Ticker data for the sell exchange (must not be null)
     * @param buyFees Trading fees on the buy exchange (as decimal)
     * @param sellFees Trading fees on the sell exchange (as decimal)
     * @return A RiskAssessment object with calculated risk factors
     */
    public RiskAssessment calculateRisk(Ticker buyTicker, Ticker sellTicker, double buyFees, double sellFees) {
        Objects.requireNonNull(buyTicker, "buyTicker must not be null");
        Objects.requireNonNull(sellTicker, "sellTicker must not be null");

        double feeImpact = calculateFeeImpact(buyFees, sellFees);
        double liquidityScore = calculateLiquidityScore(buyTicker, sellTicker);
        double volatilityScore = calculateVolatilityScore(buyTicker, sellTicker);
        double marketDepthScore = calculateMarketDepthScore(buyTicker, sellTicker);
        double executionSpeedRisk = calculateExecutionSpeedRisk(buyTicker, sellTicker);
        double slippageRisk = calculateSlippageRisk(buyTicker, sellTicker);
        double marketRegimeScore = calculateMarketRegimeScore(buyTicker, sellTicker);
        double sentimentScore = calculateSentimentScore(buyTicker, sellTicker);
        double anomalyScore = calculateAnomalyScore(buyTicker, sellTicker);
        double correlationScore = calculateCorrelationScore(buyTicker, sellTicker);

        double overallRiskScore = calculateEnhancedOverallRiskScore(
                liquidityScore, volatilityScore, feeImpact, marketDepthScore,
                executionSpeedRisk, slippageRisk, marketRegimeScore,
                sentimentScore, anomalyScore, correlationScore);


        RiskAssessment assessment = new RiskAssessment(
                liquidityScore, volatilityScore, feeImpact,
                marketDepthScore, executionSpeedRisk, slippageRisk,
                marketRegimeScore, sentimentScore, anomalyScore,
                correlationScore, overallRiskScore);

        checkEarlyWarningIndicators(assessment);
        setPredictiveAnalytics(assessment, buyTicker, sellTicker);

        return assessment;
    }

    private double calculateLiquidityScore(Ticker buyTicker, Ticker sellTicker) {
        double buyVolume = buyTicker.getVolume();
        double sellVolume = sellTicker.getVolume();
        double averageVolume = (buyVolume + sellVolume) / 2.0;
        return Math.min(averageVolume / VOLUME_NORMALIZATION_SHORT, 1.0);
    }

    private double calculateVolatilityScore(Ticker buyTicker, Ticker sellTicker) {
        double buySpread = (buyTicker.getAskPrice() - buyTicker.getBidPrice()) / buyTicker.getLastPrice();
        double sellSpread = (sellTicker.getAskPrice() - sellTicker.getBidPrice()) / sellTicker.getLastPrice();
        double averageSpread = (buySpread + sellSpread) / 2.0;
        double spreadFactor = Math.min(averageSpread * SPREAD_MULTIPLIER, 1.0);
        return 1.0 - spreadFactor;
    }

    private double calculateFeeImpact(double buyFees, double sellFees) {
        double totalFees = buyFees + sellFees;
        double feesPercent = totalFees * 100;
        double feeScore = Math.max(0.0, 1.0 - (feesPercent / 1.0));
        return feeScore;
    }

    private double calculateMarketDepthScore(Ticker buyTicker, Ticker sellTicker) {
        double buyVolume = buyTicker.getVolume();
        double sellVolume = sellTicker.getVolume();
        double volumeRatio = Math.min(buyVolume, sellVolume) / Math.max(buyVolume, sellVolume);
        double absoluteVolumeFactor = Math.min((buyVolume + sellVolume) / VOLUME_NORMALIZATION_LONG, 1.0);
        return (volumeRatio * 0.5) + (absoluteVolumeFactor * 0.5);
    }

    private double calculateExecutionSpeedRisk(Ticker buyTicker, Ticker sellTicker) {
        double volumeFactor = Math.min((buyTicker.getVolume() + sellTicker.getVolume()) / VOLUME_NORMALIZATION_LONG, 1.0);
        double buySpread = (buyTicker.getAskPrice() - buyTicker.getBidPrice()) / buyTicker.getLastPrice();
        double sellSpread = (sellTicker.getAskPrice() - sellTicker.getBidPrice()) / sellTicker.getLastPrice();
        double spreadFactor = 1.0 - Math.min(((buySpread + sellSpread) / 2.0) * SPREAD_MULTIPLIER, 1.0);
        return (volumeFactor * 0.7) + (spreadFactor * 0.3);
    }

    private double calculateSlippageRisk(Ticker buyTicker, Ticker sellTicker) {
        double volumeFactor = Math.min((buyTicker.getVolume() + sellTicker.getVolume()) / VOLUME_NORMALIZATION_LONG, 1.0);
        double buySpread = (buyTicker.getAskPrice() - buyTicker.getBidPrice()) / buyTicker.getLastPrice();
        double sellSpread = (sellTicker.getAskPrice() - sellTicker.getBidPrice()) / sellTicker.getLastPrice();
        double spreadFactor = 1.0 - Math.min(((buySpread + sellSpread) / 2.0) * SPREAD_MULTIPLIER, 1.0);
        return (volumeFactor * 0.6) + (spreadFactor * 0.4);
    }

    private double calculateMarketRegimeScore(Ticker buyTicker, Ticker sellTicker) {
        // TODO: Replace placeholder implementation with real market regime analysis
        double volatilityScore = calculateVolatilityScore(buyTicker, sellTicker);
        return (DEFAULT_PRICE_STABILITY * 0.5) + (volatilityScore * 0.5);
    }

    private double calculateSentimentScore(Ticker buyTicker, Ticker sellTicker) {
        // TODO: Replace placeholder implementation with real market sentiment analysis
        return DEFAULT_SENTIMENT_SCORE;
    }

    private double calculateAnomalyScore(Ticker buyTicker, Ticker sellTicker) {
        double priceDifference = Math.abs(buyTicker.getLastPrice() - sellTicker.getLastPrice());
        double relativeDifference = priceDifference / ((buyTicker.getLastPrice() + sellTicker.getLastPrice()) / 2.0);
        double anomalyFactor = Math.min(relativeDifference * 10.0, 1.0);
        return 1.0 - anomalyFactor;
    }

    private double calculateCorrelationScore(Ticker buyTicker, Ticker sellTicker) {
        double priceDifference = Math.abs(buyTicker.getLastPrice() - sellTicker.getLastPrice());
        double priceAverage = (buyTicker.getLastPrice() + sellTicker.getLastPrice()) / 2.0;
        double relativeDifference = priceDifference / priceAverage;
        return Math.max(0.0, 1.0 - (relativeDifference * 10.0));
    }

    private double calculateEnhancedOverallRiskScore(double liquidityScore, double volatilityScore, double feeImpact,
                                                     double marketDepthScore, double executionSpeedRisk, double slippageRisk,
                                                     double marketRegimeScore, double sentimentScore, double anomalyScore,
                                                     double correlationScore) {
        double totalWeight = liquidityWeight + volatilityWeight + feeWeight + marketDepthWeight +
                executionSpeedWeight + slippageWeight + marketRegimeWeight +
                sentimentWeight + anomalyWeight + correlationWeight;
        return (liquidityScore * liquidityWeight / totalWeight) +
                (volatilityScore * volatilityWeight / totalWeight) +
                (feeImpact * feeWeight / totalWeight) +
                (marketDepthScore * marketDepthWeight / totalWeight) +
                (executionSpeedRisk * executionSpeedWeight / totalWeight) +
                (slippageRisk * slippageWeight / totalWeight) +
                (marketRegimeScore * marketRegimeWeight / totalWeight) +
                (sentimentScore * sentimentWeight / totalWeight) +
                (anomalyScore * anomalyWeight / totalWeight) +
                (correlationScore * correlationWeight / totalWeight);
    }

    private void checkEarlyWarningIndicators(RiskAssessment assessment) {
        assessment.setWarningIndicator("liquidity", assessment.getLiquidityScore(), warningThresholds.get("liquidityMinimum"));
        assessment.setWarningIndicator("volatility", 1.0 - assessment.getVolatilityScore(), warningThresholds.get("volatilityMaximum"));
        assessment.setWarningIndicator("slippage", 1.0 - assessment.getSlippageRisk(), warningThresholds.get("slippageMaximum"));
        assessment.setWarningIndicator("marketDepth", assessment.getMarketDepthScore(), warningThresholds.get("marketDepthMinimum"));
        assessment.setWarningIndicator("anomaly", 1.0 - assessment.getAnomalyScore(), warningThresholds.get("anomalyThreshold"));
    }

    /**
     * Applies predictive analytics to the risk assessment using the given ticker data.
     *
     * @param assessment The risk assessment to update
     * @param buyTicker The buy exchange ticker data
     * @param sellTicker The sell exchange ticker data
     */
    private void setPredictiveAnalytics(RiskAssessment assessment, Ticker buyTicker, Ticker sellTicker) {
        double currentRisk = assessment.getOverallRiskScore();
        double predictedRisk = currentRisk * PREDICTIVE_RISK_FACTOR;
        assessment.setPredictiveAnalytics(predictedRisk, PREDICTIVE_CONFIDENCE);
    }

    public double getMinProfitThreshold() {
        return minProfitThreshold;
    }

    public boolean isOpportunityAcceptable(ArbitrageOpportunity opportunity) {
        if (opportunity == null || opportunity.getPotentialProfit() < minProfitThreshold) {
            return false;
        }

        RiskAssessment assessment = opportunity.getRiskAssessment();
        if (assessment != null) {
            if (assessment.isEarlyWarningTriggered()) {
                return opportunity.getPotentialProfit() > (minProfitThreshold * 1.5);
            }
            if (assessment.getOverallRiskScore() < 0.4) {
                return false;
            }
        }
        return true;
    }

    public RiskAssessment assessRisk(ArbitrageOpportunity opportunity) {
        // Basic risk assessment using placeholder values; refine as needed.
        double liquidityScore = 0.8;
        double volatilityScore = 0.7;
        double feeImpact = 0.9;
        double marketDepthScore = 0.75;
        double executionSpeedRisk = 0.8;
        double slippageRisk = 0.7;
        double marketRegimeScore = 0.6;
        double sentimentScore = 0.65;
        double anomalyScore = 0.9;
        double correlationScore = 0.8;

        double overallRiskScore = calculateEnhancedOverallRiskScore(
                liquidityScore, volatilityScore, feeImpact, marketDepthScore,
                executionSpeedRisk, slippageRisk, marketRegimeScore,
                sentimentScore, anomalyScore, correlationScore);

        RiskAssessment assessment = new RiskAssessment(
                liquidityScore, volatilityScore, feeImpact,
                marketDepthScore, executionSpeedRisk, slippageRisk,
                marketRegimeScore, sentimentScore, anomalyScore,
                correlationScore, overallRiskScore);

        checkEarlyWarningIndicators(assessment);
        assessment.setPredictiveAnalytics(overallRiskScore * PREDICTIVE_RISK_FACTOR, PREDICTIVE_CONFIDENCE);
        return assessment;
    }

    public void updateRiskWeights(String marketCondition) {
        switch (marketCondition.toLowerCase()) {
            case "volatile":
                volatilityWeight = 0.5;
                liquidityWeight = 0.4;
                feeWeight = 0.1;
                anomalyWeight = 0.3;
                break;
            case "stable":
                volatilityWeight = 0.2;
                liquidityWeight = 0.3;
                feeWeight = 0.5;
                anomalyWeight = 0.1;
                break;
            case "illiquid":
                liquidityWeight = 0.6;
                volatilityWeight = 0.2;
                feeWeight = 0.2;
                marketDepthWeight = 0.4;
                slippageWeight = 0.3;
                break;
            default:
                liquidityWeight = 0.3;
                volatilityWeight = 0.3;
                feeWeight = 0.4;
                marketDepthWeight = 0.2;
                executionSpeedWeight = 0.2;
                slippageWeight = 0.2;
                marketRegimeWeight = 0.1;
                sentimentWeight = 0.1;
                anomalyWeight = 0.2;
                correlationWeight = 0.1;
        }
    }
}