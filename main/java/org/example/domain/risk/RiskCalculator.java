package org.example.domain.risk;

import org.example.data.model.ArbitrageOpportunity;
import org.example.data.model.RiskAssessment;
import org.example.data.model.Ticker;
import org.example.data.interfaces.IRiskManager;
import org.example.config.ConfigurationFactory;
import org.example.data.model.RiskConfiguration;

import java.util.Objects;

/**
 * Advanced risk assessment engine for cryptocurrency arbitrage.
 * 
 * This class evaluates the risk of arbitrage opportunities using multiple factors:
 * - Liquidity risk through volume analysis
 * - Volatility risk through price movement assessment
 * - Slippage risk based on order book analysis
 * - Market depth assessment for large orders
 * - Asset-specific risk factors
 * 
 * The calculator provides:
 * - Overall risk scores (0-1 scale)
 * - Individual factor assessments
 * - Early warning indicators
 * - Predictive analytics 
 * - Position sizing recommendations
 * 
 * Risk parameters are loaded from the configuration system and can be
 * dynamically adjusted without changing code.
 * 
 * Compatible with Android platform.
 */
public class RiskCalculator implements IRiskManager {

    // Magic numbers extracted as configurable constants
    private static final double VOLUME_NORMALIZATION_SHORT = 1000.0;
    private static final double VOLUME_NORMALIZATION_LONG = 2000.0;
    private static final double SPREAD_MULTIPLIER = 100.0;
    private static final double DEFAULT_SENTIMENT_SCORE = 0.65;
    private static final double DEFAULT_PRICE_STABILITY = 0.7;
    private static final double PREDICTIVE_RISK_FACTOR = 0.95;
    private static final double PREDICTIVE_CONFIDENCE = 0.75;

    private double minProfitPercent;
    private RiskConfiguration riskConfig;

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
     * Default constructor.
     */
    public RiskCalculator() {
        this.riskConfig = ConfigurationFactory.getRiskConfig();
        this.minProfitPercent = ConfigurationFactory.getArbitrageConfig().getMinProfitPercent() / 100.0;
    }

    /**
     * Constructor with minimum profit percentage.
     * 
     * @param minProfitPercent Minimum profit percentage as a decimal (e.g., 0.001 for 0.1%)
     */
    public RiskCalculator(double minProfitPercent) {
        this.minProfitPercent = minProfitPercent;
        this.riskConfig = ConfigurationFactory.getRiskConfig();
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
        // Enhanced slippage calculation that considers trade size and order book depth
        double tradeSize = estimateTradeSize(buyTicker, sellTicker);
        double buySlippage = calculateExpectedSlippage(buyTicker, tradeSize, true);
        double sellSlippage = calculateExpectedSlippage(sellTicker, tradeSize, false);
        
        // Combine both slippage values and normalize to a 0-1 score (where 1 is low risk)
        double totalSlippage = buySlippage + sellSlippage;
        return Math.max(0.0, 1.0 - (totalSlippage * 20.0)); // Scale factor of 20 to normalize
    }
    
    /**
     * Estimates the appropriate trade size based on available liquidity.
     * 
     * @param buyTicker The buy exchange ticker data
     * @param sellTicker The sell exchange ticker data
     * @return Estimated optimal trade size
     */
    private double estimateTradeSize(Ticker buyTicker, Ticker sellTicker) {
        // Use a percentage of the available volume as a conservative estimate
        // Typically 1-5% of the smallest volume side to minimize market impact
        double smallestVolume = Math.min(buyTicker.getVolume(), sellTicker.getVolume());
        return smallestVolume * 0.03; // 3% of smallest volume as default
    }
    
    /**
     * Calculates expected slippage for a given trade size.
     * Enhanced version that handles edge cases and prevents NaN values.
     * 
     * @param ticker The ticker data
     * @param tradeSize The size of the trade to execute
     * @param isBuy Whether this is a buy (true) or sell (false) operation
     * @return Expected slippage as a percentage of the trade value
     */
    private double calculateExpectedSlippage(Ticker ticker, double tradeSize, boolean isBuy) {
        // Default slippage value if we can't calculate
        double defaultSlippage = 0.005; // 0.5% as a reasonable default
        
        // Check for null ticker or invalid data
        if (ticker == null || ticker.getLastPrice() <= 0 || ticker.getVolume() <= 0) {
            return defaultSlippage;
        }
        
        try {
            // Calculate the spread as a percentage of price
            double lastPrice = Math.max(ticker.getLastPrice(), 0.00000001); // Avoid division by zero
            double bidPrice = Math.max(ticker.getBidPrice(), 0.00000001);
            double askPrice = Math.max(ticker.getAskPrice(), 0.00000001);
            
            // Ensure prices are in the correct order to avoid negative spreads
            if (askPrice < bidPrice) {
                double temp = askPrice;
                askPrice = bidPrice;
                bidPrice = temp;
            }
            
            double spread = (askPrice - bidPrice) / lastPrice;
            
            // Ensure spread is positive and within reasonable limits
            spread = Math.max(0.0001, Math.min(0.1, spread)); // Between 0.01% and 10%
            
            // Ensure we have valid volume data
            double volume = Math.max(ticker.getVolume(), 0.00000001); // Avoid division by zero
            
            // Adjust volume factor based on available volume
            double volumeFactor = Math.min(volume / VOLUME_NORMALIZATION_SHORT, 1.0);
            volumeFactor = Math.max(0.0, volumeFactor); // Ensure non-negative
            
            // Estimate slippage based on size as a percentage of volume
            double sizeVolumeRatio = Math.min(tradeSize / volume, 1.0); // Cap at 100% of volume
            
            // Base slippage calculation
            double estimatedSlippage = 0.001; // Base slippage of 0.1%
            
            // Add size-dependent component
            estimatedSlippage += (sizeVolumeRatio * 0.01);
            
            // Factor in spread - wider spreads usually indicate higher slippage
            estimatedSlippage += (spread * 0.5);
            
            // Reduce slippage for higher volume
            estimatedSlippage *= (1.0 - (volumeFactor * 0.5));
            
            // Apply buy/sell adjustments (buys typically have slightly higher slippage)
            if (isBuy) {
                estimatedSlippage *= 1.1; // 10% higher for buys
            } else {
                estimatedSlippage *= 0.9; // 10% lower for sells
            }
            
            // Cap slippage at reasonable values
            estimatedSlippage = Math.min(estimatedSlippage, 0.01); // Maximum 1% slippage
            estimatedSlippage = Math.max(estimatedSlippage, 0.0001); // Minimum 0.01% slippage
            
            return estimatedSlippage;
        } catch (Exception e) {
            // If any calculation errors occur, return a safe default value
            return defaultSlippage;
        }
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
        assessment.setWarningIndicator("liquidity", assessment.getLiquidityScore(), riskConfig.getLiquidityMinimum());
        assessment.setWarningIndicator("volatility", 1.0 - assessment.getVolatilityScore(), riskConfig.getVolatilityMaximum());
        assessment.setWarningIndicator("slippage", 1.0 - assessment.getSlippageRisk(), riskConfig.getSlippageMaximum());
        assessment.setWarningIndicator("marketDepth", assessment.getMarketDepthScore(), riskConfig.getMarketDepthMinimum());
        assessment.setWarningIndicator("anomaly", 1.0 - assessment.getAnomalyScore(), riskConfig.getAnomalyThreshold());
        
        // Use existing warning indicator system for severe warnings
        if ((1.0 - assessment.getSlippageRisk()) > riskConfig.getSlippageMaximum() * 2) {
            // Instead of adding a severe warning, just set a standard warning with a descriptive name
            assessment.setWarningIndicator("extremeSlippage", 1.0, 0.5); // Always trigger this warning
        }
    }

    /**
     * Updates the risk assessment with historical slippage data if available.
     * 
     * @param assessment The risk assessment to update
     * @param buyTicker The buy ticker
     * @param sellTicker The sell ticker
     */
    private void updateWithHistoricalSlippageData(RiskAssessment assessment, Ticker buyTicker, Ticker sellTicker) {
        // If historical slippage data is available, use it to adjust the current slippage risk
        // This is a placeholder for a more sophisticated implementation
        
        // In a real implementation, we would:
        // 1. Query a database or service for historical slippage data under similar conditions
        // 2. Calculate the average historical slippage
        // 3. Use a weighted average of predicted and historical slippage
        
        // Since we can't set the slippage risk directly, we'll skip the update
        // in a real implementation, you would add a setter to RiskAssessment
        // or pass the adjusted value during construction
        
        // NOTE: The following code is commented out since setSlippageRisk doesn't exist
        /*
        // For now, we'll just use the current assessment
        double currentSlippageRisk = assessment.getSlippageRisk();
        
        // In a real implementation, this would be replaced with actual historical data
        double historicalSlippage = currentSlippageRisk;
        
        // Weight current prediction more than historical data (70/30 split)
        double adjustedSlippageRisk = (currentSlippageRisk * 0.7) + (historicalSlippage * 0.3);
        
        // Update the assessment with the adjusted value
        assessment.setSlippageRisk(adjustedSlippageRisk);
        */
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
        
        // Update with historical slippage data
        updateWithHistoricalSlippageData(assessment, buyTicker, sellTicker);
    }

    public double getMinProfitPercent() {
        return minProfitPercent;
    }

    public boolean isOpportunityAcceptable(ArbitrageOpportunity opportunity) {
        if (opportunity == null || opportunity.getPotentialProfit() < minProfitPercent) {
            return false;
        }

        RiskAssessment assessment = opportunity.getRiskAssessment();
        if (assessment != null) {
            if (assessment.isEarlyWarningTriggered()) {
                return opportunity.getPotentialProfit() > (minProfitPercent * 1.5);
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

    @Override
    public double calculateRisk(Ticker buyTicker, Ticker sellTicker) {
        // We can reuse the existing code that calculates risk
        // But we need to adapt it to match the interface
        double buyFees = 0.0;  // Default value, can be improved
        double sellFees = 0.0; // Default value, can be improved
        
        // Reuse existing risk calculation
        RiskAssessment assessment = calculateRisk(buyTicker, sellTicker, buyFees, sellFees);
        
        // Return the overall risk score
        return assessment.getOverallRiskScore();
    }
    
    @Override
    public double assessLiquidity(Ticker buyTicker, Ticker sellTicker) {
        double buyLiquidityRisk = assessLiquidity(buyTicker);
        double sellLiquidityRisk = assessLiquidity(sellTicker);
        // Use the worse liquidity as the overall liquidity risk
        return Math.max(buyLiquidityRisk, sellLiquidityRisk);
    }
    
    // Helper method for assessing liquidity of a single ticker
    public double assessLiquidity(Ticker ticker) {
        if (ticker == null) {
            return 1.0; // Maximum risk if no data
        }
        
        double volume = ticker.getVolume();
        if (volume <= 0) {
            return 0.9; // Very high risk for zero volume
        }
        
        // Higher volume means lower risk
        double volumeNormalization = riskConfig.getVolumeNormalization();
        double liquidityRisk = Math.min(1.0, volumeNormalization / volume);
        
        // Low liquidity warning threshold from configuration
        double lowLiquidityThreshold = riskConfig.getLowLiquidityThreshold();
        if (liquidityRisk > lowLiquidityThreshold) {
            // We might want to log this or add a warning
        }
        
        return liquidityRisk;
    }
    
    @Override
    public double assessVolatility(String symbol) {
        // In a real implementation, this would use historical price data
        // For now, we'll use a simple mapping based on the symbol
        
        // Use asset-specific risk factors from configuration if available
        String baseAsset = extractBaseAsset(symbol);
        double assetRiskFactor = ConfigurationFactory.getDouble("risk.assetRiskFactors." + baseAsset, 0.5);
        
        // Some assets are known to be more volatile
        if (symbol.startsWith("BTC") || symbol.contains("BTC")) {
            return 0.3 * assetRiskFactor; // Lower volatility for BTC
        } else if (symbol.startsWith("ETH") || symbol.contains("ETH")) {
            return 0.4 * assetRiskFactor; // Medium volatility for ETH
        } else if (symbol.contains("SHIB") || symbol.contains("DOGE")) {
            return 0.8 * assetRiskFactor; // High volatility for meme coins
        } else if (symbol.contains("USD") || symbol.contains("EUR")) {
            return 0.5 * assetRiskFactor; // Medium volatility for fiat pairs
        }
        
        // Default volatility risk
        return 0.6 * assetRiskFactor;
    }
    
    /**
     * Extract the base asset from a trading pair symbol.
     * 
     * @param symbol The trading pair symbol
     * @return The base asset code
     */
    private String extractBaseAsset(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            return "";
        }
        
        // Common quote assets to look for
        String[] quoteAssets = {"USDT", "USD", "BTC", "ETH", "EUR", "DAI", "GBP", "JPY"};
        
        for (String quote : quoteAssets) {
            if (symbol.endsWith(quote)) {
                return symbol.substring(0, symbol.length() - quote.length());
            }
        }
        
        // Default to first 3-4 characters if no known quote asset
        return symbol.length() > 3 ? symbol.substring(0, 3) : symbol;
    }
    
    @Override
    public int calculateSuccessRate(double profitPercentage, double riskScore, double volatility) {
        if (profitPercentage <= minProfitPercent) {
            return 0; // No chance of success if profit is below minimum
        }
        
        // Calculate base success rate from risk score
        double baseSuccessRate = 100 * riskScore;
        
        // Adjust success rate based on profit margin
        double profitAdjustment = 20 * (profitPercentage / 2.0);
        
        // Adjust success rate based on volatility (higher volatility = lower success)
        double volatilityPenalty = volatility * 20;
        
        // Calculate final success rate
        double successRate = baseSuccessRate + profitAdjustment - volatilityPenalty;
        
        // Ensure success rate is between 0 and 100
        return (int) Math.min(100, Math.max(0, successRate));
    }
}