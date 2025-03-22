package org.example.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Configuration model for arbitrage parameters.
 * Contains settings related to profit thresholds, success rate requirements,
 * and other arbitrage-specific properties.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArbitrageConfiguration {
    
    /**
     * Minimum profit percentage required to execute an arbitrage opportunity
     */
    private double minProfitPercent = 0.1;
    
    /**
     * Minimum success rate required for an arbitrage opportunity to be considered viable
     */
    private int minimumSuccessRate = 70;
    
    /**
     * Maximum percentage of capital to use in a single arbitrage trade
     */
    private double maxPositionPercent = 0.25;
    
    /**
     * Base capital available for trading
     */
    private double availableCapital = 100000.0;
    
    /**
     * Minimum trade size in base currency
     */
    private double minimumTradeSize = 10.0;
    
    /**
     * Enable detailed logging of arbitrage calculations
     */
    private boolean detailedLogging = false;
    
    /**
     * Default constructor
     */
    public ArbitrageConfiguration() {
    }
    
    /**
     * Constructor with parameters
     */
    public ArbitrageConfiguration(double minProfitPercent, int minimumSuccessRate,
                                double maxPositionPercent, double availableCapital,
                                double minimumTradeSize, boolean detailedLogging) {
        this.minProfitPercent = minProfitPercent;
        this.minimumSuccessRate = minimumSuccessRate;
        this.maxPositionPercent = maxPositionPercent;
        this.availableCapital = availableCapital;
        this.minimumTradeSize = minimumTradeSize;
        this.detailedLogging = detailedLogging;
    }

    public double getMinProfitPercent() {
        return minProfitPercent;
    }

    public void setMinProfitPercent(double minProfitPercent) {
        this.minProfitPercent = minProfitPercent;
    }

    public int getMinimumSuccessRate() {
        return minimumSuccessRate;
    }

    public void setMinimumSuccessRate(int minimumSuccessRate) {
        this.minimumSuccessRate = minimumSuccessRate;
    }

    public double getMaxPositionPercent() {
        return maxPositionPercent;
    }

    public void setMaxPositionPercent(double maxPositionPercent) {
        this.maxPositionPercent = maxPositionPercent;
    }

    public double getAvailableCapital() {
        return availableCapital;
    }

    public void setAvailableCapital(double availableCapital) {
        this.availableCapital = availableCapital;
    }

    public double getMinimumTradeSize() {
        return minimumTradeSize;
    }

    public void setMinimumTradeSize(double minimumTradeSize) {
        this.minimumTradeSize = minimumTradeSize;
    }

    public boolean isDetailedLogging() {
        return detailedLogging;
    }

    public void setDetailedLogging(boolean detailedLogging) {
        this.detailedLogging = detailedLogging;
    }
} 