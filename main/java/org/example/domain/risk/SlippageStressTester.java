package org.example.domain.risk;

import org.example.data.model.OrderBook;
import org.example.data.model.OrderBookEntry;
import org.example.data.model.Ticker;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * SlippageStressTester provides capabilities for scenario analysis and stress testing
 * of the slippage calculation system under various market conditions.
 */
public class SlippageStressTester {

    private final SlippageManagerService slippageManager;
    
    /**
     * Creates a new slippage stress tester with the given slippage manager.
     *
     * @param slippageManager The slippage manager service to test
     */
    public SlippageStressTester(SlippageManagerService slippageManager) {
        this.slippageManager = slippageManager;
    }
    
    /**
     * Performs a comprehensive stress test of the slippage calculation system.
     *
     * @param symbol The symbol to test with
     * @param baseTicker A base ticker to modify for the tests
     * @return A report of the stress test results
     */
    public StressTestReport performStressTest(String symbol, Ticker baseTicker) {
        StressTestReport report = new StressTestReport(symbol);
        
        // Create various market scenarios
        Map<String, Function<Ticker, Ticker>> scenarios = createScenarios();
        
        // Test each scenario
        for (Map.Entry<String, Function<Ticker, Ticker>> entry : scenarios.entrySet()) {
            String scenarioName = entry.getKey();
            Function<Ticker, Ticker> scenarioGenerator = entry.getValue();
            
            // Generate scenario ticker
            Ticker scenarioTicker = scenarioGenerator.apply(baseTicker);
            
            // Test various trade sizes
            List<TradeSize> tradeSizes = List.of(
                new TradeSize("Small", baseTicker.getVolume() * 0.001),
                new TradeSize("Medium", baseTicker.getVolume() * 0.01),
                new TradeSize("Large", baseTicker.getVolume() * 0.05),
                new TradeSize("Very Large", baseTicker.getVolume() * 0.2)
            );
            
            // Record results for buy and sell sides
            for (TradeSize tradeSize : tradeSizes) {
                double buySlippage = slippageManager.calculateSlippage(
                    scenarioTicker, tradeSize.getSize(), true, symbol);
                
                double sellSlippage = slippageManager.calculateSlippage(
                    scenarioTicker, tradeSize.getSize(), false, symbol);
                
                report.addResult(scenarioName, tradeSize.getName(), true, buySlippage);
                report.addResult(scenarioName, tradeSize.getName(), false, sellSlippage);
            }
        }
        
        return report;
    }
    
    /**
     * Tests slippage calculation with historical flash crash data.
     *
     * @param symbol The symbol to test with
     * @param historicalFlashCrash A ticker representing flash crash conditions
     * @param normalMarket A ticker representing normal market conditions
     * @param tradeSize The trade size to test with
     * @return A report comparing slippage in normal vs. flash crash conditions
     */
    public FlashCrashReport testFlashCrashScenario(String symbol, Ticker historicalFlashCrash, 
                                               Ticker normalMarket, double tradeSize) {
        double normalBuySlippage = slippageManager.calculateSlippage(
            normalMarket, tradeSize, true, symbol);
        
        double normalSellSlippage = slippageManager.calculateSlippage(
            normalMarket, tradeSize, false, symbol);
        
        double crashBuySlippage = slippageManager.calculateSlippage(
            historicalFlashCrash, tradeSize, true, symbol);
        
        double crashSellSlippage = slippageManager.calculateSlippage(
            historicalFlashCrash, tradeSize, false, symbol);
        
        return new FlashCrashReport(symbol, tradeSize, normalBuySlippage, normalSellSlippage,
                                 crashBuySlippage, crashSellSlippage);
    }
    
    /**
     * Creates a set of market scenarios for testing.
     */
    private Map<String, Function<Ticker, Ticker>> createScenarios() {
        Map<String, Function<Ticker, Ticker>> scenarios = new HashMap<>();
        
        // Normal market conditions
        scenarios.put("Normal Market", ticker -> ticker);
        
        // Wide spread scenario (2x normal spread)
        scenarios.put("Wide Spread", ticker -> {
            double midPrice = ticker.getLastPrice();
            double normalSpread = ticker.getAskPrice() - ticker.getBidPrice();
            double wideSpread = normalSpread * 2;
            
            return new Ticker(
                midPrice - (wideSpread / 2),  // Wider bid
                midPrice + (wideSpread / 2),  // Wider ask
                ticker.getLastPrice(),
                ticker.getVolume(),
                ticker.getTimestamp()
            );
        });
        
        // Low volume scenario (10% of normal volume)
        scenarios.put("Low Volume", ticker -> new Ticker(
            ticker.getBidPrice(),
            ticker.getAskPrice(),
            ticker.getLastPrice(),
            ticker.getVolume() * 0.1,
            ticker.getTimestamp()
        ));
        
        // High volatility scenario (3x spread)
        scenarios.put("High Volatility", ticker -> {
            double midPrice = ticker.getLastPrice();
            double normalSpread = ticker.getAskPrice() - ticker.getBidPrice();
            double wideSpread = normalSpread * 3;
            
            return new Ticker(
                midPrice - (wideSpread / 2),
                midPrice + (wideSpread / 2),
                ticker.getLastPrice(),
                ticker.getVolume(),
                ticker.getTimestamp()
            );
        });
        
        // Flash crash scenario (price down 20%, very wide spread, low volume)
        scenarios.put("Flash Crash", ticker -> {
            double crashPrice = ticker.getLastPrice() * 0.8;
            double normalSpread = ticker.getAskPrice() - ticker.getBidPrice();
            double panicSpread = normalSpread * 5;
            
            return new Ticker(
                crashPrice - (panicSpread / 2),
                crashPrice + (panicSpread / 2),
                crashPrice,
                ticker.getVolume() * 0.3,
                ticker.getTimestamp()
            );
        });
        
        // Bull run scenario (price up 10%, moderate spread, high volume)
        scenarios.put("Bull Run", ticker -> {
            double bullPrice = ticker.getLastPrice() * 1.1;
            double normalSpread = ticker.getAskPrice() - ticker.getBidPrice();
            double excitementSpread = normalSpread * 1.5;
            
            return new Ticker(
                bullPrice - (excitementSpread / 2),
                bullPrice + (excitementSpread / 2),
                bullPrice,
                ticker.getVolume() * 2.0,
                ticker.getTimestamp()
            );
        });
        
        return scenarios;
    }
    
    /**
     * Creates a simulated order book for testing with specific market depth characteristics.
     *
     * @param ticker The ticker to base the order book on
     * @param symbol The trading symbol for the order book
     * @param depth The number of levels to create
     * @param depthProfile The profile of the depth (e.g., "normal", "thin", "thick")
     * @return A simulated order book
     */
    public OrderBook createSimulatedOrderBook(Ticker ticker, String symbol, int depth, String depthProfile) {
        List<OrderBookEntry> bids = new ArrayList<>();
        List<OrderBookEntry> asks = new ArrayList<>();
        
        double bidStart = ticker.getBidPrice();
        double askStart = ticker.getAskPrice();
        double avgVolume = ticker.getVolume() / (depth * 5); // Rough estimate for single level
        
        // Determine depth profile multipliers
        double volumeMultiplier;
        double priceStepMultiplier;
        
        switch (depthProfile.toLowerCase()) {
            case "thin":
                volumeMultiplier = 0.5;  // Less volume at each level
                priceStepMultiplier = 2.0;  // Bigger price gaps
                break;
            case "thick":
                volumeMultiplier = 2.0;  // More volume at each level
                priceStepMultiplier = 0.5;  // Smaller price gaps
                break;
            case "normal":
            default:
                volumeMultiplier = 1.0;
                priceStepMultiplier = 1.0;
                break;
        }
        
        // Create bid side (descending prices)
        double priceStep = (bidStart * 0.001) * priceStepMultiplier; // 0.1% steps as default
        double currentBid = bidStart;
        
        for (int i = 0; i < depth; i++) {
            // Volume decreases as we get further from the mid price
            double levelVolume = avgVolume * volumeMultiplier * (1.0 - (i * 0.5 / depth));
            bids.add(new OrderBookEntry(currentBid, levelVolume));
            currentBid -= priceStep;
        }
        
        // Create ask side (ascending prices)
        double currentAsk = askStart;
        
        for (int i = 0; i < depth; i++) {
            // Volume decreases as we get further from the mid price
            double levelVolume = avgVolume * volumeMultiplier * (1.0 - (i * 0.5 / depth));
            asks.add(new OrderBookEntry(currentAsk, levelVolume));
            currentAsk += priceStep;
        }
        
        // Convert Instant to Date when creating OrderBook
        return new OrderBook(symbol, bids, asks, Date.from(Instant.now()));
    }
    
    /**
     * Helper class to represent a trade size configuration.
     */
    private static class TradeSize {
        private final String name;
        private final double size;
        
        public TradeSize(String name, double size) {
            this.name = name;
            this.size = size;
        }
        
        public String getName() {
            return name;
        }
        
        public double getSize() {
            return size;
        }
    }
    
    /**
     * Class representing a stress test report.
     */
    public static class StressTestReport {
        private final String symbol;
        private final Map<String, Map<String, Map<Boolean, Double>>> results = new HashMap<>();
        
        public StressTestReport(String symbol) {
            this.symbol = symbol;
        }
        
        public void addResult(String scenario, String tradeSize, boolean isBuy, double slippage) {
            results.computeIfAbsent(scenario, k -> new HashMap<>())
                   .computeIfAbsent(tradeSize, k -> new HashMap<>())
                   .put(isBuy, slippage);
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Stress Test Report for ").append(symbol).append("\n");
            sb.append("======================================\n\n");
            
            for (String scenario : results.keySet()) {
                sb.append("Scenario: ").append(scenario).append("\n");
                sb.append("--------------------\n");
                
                for (String tradeSize : results.get(scenario).keySet()) {
                    Map<Boolean, Double> buySelllSlippage = results.get(scenario).get(tradeSize);
                    double buySlippage = buySelllSlippage.getOrDefault(true, 0.0);
                    double sellSlippage = buySelllSlippage.getOrDefault(false, 0.0);
                    
                    sb.append(String.format("  %-10s: Buy Slippage: %.4f%%, Sell Slippage: %.4f%%\n", 
                        tradeSize, buySlippage * 100, sellSlippage * 100));
                }
                
                sb.append("\n");
            }
            
            return sb.toString();
        }
        
        public String getSymbol() {
            return symbol;
        }
        
        public Map<String, Map<String, Map<Boolean, Double>>> getResults() {
            return results;
        }
    }
    
    /**
     * Class representing a flash crash test report.
     */
    public static class FlashCrashReport {
        private final String symbol;
        private final double tradeSize;
        private final double normalBuySlippage;
        private final double normalSellSlippage;
        private final double crashBuySlippage;
        private final double crashSellSlippage;
        
        public FlashCrashReport(String symbol, double tradeSize,
                             double normalBuySlippage, double normalSellSlippage,
                             double crashBuySlippage, double crashSellSlippage) {
            this.symbol = symbol;
            this.tradeSize = tradeSize;
            this.normalBuySlippage = normalBuySlippage;
            this.normalSellSlippage = normalSellSlippage;
            this.crashBuySlippage = crashBuySlippage;
            this.crashSellSlippage = crashSellSlippage;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Flash Crash Analysis for ").append(symbol).append("\n");
            sb.append("======================================\n");
            sb.append(String.format("Trade Size: %.2f units\n\n", tradeSize));
            
            sb.append("Normal Market Conditions:\n");
            sb.append(String.format("  Buy Slippage: %.4f%%\n", normalBuySlippage * 100));
            sb.append(String.format("  Sell Slippage: %.4f%%\n\n", normalSellSlippage * 100));
            
            sb.append("Flash Crash Conditions:\n");
            sb.append(String.format("  Buy Slippage: %.4f%%\n", crashBuySlippage * 100));
            sb.append(String.format("  Sell Slippage: %.4f%%\n\n", crashSellSlippage * 100));
            
            double buyIncrease = (crashBuySlippage / normalBuySlippage) - 1.0;
            double sellIncrease = (crashSellSlippage / normalSellSlippage) - 1.0;
            
            sb.append("Analysis:\n");
            sb.append(String.format("  Buy Slippage Increase: %.2f times higher during crash\n",
                                  crashBuySlippage / normalBuySlippage));
            sb.append(String.format("  Sell Slippage Increase: %.2f times higher during crash\n",
                                  crashSellSlippage / normalSellSlippage));
            
            return sb.toString();
        }
        
        public String getSymbol() {
            return symbol;
        }
        
        public double getTradeSize() {
            return tradeSize;
        }
        
        public double getNormalBuySlippage() {
            return normalBuySlippage;
        }
        
        public double getNormalSellSlippage() {
            return normalSellSlippage;
        }
        
        public double getCrashBuySlippage() {
            return crashBuySlippage;
        }
        
        public double getCrashSellSlippage() {
            return crashSellSlippage;
        }
    }
} 