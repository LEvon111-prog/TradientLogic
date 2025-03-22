package org.example.domain.risk;

import org.example.data.model.OrderBook;
import org.example.data.model.OrderBookEntry;
import org.example.data.model.Ticker;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced slippage calculation engine for cryptocurrency trading.
 * 
 * This class provides sophisticated algorithms for estimating trading slippage
 * across various market conditions, improving trade execution and profitability.
 * 
 * Key features:
 * - Dynamic parameter calibration based on real-time market conditions
 * - Advanced order book analysis for precise price impact estimation
 * - Historical performance tracking to improve future predictions
 * - Volatility-aware adjustments for changing market conditions
 * - Time-of-day adjustments for liquidity patterns
 * - Feedback loop system for continuous improvement
 * 
 * The calculator adjusts slippage estimates based on order size, market depth,
 * volatility, and historical accuracy of previous estimates.
 * 
 * Compatible with Android platform.
 */
public class AdvancedSlippageCalculator {

    // Constants for base calculation
    private static final double BASE_SLIPPAGE = 0.001; // Base slippage of 0.1%
    private static final double VOLUME_NORMALIZATION = 1000.0; // Volume normalization factor
    private static final double SPREAD_IMPACT_FACTOR = 0.5; // How much spread affects slippage
    private static final double SIZE_IMPACT_FACTOR = 0.01; // How much trade size affects slippage
    private static final double VOLUME_DISCOUNT_FACTOR = 0.5; // How much volume reduces slippage
    
    // Default slippage bounds
    private static final double MIN_SLIPPAGE = 0.0001; // Minimum 0.01% slippage
    private static final double MAX_SLIPPAGE = 0.01; // Maximum 1% slippage
    
    // Dynamic calibration parameters
    private static final double VOLATILITY_MULTIPLIER = 0.5; // How much volatility increases slippage
    private static final double OFF_HOURS_MULTIPLIER = 1.2; // Multiplier for low liquidity hours
    private static final double MARKET_STRESS_MULTIPLIER = 1.5; // Multiplier for stressed markets

    // Historical performance tracking
    private final Map<String, SlippageHistory> slippageHistory = new ConcurrentHashMap<>();
    
    // Market condition tracking
    private final Map<String, MarketCondition> marketConditions = new ConcurrentHashMap<>();
    
    /**
     * Calculates expected slippage with enhanced techniques including order book analysis
     * and dynamic parameter calibration.
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
        // Default slippage value if we can't calculate
        double defaultSlippage = 0.005; // 0.5% as a reasonable default
        
        // Check for null or invalid data
        if (ticker == null || ticker.getLastPrice() <= 0 || ticker.getVolume() <= 0) {
            return defaultSlippage;
        }
        
        try {
            // Get base slippage from simplified calculation
            double baseSlippage = calculateBaseSlippage(ticker, tradeSize, isBuy);
            
            // Enhance with order book analysis if available
            if (orderBook != null) {
                baseSlippage = enhanceWithOrderBookAnalysis(baseSlippage, orderBook, tradeSize, isBuy);
            }
            
            // Apply dynamic calibration based on current market conditions
            double calibratedSlippage = applyDynamicCalibration(baseSlippage, ticker, symbol);
            
            // Apply historical adjustment based on prediction accuracy
            double finalSlippage = applyHistoricalAdjustment(calibratedSlippage, symbol);
            
            // Ensure slippage is within acceptable bounds
            finalSlippage = Math.min(Math.max(finalSlippage, MIN_SLIPPAGE), MAX_SLIPPAGE);
            
            return finalSlippage;
            
        } catch (Exception e) {
            // If any calculation errors occur, return a safe default value
            return defaultSlippage;
        }
    }

    /**
     * Calculates the base slippage using the simplified method.
     */
    private double calculateBaseSlippage(Ticker ticker, double tradeSize, boolean isBuy) {
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
        double volumeFactor = Math.min(volume / VOLUME_NORMALIZATION, 1.0);
        volumeFactor = Math.max(0.0, volumeFactor); // Ensure non-negative
        
        // Estimate slippage based on size as a percentage of volume
        double sizeVolumeRatio = Math.min(tradeSize / volume, 1.0); // Cap at 100% of volume
        
        // Base slippage calculation
        double estimatedSlippage = BASE_SLIPPAGE;
        
        // Add size-dependent component
        estimatedSlippage += (sizeVolumeRatio * SIZE_IMPACT_FACTOR);
        
        // Factor in spread - wider spreads usually indicate higher slippage
        estimatedSlippage += (spread * SPREAD_IMPACT_FACTOR);
        
        // Reduce slippage for higher volume
        estimatedSlippage *= (1.0 - (volumeFactor * VOLUME_DISCOUNT_FACTOR));
        
        // Apply buy/sell adjustments (buys typically have slightly higher slippage)
        if (isBuy) {
            estimatedSlippage *= 1.1; // 10% higher for buys
        } else {
            estimatedSlippage *= 0.9; // 10% lower for sells
        }
        
        return estimatedSlippage;
    }

    /**
     * Enhances slippage calculation using order book data.
     */
    private double enhanceWithOrderBookAnalysis(double baseSlippage, OrderBook orderBook, 
                                              double tradeSize, boolean isBuy) {
        // If we don't have valid order book data, return the base slippage
        if (orderBook == null || orderBook.getBids().isEmpty() || orderBook.getAsks().isEmpty()) {
            return baseSlippage;
        }
        
        // Get relevant side of the order book
        List<OrderBookEntry> relevantSide = isBuy ? orderBook.getAsks() : orderBook.getBids();
        
        // Calculate how much of the order book we'd consume with our trade
        double remainingSize = tradeSize;
        double totalCost = 0;
        double totalSize = 0;
        
        for (OrderBookEntry entry : relevantSide) {
            double available = entry.getVolume();
            double taken = Math.min(remainingSize, available);
            
            totalCost += taken * entry.getPrice();
            totalSize += taken;
            remainingSize -= taken;
            
            if (remainingSize <= 0) {
                break;
            }
        }
        
        // If we couldn't fill the entire order from the visible order book
        if (remainingSize > 0) {
            // Use the last price level with a penalty
            OrderBookEntry lastEntry = relevantSide.get(relevantSide.size() - 1);
            totalCost += remainingSize * lastEntry.getPrice() * (isBuy ? 1.03 : 0.97); // 3% penalty
            totalSize += remainingSize;
        }
        
        // Calculate average execution price
        double avgPrice = totalCost / totalSize;
        
        // Calculate slippage relative to best price
        double bestPrice = isBuy ? relevantSide.get(0).getPrice() : relevantSide.get(0).getPrice();
        double calculatedSlippage = isBuy ? 
            (avgPrice - bestPrice) / bestPrice : 
            (bestPrice - avgPrice) / bestPrice;
        
        // Use the higher of calculated and base slippage for safety
        return Math.max(calculatedSlippage, baseSlippage);
    }

    /**
     * Applies dynamic calibration based on current market conditions.
     */
    private double applyDynamicCalibration(double baseSlippage, Ticker ticker, String symbol) {
        double adjustedSlippage = baseSlippage;
        
        // Adjust for time of day (market hours)
        LocalTime now = LocalTime.now();
        boolean isOffHours = (now.getHour() < 8 || now.getHour() > 16); // simplified example
        
        if (isOffHours) {
            adjustedSlippage *= OFF_HOURS_MULTIPLIER;
        }
        
        // Adjust for recent volatility if we have it
        MarketCondition condition = marketConditions.get(symbol);
        if (condition != null && condition.getVolatility() > 0) {
            double volatilityAdjustment = 1.0 + (condition.getVolatility() * VOLATILITY_MULTIPLIER);
            adjustedSlippage *= volatilityAdjustment;
        }
        
        // Adjust for market stress indicators if available
        if (condition != null && condition.isStressedMarket()) {
            adjustedSlippage *= MARKET_STRESS_MULTIPLIER;
        }
        
        return adjustedSlippage;
    }

    /**
     * Applies adjustments based on historical slippage prediction accuracy.
     */
    private double applyHistoricalAdjustment(double calibratedSlippage, String symbol) {
        SlippageHistory history = slippageHistory.get(symbol);
        
        if (history == null || history.getSampleCount() < 5) {
            // Not enough history for this symbol
            return calibratedSlippage;
        }
        
        // Calculate adjustment based on historical accuracy
        double averagePredictionError = history.getAveragePredictionError();
        
        // If we historically underestimate slippage, increase our prediction
        if (averagePredictionError < 0) {
            return calibratedSlippage * (1.0 - averagePredictionError);
        }
        
        return calibratedSlippage;
    }

    /**
     * Records actual observed slippage to improve future predictions.
     *
     * @param symbol The trading symbol
     * @param predictedSlippage The slippage that was predicted before the trade
     * @param actualSlippage The actual slippage that occurred during execution
     */
    public void recordActualSlippage(String symbol, double predictedSlippage, double actualSlippage) {
        SlippageHistory history = slippageHistory.computeIfAbsent(symbol, k -> new SlippageHistory());
        history.addObservation(predictedSlippage, actualSlippage);
    }

    /**
     * Updates market condition data for a symbol.
     *
     * @param symbol The trading symbol
     * @param volatility Recent price volatility measure
     * @param isStressedMarket Whether the market is showing stress indicators
     */
    public void updateMarketCondition(String symbol, double volatility, boolean isStressedMarket) {
        marketConditions.put(symbol, new MarketCondition(volatility, isStressedMarket));
    }

    /**
     * Helper class to track historical slippage prediction accuracy.
     */
    private static class SlippageHistory {
        private double totalPredictionError = 0;
        private int sampleCount = 0;
        
        public void addObservation(double predicted, double actual) {
            // Positive error means we overestimated, negative means underestimated
            double error = predicted - actual;
            totalPredictionError += error;
            sampleCount++;
        }
        
        public double getAveragePredictionError() {
            return sampleCount > 0 ? totalPredictionError / sampleCount : 0;
        }
        
        public int getSampleCount() {
            return sampleCount;
        }
    }

    /**
     * Helper class to store market condition information.
     */
    private static class MarketCondition {
        private final double volatility;
        private final boolean stressedMarket;
        
        public MarketCondition(double volatility, boolean stressedMarket) {
            this.volatility = volatility;
            this.stressedMarket = stressedMarket;
        }
        
        public double getVolatility() {
            return volatility;
        }
        
        public boolean isStressedMarket() {
            return stressedMarket;
        }
    }
} 