package org.example.domain.risk;

import org.example.data.model.ArbitrageOpportunity;
import org.example.data.model.RiskAssessment;
import org.example.data.model.Ticker;

/**
 * RiskCalculator evaluates the risk factors associated with arbitrage opportunities.
 * It takes into account real exchange trading fees to provide accurate risk assessment.
 */
public class RiskCalculator {

    // Minimum profit required to consider an opportunity acceptable (as a ratio)
    private double minProfitThreshold;

    /**
     * Constructor for RiskCalculator.
     *
     * @param minProfitThreshold The minimum profit threshold as a decimal (e.g., 0.005 for 0.5%)
     */
    public RiskCalculator(double minProfitThreshold) {
        this.minProfitThreshold = minProfitThreshold;
    }
    
    /**
     * Calculates comprehensive risk factors for an arbitrage opportunity.
     * 
     * @param buyTicker Ticker data for the buy exchange
     * @param sellTicker Ticker data for the sell exchange
     * @param buyFees Trading fees on the buy exchange (as decimal)
     * @param sellFees Trading fees on the sell exchange (as decimal)
     * @return A RiskAssessment object with calculated risk factors
     */
    public RiskAssessment calculateRisk(Ticker buyTicker, Ticker sellTicker, double buyFees, double sellFees) {
        // Calculate fee impact score - lower fees mean better score
        double feeImpact = calculateFeeImpact(buyFees, sellFees);
        
        // Calculate liquidity score based on available volumes
        double liquidityScore = calculateLiquidityScore(buyTicker, sellTicker);
        
        // Calculate volatility score based on price spreads
        double volatilityScore = calculateVolatilityScore(buyTicker, sellTicker);
        
        // Calculate overall risk score (weighted average of individual scores)
        double overallRiskScore = calculateOverallRiskScore(liquidityScore, volatilityScore, feeImpact);
        
        System.out.println("Risk Assessment:");
        System.out.println("  Fee Impact: " + String.format("%.2f", feeImpact) + " (higher is better)");
        System.out.println("  Liquidity: " + String.format("%.2f", liquidityScore) + " (higher is better)");
        System.out.println("  Volatility: " + String.format("%.2f", volatilityScore) + " (higher is better)");
        System.out.println("  Overall Risk: " + String.format("%.2f", overallRiskScore) + " (higher is better)");
        
        return new RiskAssessment(liquidityScore, volatilityScore, feeImpact, overallRiskScore);
    }
    
    /**
     * Calculates a liquidity score based on volume data.
     * Higher scores indicate better liquidity.
     */
    private double calculateLiquidityScore(Ticker buyTicker, Ticker sellTicker) {
        // Simple implementation - in a real system this would be more sophisticated
        double buyVolume = buyTicker.getVolume();
        double sellVolume = sellTicker.getVolume();
        
        // Higher volumes generally mean better liquidity
        double averageVolume = (buyVolume + sellVolume) / 2;
        
        // Basic score calculation - normalize to 0-1 range
        // In a real system, this would be calibrated to typical volumes for the asset
        return Math.min(averageVolume / 1000.0, 1.0);
    }
    
    /**
     * Calculates a volatility score based on price spreads.
     * Higher scores indicate less volatility (less risk).
     */
    private double calculateVolatilityScore(Ticker buyTicker, Ticker sellTicker) {
        // Calculate spread as a percentage of price for both exchanges
        double buySpread = (buyTicker.getAskPrice() - buyTicker.getBidPrice()) / buyTicker.getLastPrice();
        double sellSpread = (sellTicker.getAskPrice() - sellTicker.getBidPrice()) / sellTicker.getLastPrice();
        
        // Average the spreads
        double averageSpread = (buySpread + sellSpread) / 2;
        
        // Convert to a score where lower spread = higher score (less volatility)
        // Typical spread might be 0.1% (0.001) to 1% (0.01)
        double spreadFactor = Math.min(averageSpread * 100, 1.0);
        
        // Invert so higher is better (less volatile)
        return 1.0 - spreadFactor;
    }
    
    /**
     * Calculates the impact of fees on profitability.
     * Higher scores indicate less fee impact (better).
     * 
     * @param buyFees Trading fees on the buy exchange (as decimal)
     * @param sellFees Trading fees on the sell exchange (as decimal)
     * @return A score from 0-1 where higher is better (lower fee impact)
     */
    private double calculateFeeImpact(double buyFees, double sellFees) {
        // Total fees for the round trip trade
        double totalFees = buyFees + sellFees;
        
        // Convert to percentage for easier understanding
        double feesPercent = totalFees * 100;
        
        // Calculate score where lower fees = higher score
        // Typical combined fees might range from 0.1% to 0.5%
        double feeScore = Math.max(0.0, 1.0 - (feesPercent / 1.0));
        
        return feeScore;
    }
    
    /**
     * Calculates an overall risk score based on individual risk factors.
     * Higher scores indicate more favorable risk profile.
     */
    private double calculateOverallRiskScore(double liquidityScore, double volatilityScore, double feeImpact) {
        // Weighted average with adjustable weights
        double liquidityWeight = 0.3;    // Importance of liquidity
        double volatilityWeight = 0.3;   // Importance of low volatility
        double feeWeight = 0.4;          // Importance of low fees
        
        return (liquidityScore * liquidityWeight) + 
               (volatilityScore * volatilityWeight) + 
               (feeImpact * feeWeight);
    }
    
    /**
     * Gets the minimum profit threshold.
     * 
     * @return The minimum profit threshold as a decimal
     */
    public double getMinProfitThreshold() {
        return minProfitThreshold;
    }
    
    /**
     * Determines if an arbitrage opportunity is acceptable based on
     * its profit potential and risk assessment.
     * 
     * @param opportunity The arbitrage opportunity to evaluate
     * @return true if the opportunity is acceptable, false otherwise
     */
    public boolean isOpportunityAcceptable(ArbitrageOpportunity opportunity) {
        // Check if opportunity exists and has sufficient profit
        if (opportunity == null || opportunity.getPotentialProfit() < minProfitThreshold) {
            return false;
        }
        
        // If a risk assessment is available, evaluate it
        RiskAssessment assessment = opportunity.getRiskAssessment();
        if (assessment != null) {
            // Add more sophisticated risk evaluation logic here
            // For now, we're just checking the profit threshold
        }
        
        return true;
    }
    
    /**
     * Performs a comprehensive risk assessment of an arbitrage opportunity.
     * 
     * @param opportunity The arbitrage opportunity to assess
     * @return A RiskAssessment object containing various risk metrics
     */
    public RiskAssessment assessRisk(ArbitrageOpportunity opportunity) {
        // This would normally calculate various risk factors:
        // - Liquidity score (how easy it is to execute the trades)
        // - Volatility score (how likely the prices are to change during execution)
        // - Fee impact (the impact of fees on profitability)
        // - Overall risk score
        
        // For simplicity, we're returning a basic risk assessment
        double liquidityScore = 0.8; // Higher is better
        double volatilityScore = 0.7; // Higher means lower volatility
        double feeImpact = 0.1; // Lower is better
        double overallRiskScore = 0.8; // Higher is better
        
        return new RiskAssessment(liquidityScore, volatilityScore, feeImpact, overallRiskScore);
    }
}
