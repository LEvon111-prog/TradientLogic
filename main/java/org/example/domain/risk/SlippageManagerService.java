package org.example.domain.risk;

import org.example.data.model.OrderBook;
import org.example.data.model.Ticker;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Central service for managing slippage calculations in cryptocurrency trading.
 * 
 * This service coordinates the advanced slippage calculation system, providing
 * a unified interface for estimating price impact, tracking executions, and
 * feeding real results back into the prediction model.
 * 
 * Key functionality:
 * - Calculates expected slippage with enhanced accuracy
 * - Manages volatility data and integrates it into slippage estimates
 * - Maintains a feedback loop of prediction vs. actual results
 * - Caches recent calculations for performance optimization
 * - Tracks pending trades to correlate predictions with outcomes
 * - Performs periodic cleanup of stale data
 * 
 * The service acts as the central coordination point for all slippage-related
 * functionality in the arbitrage system.
 * 
 * Compatible with Android platform.
 */
public class SlippageManagerService {

    private final AdvancedSlippageCalculator slippageCalculator;
    private final VolatilityCalculator volatilityCalculator;
    
    // Cache recent slippage estimates for quick access
    private final Map<String, SlippageEstimate> slippageEstimateCache = new ConcurrentHashMap<>();
    
    // Track pending trades for feedback loop
    private final Map<String, PendingTrade> pendingTrades = new ConcurrentHashMap<>();
    
    // Cleanup scheduler
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    /**
     * Creates a new slippage manager service with default calculator instances.
     */
    public SlippageManagerService() {
        this.slippageCalculator = new AdvancedSlippageCalculator();
        this.volatilityCalculator = new VolatilityCalculator();
        
        // Schedule periodic cleanup of stale data
        scheduler.scheduleAtFixedRate(this::cleanupStaleData, 1, 1, TimeUnit.HOURS);
    }
    
    /**
     * Creates a new slippage manager service with custom calculator instances.
     * 
     * @param slippageCalculator The advanced slippage calculator to use
     * @param volatilityCalculator The volatility calculator to use
     */
    public SlippageManagerService(AdvancedSlippageCalculator slippageCalculator, VolatilityCalculator volatilityCalculator) {
        this.slippageCalculator = slippageCalculator;
        this.volatilityCalculator = volatilityCalculator;
        
        // Schedule periodic cleanup of stale data
        scheduler.scheduleAtFixedRate(this::cleanupStaleData, 1, 1, TimeUnit.HOURS);
    }
    
    /**
     * Calculates expected slippage for a trade with enhanced accuracy and dynamic calibration.
     *
     * @param ticker The market ticker data
     * @param orderBook The order book data (can be null)
     * @param tradeSize The size of the trade to execute
     * @param isBuy Whether this is a buy (true) or sell (false) operation
     * @param symbol The trading symbol
     * @return Expected slippage as a decimal (e.g., 0.002 for 0.2%)
     */
    public double calculateSlippage(Ticker ticker, OrderBook orderBook, double tradeSize, 
                                  boolean isBuy, String symbol) {
        // Update volatility data
        if (ticker != null) {
            volatilityCalculator.updatePrice(symbol, ticker.getLastPrice(), Instant.now());
            
            // Update market condition data for the slippage calculator
            double volatility = volatilityCalculator.calculateVolatility(symbol);
            boolean isStressed = volatilityCalculator.isMarketStressed(symbol);
            slippageCalculator.updateMarketCondition(symbol, volatility, isStressed);
        }
        
        // Calculate slippage
        double slippage = slippageCalculator.calculateSlippage(ticker, orderBook, tradeSize, isBuy, symbol);
        
        // Cache the estimate
        SlippageEstimate estimate = new SlippageEstimate(slippage, tradeSize, isBuy, Instant.now());
        slippageEstimateCache.put(getEstimateKey(symbol, tradeSize, isBuy), estimate);
        
        return slippage;
    }
    
    /**
     * Simplified method for calculating slippage when order book data is not available.
     */
    public double calculateSlippage(Ticker ticker, double tradeSize, boolean isBuy, String symbol) {
        return calculateSlippage(ticker, null, tradeSize, isBuy, symbol);
    }
    
    /**
     * Records that a trade is about to be executed, to track for feedback loop.
     *
     * @param tradeId Unique identifier for the trade
     * @param symbol The trading symbol
     * @param tradeSize The size of the trade
     * @param isBuy Whether this is a buy (true) or sell (false) operation
     * @param predictedSlippage The predicted slippage
     */
    public void recordPendingTrade(String tradeId, String symbol, double tradeSize, 
                                 boolean isBuy, double predictedSlippage) {
        PendingTrade trade = new PendingTrade(symbol, tradeSize, isBuy, predictedSlippage, Instant.now());
        pendingTrades.put(tradeId, trade);
    }
    
    /**
     * Records the actual execution results of a trade to improve future slippage predictions.
     *
     * @param tradeId Unique identifier for the trade
     * @param actualExecutionPrice The actual execution price
     * @param expectedPrice The expected execution price before slippage
     */
    public void recordTradeExecution(String tradeId, double actualExecutionPrice, double expectedPrice) {
        PendingTrade trade = pendingTrades.remove(tradeId);
        if (trade == null) {
            return; // Unknown trade, can't record feedback
        }
        
        // Calculate actual slippage
        double actualSlippage;
        if (trade.isBuy()) {
            // For buys, slippage means paying more than expected
            actualSlippage = (actualExecutionPrice - expectedPrice) / expectedPrice;
        } else {
            // For sells, slippage means receiving less than expected
            actualSlippage = (expectedPrice - actualExecutionPrice) / expectedPrice;
        }
        
        // Only record non-negative slippage values (negative would mean price improvement)
        actualSlippage = Math.max(0, actualSlippage);
        
        // Record for feedback loop
        slippageCalculator.recordActualSlippage(trade.getSymbol(), trade.getPredictedSlippage(), actualSlippage);
    }
    
    /**
     * Gets the volatility calculator for direct use if needed.
     */
    public VolatilityCalculator getVolatilityCalculator() {
        return volatilityCalculator;
    }
    
    /**
     * Shuts down the service and its resources.
     */
    public void shutdown() {
        scheduler.shutdown();
    }
    
    /**
     * Cleans up stale data periodically.
     */
    private void cleanupStaleData() {
        Instant oneHourAgo = Instant.now().minus(1, TimeUnit.HOURS.toChronoUnit());
        
        // Clean up stale estimates
        slippageEstimateCache.entrySet().removeIf(entry -> 
            entry.getValue().getTimestamp().isBefore(oneHourAgo));
        
        // Clean up stale pending trades
        pendingTrades.entrySet().removeIf(entry -> 
            entry.getValue().getTimestamp().isBefore(oneHourAgo));
    }
    
    /**
     * Generates a key for the slippage estimate cache.
     */
    private String getEstimateKey(String symbol, double tradeSize, boolean isBuy) {
        return symbol + ":" + tradeSize + ":" + (isBuy ? "buy" : "sell");
    }
    
    /**
     * Helper class to store slippage estimates.
     */
    private static class SlippageEstimate {
        private final double slippage;
        private final double tradeSize;
        private final boolean isBuy;
        private final Instant timestamp;
        
        public SlippageEstimate(double slippage, double tradeSize, boolean isBuy, Instant timestamp) {
            this.slippage = slippage;
            this.tradeSize = tradeSize;
            this.isBuy = isBuy;
            this.timestamp = timestamp;
        }
        
        public double getSlippage() {
            return slippage;
        }
        
        public double getTradeSize() {
            return tradeSize;
        }
        
        public boolean isBuy() {
            return isBuy;
        }
        
        public Instant getTimestamp() {
            return timestamp;
        }
    }
    
    /**
     * Helper class to track pending trades.
     */
    private static class PendingTrade {
        private final String symbol;
        private final double tradeSize;
        private final boolean isBuy;
        private final double predictedSlippage;
        private final Instant timestamp;
        
        public PendingTrade(String symbol, double tradeSize, boolean isBuy, 
                          double predictedSlippage, Instant timestamp) {
            this.symbol = symbol;
            this.tradeSize = tradeSize;
            this.isBuy = isBuy;
            this.predictedSlippage = predictedSlippage;
            this.timestamp = timestamp;
        }
        
        public String getSymbol() {
            return symbol;
        }
        
        public double getTradeSize() {
            return tradeSize;
        }
        
        public boolean isBuy() {
            return isBuy;
        }
        
        public double getPredictedSlippage() {
            return predictedSlippage;
        }
        
        public Instant getTimestamp() {
            return timestamp;
        }
    }
} 