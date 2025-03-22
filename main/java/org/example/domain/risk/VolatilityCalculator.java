package org.example.domain.risk;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Real-time market volatility analysis system for cryptocurrency trading.
 * 
 * This calculator tracks price movements to compute volatility metrics and
 * detect market stress conditions, which are critical for accurate slippage
 * estimation and risk management.
 * 
 * Key capabilities:
 * - Maintains rolling price history for multiple trading symbols
 * - Calculates standard deviation of returns to measure volatility
 * - Detects rapid price changes indicating unstable markets
 * - Provides market stress indicators for risk adjustment
 * - Supports multiple timeframes for varying analysis windows
 * 
 * The volatility data produced by this class helps trading algorithms
 * adapt to changing market conditions in real-time, improving execution
 * quality and reducing unexpected costs.
 * 
 * Compatible with Android platform.
 */
public class VolatilityCalculator {

    // How many price points to keep for volatility calculation
    private static final int PRICE_HISTORY_SIZE = 20;
    
    // Threshold for detecting rapid price changes
    private static final double VOLATILITY_SPIKE_THRESHOLD = 0.02; // 2% rapid change
    
    // Threshold for determining market stress
    private static final double MARKET_STRESS_THRESHOLD = 0.05; // 5% volatility indicates stress
    
    // Price history for each trading symbol
    private final Map<String, PriceHistory> priceHistories = new ConcurrentHashMap<>();
    
    /**
     * Updates price history with a new price point.
     *
     * @param symbol The trading symbol
     * @param price The current price
     * @param timestamp The timestamp of the price update
     */
    public void updatePrice(String symbol, double price, Instant timestamp) {
        PriceHistory history = priceHistories.computeIfAbsent(symbol, k -> new PriceHistory());
        history.addPrice(price, timestamp);
    }
    
    /**
     * Calculates recent price volatility for a symbol.
     *
     * @param symbol The trading symbol
     * @return Volatility measure (standard deviation of returns)
     */
    public double calculateVolatility(String symbol) {
        PriceHistory history = priceHistories.get(symbol);
        if (history == null || history.getPricePoints().size() < 2) {
            return 0.0;
        }
        
        // Calculate returns
        double[] returns = new double[history.getPricePoints().size() - 1];
        PricePoint[] points = history.getPricePoints().toArray(new PricePoint[0]);
        
        for (int i = 0; i < points.length - 1; i++) {
            returns[i] = (points[i + 1].getPrice() - points[i].getPrice()) / points[i].getPrice();
        }
        
        // Calculate standard deviation of returns
        double mean = 0.0;
        for (double ret : returns) {
            mean += ret;
        }
        mean /= returns.length;
        
        double variance = 0.0;
        for (double ret : returns) {
            variance += Math.pow(ret - mean, 2);
        }
        variance /= returns.length;
        
        return Math.sqrt(variance);
    }
    
    /**
     * Detects if the market for a symbol is currently under stress.
     *
     * @param symbol The trading symbol
     * @return true if market stress is detected, false otherwise
     */
    public boolean isMarketStressed(String symbol) {
        double volatility = calculateVolatility(symbol);
        
        // High volatility indicates market stress
        if (volatility > MARKET_STRESS_THRESHOLD) {
            return true;
        }
        
        // Check for rapid price changes
        PriceHistory history = priceHistories.get(symbol);
        if (history == null || history.getPricePoints().size() < 2) {
            return false;
        }
        
        PricePoint[] points = history.getPricePoints().toArray(new PricePoint[0]);
        double latestPrice = points[points.length - 1].getPrice();
        double previousPrice = points[points.length - 2].getPrice();
        
        // Detect rapid price changes
        double priceChange = Math.abs(latestPrice - previousPrice) / previousPrice;
        if (priceChange > VOLATILITY_SPIKE_THRESHOLD) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Helper class to store a limited history of price points.
     */
    private static class PriceHistory {
        private final Queue<PricePoint> pricePoints = new LinkedList<>();
        
        public void addPrice(double price, Instant timestamp) {
            pricePoints.add(new PricePoint(price, timestamp));
            
            // Keep only the most recent points
            while (pricePoints.size() > PRICE_HISTORY_SIZE) {
                pricePoints.poll();
            }
        }
        
        public Queue<PricePoint> getPricePoints() {
            return pricePoints;
        }
    }
    
    /**
     * Helper class to store a price point with its timestamp.
     */
    private static class PricePoint {
        private final double price;
        private final Instant timestamp;
        
        public PricePoint(double price, Instant timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }
        
        public double getPrice() {
            return price;
        }
        
        public Instant getTimestamp() {
            return timestamp;
        }
    }
} 