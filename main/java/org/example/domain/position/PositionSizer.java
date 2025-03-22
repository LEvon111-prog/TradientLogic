package org.example.domain.position;

import org.example.data.model.ArbitrageOpportunity;
import org.example.data.model.RiskAssessment;

/**
 * Provides advanced position sizing algorithms for arbitrage trading.
 * Implements strategies including Kelly Criterion and risk-adjusted sizing.
 */
public class PositionSizer {
    
    // Default parameters
    private double maxPositionPct = 0.25;  // Maximum 25% of capital per trade
    private double safetyFactor = 0.5;     // Half-Kelly for conservative sizing
    private double minPositionSize = 10.0; // Minimum position size in base currency

    /**
     * Default constructor with default parameters
     */
    public PositionSizer() {
    }
    
    /**
     * Constructor with custom position sizing parameters
     * 
     * @param maxPositionPct Maximum position as percentage of capital (e.g. 0.25 for 25%)
     * @param safetyFactor Safety factor to apply to Kelly calculation (typically 0.1-0.5)
     * @param minPositionSize Minimum position size to avoid dust positions
     */
    public PositionSizer(double maxPositionPct, double safetyFactor, double minPositionSize) {
        this.maxPositionPct = maxPositionPct;
        this.safetyFactor = safetyFactor;
        this.minPositionSize = minPositionSize;
    }
    
    /**
     * Calculates the optimal position size for an arbitrage opportunity using the Kelly Criterion
     * 
     * @param opportunity The arbitrage opportunity to size
     * @param availableCapital Total capital available for trading
     * @return Optimal position size in base currency units
     */
    public double calculateOptimalPositionSize(ArbitrageOpportunity opportunity, double availableCapital) {
        // Validate inputs
        if (opportunity == null || opportunity.getRiskAssessment() == null) {
            return 0.0;
        }
        
        RiskAssessment risk = opportunity.getRiskAssessment();
        
        // Extract key risk factors
        double overallRisk = risk.getOverallRiskScore();
        double slippageRisk = risk.getSlippageRisk();
        double liquidityScore = risk.getLiquidityScore();
        double volatilityScore = risk.getVolatilityScore();
        
        // Calculate win probability (using overall risk as a proxy)
        double winProbability = Math.min(0.95, overallRisk * 0.9 + 0.05);
        
        // Calculate potential profit and loss
        double potentialProfit = opportunity.getProfitPercent() / 100.0;
        double potentialLoss = 1.0 - slippageRisk; // Use slippage risk as a proxy for potential loss
        
        // Calculate Kelly fraction (optimal bet size as fraction of capital)
        double kellyFraction = 0.0;
        if (potentialLoss > 0) {
            kellyFraction = (winProbability * (1 + potentialProfit) - 1) / potentialLoss;
        }
        
        // Apply safety factor
        kellyFraction *= safetyFactor;
        
        // Cap the position size
        double cappedFraction = Math.min(kellyFraction, maxPositionPct);
        
        // Apply additional risk-based scaling factors
        double liquidityAdjustment = Math.pow(liquidityScore, 1.5); // Penalize low liquidity more aggressively
        double volatilityAdjustment = Math.pow(volatilityScore, 1.2); // Slightly reduce size for high volatility
        
        // Calculate final position size with all constraints
        double optimalFraction = cappedFraction * liquidityAdjustment * volatilityAdjustment;
        
        // Convert fraction to actual position size
        double positionSize = availableCapital * optimalFraction;
        
        // Implement minimum position size threshold (to avoid dust positions)
        if (positionSize < minPositionSize) {
            return 0.0; // Don't trade if optimal size is too small
        }
        
        return positionSize;
    }
    
    /**
     * Calculates an appropriate position size based on fixed percentage of capital.
     * This is a simpler alternative to Kelly when risk factors are uncertain.
     * 
     * @param opportunity The arbitrage opportunity
     * @param availableCapital Total available capital
     * @param riskPercent Percentage of capital to risk (e.g., 0.01 for 1%)
     * @return Position size in base currency
     */
    public double calculateFixedRiskPositionSize(ArbitrageOpportunity opportunity, 
                                                double availableCapital, 
                                                double riskPercent) {
        if (opportunity == null) {
            return 0.0;
        }
        
        // Simple fixed percentage of capital
        double positionSize = availableCapital * riskPercent;
        
        // Adjust for volatility if risk assessment is available
        if (opportunity.getRiskAssessment() != null) {
            double volatilityScore = opportunity.getRiskAssessment().getVolatilityScore();
            // Reduce position for high volatility
            positionSize *= volatilityScore;
        }
        
        // Check minimum size
        if (positionSize < minPositionSize) {
            return 0.0;
        }
        
        return positionSize;
    }
    
    /**
     * Determines if we should adjust our position size due to changing market conditions.
     * 
     * @param currentPosition Current position size
     * @param newOptimalSize Newly calculated optimal position size
     * @param currentPrice Current market price
     * @return true if position should be resized
     */
    public boolean shouldResizePosition(double currentPosition, double newOptimalSize, double currentPrice) {
        // Only resize if the difference is substantial (to avoid excessive trading)
        double sizeDifferenceRatio = Math.abs(currentPosition - newOptimalSize) / currentPosition;
        
        // Calculate the resizing cost (fees, slippage, etc.)
        double resizingCostPct = estimateResizingCost(currentPosition, newOptimalSize, currentPrice);
        
        // Resize if the benefit outweighs the cost
        return sizeDifferenceRatio > 0.2 && sizeDifferenceRatio > resizingCostPct * 2;
    }
    
    /**
     * Estimates the cost of resizing a position as a percentage of position value.
     * 
     * @param currentPosition Current position size
     * @param newPosition New position size
     * @param currentPrice Current market price
     * @return Estimated cost as a percentage
     */
    private double estimateResizingCost(double currentPosition, double newPosition, double currentPrice) {
        // Calculate the size of the adjustment
        double adjustmentSize = Math.abs(currentPosition - newPosition);
        
        // Estimate trading fees
        double feePct = 0.001; // 0.1% fee example
        
        // Estimate slippage based on size
        double slippagePct = 0.001 * Math.sqrt(adjustmentSize / (currentPosition * 10));
        
        return feePct + slippagePct;
    }
    
    // Getters and setters
    
    public double getMaxPositionPct() {
        return maxPositionPct;
    }

    public void setMaxPositionPct(double maxPositionPct) {
        this.maxPositionPct = maxPositionPct;
    }

    public double getSafetyFactor() {
        return safetyFactor;
    }

    public void setSafetyFactor(double safetyFactor) {
        this.safetyFactor = safetyFactor;
    }

    public double getMinPositionSize() {
        return minPositionSize;
    }

    public void setMinPositionSize(double minPositionSize) {
        this.minPositionSize = minPositionSize;
    }
} 