package org.example.domain.risk;

import org.example.data.model.OrderBook;
import org.example.data.model.Ticker;
import java.time.Instant;

/**
 * SlippageAnalyticsBuilder provides a convenient builder pattern for creating and accessing
 * all slippage-related components, making it easier to integrate slippage analytics into the application.
 * This class serves as a central point of access to slippage calculation, volatility tracking,
 * and stress testing functionality.
 */
public class SlippageAnalyticsBuilder {
    
    private AdvancedSlippageCalculator slippageCalculator;
    private VolatilityCalculator volatilityCalculator;
    private SlippageManagerService slippageManager;
    private SlippageStressTester stressTester;
    
    /**
     * Creates a new SlippageAnalyticsBuilder with default components.
     */
    public SlippageAnalyticsBuilder() {
        this.slippageCalculator = new AdvancedSlippageCalculator();
        this.volatilityCalculator = new VolatilityCalculator();
        this.slippageManager = new SlippageManagerService(slippageCalculator, volatilityCalculator);
        this.stressTester = new SlippageStressTester(slippageManager);
    }
    
    /**
     * Gets the advanced slippage calculator.
     *
     * @return The advanced slippage calculator
     */
    public AdvancedSlippageCalculator getSlippageCalculator() {
        return slippageCalculator;
    }
    
    /**
     * Gets the volatility calculator.
     *
     * @return The volatility calculator
     */
    public VolatilityCalculator getVolatilityCalculator() {
        return volatilityCalculator;
    }
    
    /**
     * Gets the slippage manager service.
     *
     * @return The slippage manager service
     */
    public SlippageManagerService getSlippageManager() {
        return slippageManager;
    }
    
    /**
     * Gets the slippage stress tester.
     *
     * @return The slippage stress tester
     */
    public SlippageStressTester getStressTester() {
        return stressTester;
    }
    
    /**
     * Sets a custom slippage calculator.
     *
     * @param slippageCalculator The custom slippage calculator to use
     * @return This builder instance for method chaining
     */
    public SlippageAnalyticsBuilder withSlippageCalculator(AdvancedSlippageCalculator slippageCalculator) {
        this.slippageCalculator = slippageCalculator;
        this.slippageManager = new SlippageManagerService(slippageCalculator, volatilityCalculator);
        this.stressTester = new SlippageStressTester(slippageManager);
        return this;
    }
    
    /**
     * Sets a custom volatility calculator.
     *
     * @param volatilityCalculator The custom volatility calculator to use
     * @return This builder instance for method chaining
     */
    public SlippageAnalyticsBuilder withVolatilityCalculator(VolatilityCalculator volatilityCalculator) {
        this.volatilityCalculator = volatilityCalculator;
        this.slippageManager = new SlippageManagerService(slippageCalculator, volatilityCalculator);
        this.stressTester = new SlippageStressTester(slippageManager);
        return this;
    }
    
    /**
     * Convenience method to calculate slippage using the current configuration.
     *
     * @param ticker The ticker data
     * @param orderBook The order book data (can be null)
     * @param tradeSize The size of the trade
     * @param isBuy Whether this is a buy (true) or sell (false) order
     * @param symbol The trading symbol
     * @return The calculated slippage
     */
    public double calculateSlippage(Ticker ticker, OrderBook orderBook, double tradeSize, boolean isBuy, String symbol) {
        return slippageManager.calculateSlippage(ticker, orderBook, tradeSize, isBuy, symbol);
    }
    
    /**
     * Convenience method to calculate slippage without order book data using the current configuration.
     *
     * @param ticker The ticker data
     * @param tradeSize The size of the trade
     * @param isBuy Whether this is a buy (true) or sell (false) order
     * @param symbol The trading symbol
     * @return The calculated slippage
     */
    public double calculateSlippage(Ticker ticker, double tradeSize, boolean isBuy, String symbol) {
        return slippageManager.calculateSlippage(ticker, tradeSize, isBuy, symbol);
    }
    
    /**
     * Convenience method to perform a stress test using the current configuration.
     *
     * @param symbol The symbol to test
     * @param baseTicker The base ticker to use for the test
     * @return A stress test report
     */
    public SlippageStressTester.StressTestReport performStressTest(String symbol, Ticker baseTicker) {
        return stressTester.performStressTest(symbol, baseTicker);
    }
    
    /**
     * Convenience method to update volatility data.
     *
     * @param symbol The trading symbol
     * @param price The current price
     * @param timestamp The timestamp of the price update in milliseconds
     */
    public void updateVolatility(String symbol, double price, long timestamp) {
        // Convert long timestamp to Instant
        Instant instant = Instant.ofEpochMilli(timestamp);
        volatilityCalculator.updatePrice(symbol, price, instant);
    }
    
    /**
     * Records a pending trade in the feedback system.
     *
     * @param tradeId The unique ID of the trade
     * @param symbol The trading symbol
     * @param tradeSize The size of the trade
     * @param isBuy Whether this is a buy (true) or sell (false) order
     * @param predictedSlippage The predicted slippage for the trade
     * @return This builder instance for method chaining
     */
    public SlippageAnalyticsBuilder recordPendingTrade(String tradeId, String symbol, 
                                                      double tradeSize, boolean isBuy, 
                                                      double predictedSlippage) {
        slippageManager.recordPendingTrade(tradeId, symbol, tradeSize, isBuy, predictedSlippage);
        return this;
    }
    
    /**
     * Records the execution of a trade in the feedback system.
     *
     * @param tradeId The unique ID of the trade
     * @param actualExecutionPrice The actual execution price
     * @param expectedPrice The expected execution price
     * @return This builder instance for method chaining
     */
    public SlippageAnalyticsBuilder recordTradeExecution(String tradeId, double actualExecutionPrice, 
                                                        double expectedPrice) {
        slippageManager.recordTradeExecution(tradeId, actualExecutionPrice, expectedPrice);
        return this;
    }
    
    /**
     * Creates a fully configured SlippageAnalyticsBuilder.
     *
     * @return A new SlippageAnalyticsBuilder instance
     */
    public static SlippageAnalyticsBuilder create() {
        return new SlippageAnalyticsBuilder();
    }
} 