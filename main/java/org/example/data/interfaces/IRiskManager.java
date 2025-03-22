package org.example.data.interfaces;

import org.example.data.model.Ticker;

/**
 * Interface for risk management and assessment for arbitrage operations.
 * This defines methods for evaluating different risk aspects of trading.
 */
public interface IRiskManager {
    
    /**
     * Calculate the overall risk score for an arbitrage opportunity.
     *
     * @param buyTicker The ticker data for the buy exchange
     * @param sellTicker The ticker data for the sell exchange
     * @return A risk score between 0 (no risk) and 1 (maximum risk)
     */
    double calculateRisk(Ticker buyTicker, Ticker sellTicker);
    
    /**
     * Assess the liquidity of the market based on ticker data.
     *
     * @param buyTicker The ticker data for the buy exchange
     * @param sellTicker The ticker data for the sell exchange
     * @return A liquidity score between 0 (low liquidity) and 1 (high liquidity)
     */
    double assessLiquidity(Ticker buyTicker, Ticker sellTicker);
    
    /**
     * Assess the volatility of a trading pair.
     *
     * @param tradingPair The trading pair to assess
     * @return A volatility score between 0 (low volatility) and 1 (high volatility)
     */
    double assessVolatility(String tradingPair);
    
    /**
     * Calculate the success rate of an arbitrage opportunity.
     *
     * @param profitPercentage The expected profit percentage
     * @param riskScore The calculated risk score
     * @param volatility The volatility score
     * @return A success rate percentage (0-100)
     */
    int calculateSuccessRate(double profitPercentage, double riskScore, double volatility);
} 