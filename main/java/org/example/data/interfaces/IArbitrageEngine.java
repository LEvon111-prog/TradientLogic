package org.example.data.interfaces;

import org.example.data.model.ArbitrageOpportunity;
import org.example.data.model.TradingPair;
import org.example.data.service.ExchangeService;

import java.util.List;

/**
 * Interface for arbitrage calculation engines.
 * This defines the contract for classes that identify arbitrage opportunities
 * between different exchanges or markets.
 */
public interface IArbitrageEngine {
    /**
     * Scan for all arbitrage opportunities across configured exchanges.
     *
     * @return The arbitrage result with all found opportunities
     */
    ArbitrageResult scanForOpportunities();

    /**
     * Scan for arbitrage opportunities for specific trading pairs.
     *
     * @param pairs List of trading pairs to scan
     * @return The arbitrage result with opportunities for the specified pairs
     */
    ArbitrageResult scanForOpportunities(List<TradingPair> pairs);

    /**
     * Add an exchange to the arbitrage engine.
     *
     * @param exchange The exchange to add
     */
    void addExchange(ExchangeService exchange);

    /**
     * Remove an exchange from the arbitrage engine.
     *
     * @param exchange The exchange to remove
     */
    void removeExchange(ExchangeService exchange);

    /**
     * Get the list of configured exchanges.
     *
     * @return The list of exchanges
     */
    List<ExchangeService> getExchanges();

    /**
     * Set the minimum profit threshold for arbitrage opportunities.
     *
     * @param threshold The threshold value as a percentage
     */
    void setMinProfitThreshold(double threshold);

    /**
     * Calculate arbitrage opportunity between two exchanges for a specific trading pair.
     *
     * @param fromExchange The exchange to buy from
     * @param toExchange The exchange to sell to
     * @param tradingPair The trading pair (e.g., "BTCUSDT")
     * @param amount The amount to trade
     * @param riskManager The risk manager to evaluate risk
     * @param notificationService The notification service for logging
     * @return An ArbitrageOpportunity object, or null if no opportunity exists
     */
    ArbitrageOpportunity calculateArbitrage(
            IExchangeService fromExchange, 
            IExchangeService toExchange,
            String tradingPair, 
            double amount, 
            IRiskManager riskManager,
            INotificationService notificationService);
} 