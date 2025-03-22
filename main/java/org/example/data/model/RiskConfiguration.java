package org.example.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Configuration model for risk management parameters.
 * Contains settings related to slippage calculation, risk scoring,
 * volatility thresholds, and other risk-related properties.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RiskConfiguration {
    
    /**
     * Maximum acceptable slippage percentage
     */
    private double maxSlippagePercent = 0.3;
    
    /**
     * Base slippage factor used in calculations
     */
    private double baseSlippage = 0.001;
    
    /**
     * Spread impact factor for slippage calculations
     */
    private double spreadImpactFactor = 0.5;
    
    /**
     * Volume normalization factor for slippage calculations
     */
    private double volumeNormalization = 10000.0;
    
    /**
     * Liquidity threshold for high risk classification
     */
    private double lowLiquidityThreshold = 0.3;
    
    /**
     * Volatility threshold for high risk classification
     */
    private double highVolatilityThreshold = 0.7;
    
    /**
     * Number of price points to keep for volatility calculation
     */
    private int priceHistorySize = 100;
    
    /**
     * Volatility spike threshold (standard deviations)
     */
    private double volatilitySpikeThreshold = 3.0;
    
    /**
     * Market stress threshold
     */
    private double marketStressThreshold = 2.5;
    
    /**
     * Enable historical slippage data recording
     */
    private boolean enableSlippageHistory = true;
    
    /**
     * Number of slippage observations to keep in history
     */
    private int slippageHistorySize = 100;
    
    /**
     * Default constructor
     */
    public RiskConfiguration() {
    }

    /**
     * Constructor with parameters
     */
    public RiskConfiguration(double maxSlippagePercent, double baseSlippage, 
                           double spreadImpactFactor, double volumeNormalization,
                           double lowLiquidityThreshold, double highVolatilityThreshold,
                           int priceHistorySize, double volatilitySpikeThreshold,
                           double marketStressThreshold, boolean enableSlippageHistory,
                           int slippageHistorySize) {
        this.maxSlippagePercent = maxSlippagePercent;
        this.baseSlippage = baseSlippage;
        this.spreadImpactFactor = spreadImpactFactor;
        this.volumeNormalization = volumeNormalization;
        this.lowLiquidityThreshold = lowLiquidityThreshold;
        this.highVolatilityThreshold = highVolatilityThreshold;
        this.priceHistorySize = priceHistorySize;
        this.volatilitySpikeThreshold = volatilitySpikeThreshold;
        this.marketStressThreshold = marketStressThreshold;
        this.enableSlippageHistory = enableSlippageHistory;
        this.slippageHistorySize = slippageHistorySize;
    }

    public double getMaxSlippagePercent() {
        return maxSlippagePercent;
    }

    public void setMaxSlippagePercent(double maxSlippagePercent) {
        this.maxSlippagePercent = maxSlippagePercent;
    }

    public double getBaseSlippage() {
        return baseSlippage;
    }

    public void setBaseSlippage(double baseSlippage) {
        this.baseSlippage = baseSlippage;
    }

    public double getSpreadImpactFactor() {
        return spreadImpactFactor;
    }

    public void setSpreadImpactFactor(double spreadImpactFactor) {
        this.spreadImpactFactor = spreadImpactFactor;
    }

    public double getVolumeNormalization() {
        return volumeNormalization;
    }

    public void setVolumeNormalization(double volumeNormalization) {
        this.volumeNormalization = volumeNormalization;
    }

    public double getLowLiquidityThreshold() {
        return lowLiquidityThreshold;
    }

    public void setLowLiquidityThreshold(double lowLiquidityThreshold) {
        this.lowLiquidityThreshold = lowLiquidityThreshold;
    }

    public double getHighVolatilityThreshold() {
        return highVolatilityThreshold;
    }

    public void setHighVolatilityThreshold(double highVolatilityThreshold) {
        this.highVolatilityThreshold = highVolatilityThreshold;
    }

    public int getPriceHistorySize() {
        return priceHistorySize;
    }

    public void setPriceHistorySize(int priceHistorySize) {
        this.priceHistorySize = priceHistorySize;
    }

    public double getVolatilitySpikeThreshold() {
        return volatilitySpikeThreshold;
    }

    public void setVolatilitySpikeThreshold(double volatilitySpikeThreshold) {
        this.volatilitySpikeThreshold = volatilitySpikeThreshold;
    }

    public double getMarketStressThreshold() {
        return marketStressThreshold;
    }

    public void setMarketStressThreshold(double marketStressThreshold) {
        this.marketStressThreshold = marketStressThreshold;
    }

    public boolean isEnableSlippageHistory() {
        return enableSlippageHistory;
    }

    public void setEnableSlippageHistory(boolean enableSlippageHistory) {
        this.enableSlippageHistory = enableSlippageHistory;
    }

    public int getSlippageHistorySize() {
        return slippageHistorySize;
    }

    public void setSlippageHistorySize(int slippageHistorySize) {
        this.slippageHistorySize = slippageHistorySize;
    }
    
    /**
     * Get the minimum acceptable liquidity level
     * 
     * @return The minimum liquidity level
     */
    public double getLiquidityMinimum() {
        return lowLiquidityThreshold;
    }
    
    /**
     * Get the maximum acceptable volatility level
     * 
     * @return The maximum volatility level
     */
    public double getVolatilityMaximum() {
        return highVolatilityThreshold;
    }
    
    /**
     * Get the maximum acceptable slippage percentage
     * 
     * @return The maximum slippage percentage
     */
    public double getSlippageMaximum() {
        return maxSlippagePercent;
    }
    
    /**
     * Get the minimum acceptable market depth
     * 
     * @return The minimum market depth
     */
    public double getMarketDepthMinimum() {
        return 0.3; // Default value, can be made configurable
    }
    
    /**
     * Get the threshold for detecting anomalies
     * 
     * @return The anomaly threshold
     */
    public double getAnomalyThreshold() {
        return 0.7; // Default value, can be made configurable
    }
} 