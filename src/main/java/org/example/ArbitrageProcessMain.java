package org.example;

import org.example.data.model.ArbitrageOpportunity;
import org.example.data.model.Ticker;
import org.example.data.model.TradingPair;
import org.example.data.model.RiskAssessment;
import org.example.data.service.BinanceExchangeService;
import org.example.data.service.BybitV5ExchangeService;
import org.example.data.service.CoinbaseExchangeService;
import org.example.data.service.ExchangeService;
import org.example.data.service.KrakenExchangeService;
import org.example.domain.engine.ArbitrageEngine;
import org.example.domain.engine.ExchangeToExchangeArbitrage;
import org.example.domain.risk.RiskCalculator;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ArbitrageProcessMain {
    // Minimum profit percentage to consider an arbitrage opportunity
    private static final double MIN_PROFIT_PERCENT = 0.1; // 0.5%
    
    // Store exchange symbol mappings
    private static Map<ExchangeService, Map<String, String>> exchangeSymbolMap = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("=== Starting Real-time Arbitrage Process with WebSocket Data ===");

        // Step 1: Initialize all Exchange Services.
        ExchangeService binance = new BinanceExchangeService(0.001);      // 0.1% fee
        ExchangeService coinbase = new CoinbaseExchangeService(0.001);    // 0.1% fee
        ExchangeService kraken = new KrakenExchangeService(0.002);        // 0.2% fee
        ExchangeService bybit = new BybitV5ExchangeService(0.001);        // 0.1% fee

        List<ExchangeService> exchanges = new ArrayList<>();
        exchanges.add(binance);
        exchanges.add(coinbase);
        exchanges.add(kraken);
        exchanges.add(bybit);

        // Step 2: Fetch Trading Pairs from Each Exchange using REST API.
        System.out.println("\n[Step 2] Fetching Trading Pairs from each Exchange...");
        for (ExchangeService ex : exchanges) {
            System.out.println("\n[" + ex.getExchangeName() + "] Fetching Trading Pairs...");
            List<TradingPair> pairs = ex.fetchTradingPairs();
            System.out.println("[" + ex.getExchangeName() + "] Fetched " + pairs.size() + " trading pairs.");
        }

        // Step 3: Determine common trading pairs among all exchanges using symbol normalization
        System.out.println("\n[Step 3] Determining common trading pairs using symbol normalization...");
        List<String> commonSymbols = findCommonSymbols(exchanges);
        
        // For demo purposes, limit to a few common symbols if there are too many
        if (commonSymbols.size() > 10) {
            commonSymbols = commonSymbols.subList(0, 10);
            System.out.println("Limited to 10 common symbols for demonstration purposes");
        }
        

        
        // Step 4: Initialize WebSocket connections for all exchanges with proper error handling

        // Only attempt to initialize WebSockets if there are common symbols
        if (!commonSymbols.isEmpty()) {
            try {
                // For each exchange, we need to use its specific symbol format
                for (ExchangeService ex : exchanges) {
                    try {
                        // Get the exchange-specific symbols for this exchange
                        List<String> exchangeSpecificSymbols = new ArrayList<>();
                        Map<String, String> symbolMap = exchangeSymbolMap.get(ex);
                        
                        if (symbolMap != null) {
                            for (String normalizedSymbol : commonSymbols) {
                                String exchangeSymbol = symbolMap.get(normalizedSymbol);
                                if (exchangeSymbol != null) {
                                    exchangeSpecificSymbols.add(exchangeSymbol);
                                }
                            }
                        }
                        
                        if (exchangeSpecificSymbols.isEmpty()) {
                            continue;
                        }
                        
                        boolean success = ex.initializeWebSocket(exchangeSpecificSymbols);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                // Wait for WebSocket connections to establish and receive initial data

            } catch (Exception e) {
                System.err.println("Error during WebSocket initialization: " + e.getMessage());
                e.printStackTrace();
                System.out.println("Continuing with REST API fallback for all exchanges");
            }
        } else {
            System.out.println("No common symbols found across exchanges. WebSocket initialization skipped.");
        }
        
        // Step 5: Create a risk calculator with a minimum profit threshold (0.5%)
        System.out.println("\n[Step 5] Creating risk calculator with profit threshold...");
        RiskCalculator riskCalculator = new RiskCalculator(MIN_PROFIT_PERCENT / 100); // Convert to decimal
        
        // Step 6: Create and initialize the arbitrage engine
        System.out.println("\n[Step 6] Creating and initializing the arbitrage engine...");
        ArbitrageEngine arbitrageEngine = new ArbitrageEngine(exchanges, riskCalculator);
        
        // Create final copies of variables to use in the lambda expression
        final List<ExchangeService> finalExchanges = exchanges;
        final List<String> finalCommonSymbols = commonSymbols;
        
        // Step 7: Set up a scheduled task to scan for arbitrage opportunities periodically
        System.out.println("\n[Step 7] Setting up scheduled arbitrage scanning...");
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        // Schedule the arbitrage scan to run every 5 seconds
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("\n=== Running Arbitrage Scan at " + new java.util.Date() + " ===");
                
                // Run the arbitrage scanning process
                arbitrageEngine.scanForOpportunities();
                
                // For demonstration, also run a direct arbitrage check between exchanges
                System.out.println("\n=== Running Direct Exchange-to-Exchange Comparison ===");
                runDirectComparisonStatic(finalExchanges, finalCommonSymbols);
            } catch (Exception e) {
                System.err.println("Error during arbitrage scan: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.SECONDS);
        
        // Add shutdown hook to gracefully close resources when program is terminated
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n=== Shutting down Arbitrage Process ===");
            scheduler.shutdown();
            
            // Close all WebSocket connections
            for (ExchangeService ex : finalExchanges) {
                try {
                    ex.closeWebSocket();
                } catch (Exception e) {
                    System.err.println("Error closing WebSocket for " + ex.getExchangeName() + ": " + e.getMessage());
                }
            }
            
            System.out.println("=== Arbitrage Process Shutdown Complete ===");
        }));
        
        System.out.println("\n=== Arbitrage Process is running. Press Ctrl+C to stop. ===");
    }
    
    /**
     * Runs a direct comparison between exchanges for all common symbols.
     * This provides a more detailed view of the price differences.
     * Note: This is the original method which doesn't use symbol normalization.
     */
    private static void runDirectArbitrageComparison(List<ExchangeService> exchanges, Set<String> commonSymbols) {
        if (commonSymbols.isEmpty()) {
            System.out.println("No common symbols to compare between exchanges.");
            return;
        }
        
        for (int i = 0; i < exchanges.size(); i++) {
            for (int j = i + 1; j < exchanges.size(); j++) {
                ExchangeService exA = exchanges.get(i);
                ExchangeService exB = exchanges.get(j);
                
                System.out.println("\n=== Comparing " + exA.getExchangeName() + " vs " + exB.getExchangeName() + " ===");
                
                for (String symbol : commonSymbols) {
                    try {
                    TradingPair pair = new TradingPair(symbol);

                        // Create an instance of the arbitrage calculator
                    ExchangeToExchangeArbitrage arbitrageCalc = new ExchangeToExchangeArbitrage(exA, exB);

                        // Calculate arbitrage opportunity
                    ArbitrageOpportunity opportunity = arbitrageCalc.calculateArbitrage(pair);

                    if (opportunity != null) {
                        System.out.println(">>> Arbitrage Opportunity Found: " + opportunity);
                        }
                    } catch (Exception e) {
                        System.err.println("Error calculating arbitrage for " + symbol + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Normalizes trading pair symbols to a common format to enable cross-exchange comparison
     * @param symbol The original symbol from an exchange
     * @return A normalized symbol string
     */
    private static String normalizeSymbol(String symbol) {
        // Remove any separators like '-' or '/'
        String normalized = symbol.replace("-", "").replace("/", "").toUpperCase();
        
        // Handle special cases like XBT (Kraken's name for BTC)
        if (normalized.startsWith("XBT")) {
            normalized = "BTC" + normalized.substring(3);
        }
        
        // Strip exchange-specific suffixes if needed
        // Some exchanges add specific identifiers we don't need for comparison
        if (normalized.endsWith(".P") || normalized.endsWith(".T")) {
            normalized = normalized.substring(0, normalized.length() - 2);
        }
        
        return normalized;
    }

    /**
     * Find common symbols across exchanges, accounting for different format conventions
     */
    private static List<String> findCommonSymbols(List<ExchangeService> exchangeServices) {
        if (exchangeServices.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Create maps to store normalized symbol -> original symbol for each exchange
        Map<ExchangeService, Map<String, String>> exchangeSymbolMaps = new HashMap<>();
        
        // Populate maps with normalized symbols for each exchange
        for (ExchangeService exchange : exchangeServices) {
            Map<String, String> normalizedMap = new HashMap<>();
            List<TradingPair> pairs = exchange.getTradingPairs();
            
            // Skip exchanges with no trading pairs
            if (pairs == null || pairs.isEmpty()) {
                System.out.println("Warning: No trading pairs available for " + exchange.getExchangeName());
                continue;
            }
            
            for (TradingPair pair : pairs) {
                String originalSymbol = pair.getSymbol();
                String normalizedSymbol = normalizeSymbol(originalSymbol);
                normalizedMap.put(normalizedSymbol, originalSymbol);
            }
            
            exchangeSymbolMaps.put(exchange, normalizedMap);
            System.out.println("Found " + normalizedMap.size() + " trading pairs for " + exchange.getExchangeName());
        }
        
        // Find common normalized symbols across all exchanges
        Set<String> commonNormalizedSymbols = null;
        
        for (Map<String, String> symbolMap : exchangeSymbolMaps.values()) {
            if (commonNormalizedSymbols == null) {
                commonNormalizedSymbols = new HashSet<>(symbolMap.keySet());
                    } else {
                commonNormalizedSymbols.retainAll(symbolMap.keySet());
            }
        }
        
        if (commonNormalizedSymbols == null || commonNormalizedSymbols.isEmpty()) {
            System.out.println("No common symbols found across exchanges after normalization");
            return new ArrayList<>();
        }
        
        // Create mapping of normalized symbols to original symbols for each exchange
        // This will help us later when we need to query data for specific exchange formats
        exchangeSymbolMap.clear(); // Clear any previous mappings
        
        for (String normalizedSymbol : commonNormalizedSymbols) {
            StringBuilder sb = new StringBuilder("Found common symbol: " + normalizedSymbol + " (");
            boolean first = true;
            
            for (ExchangeService exchange : exchangeSymbolMaps.keySet()) {
                Map<String, String> symbolMap = exchangeSymbolMaps.get(exchange);
                String originalSymbol = symbolMap.get(normalizedSymbol);
                
                if (originalSymbol != null) {
                    if (!first) {
                        sb.append(", ");
                    }
                    sb.append(exchange.getExchangeName()).append(": ").append(originalSymbol);
                    first = false;
                    
                    // Store the mapping for later use
                    if (!exchangeSymbolMap.containsKey(exchange)) {
                        exchangeSymbolMap.put(exchange, new HashMap<>());
                    }
                    exchangeSymbolMap.get(exchange).put(normalizedSymbol, originalSymbol);
                }
            }
            sb.append(")");
            System.out.println(sb.toString());
        }
        
        return new ArrayList<>(commonNormalizedSymbols);
    }

    /**
     * Static version of the direct comparison method that uses symbol mappings
     */
    private static void runDirectComparisonStatic(List<ExchangeService> exchanges, List<String> commonSymbols) {
        System.out.println("\n=== Running Direct Exchange-to-Exchange Comparison ===");
        
        if (commonSymbols == null || commonSymbols.isEmpty()) {
            System.out.println("No common symbols to compare between exchanges.");
            return;
        }
        
        List<ArbitrageOpportunity> opportunities = new ArrayList<>();
        
        for (String normalizedSymbol : commonSymbols) {
            for (int i = 0; i < exchanges.size(); i++) {
                for (int j = i + 1; j < exchanges.size(); j++) {
                    ExchangeService exchangeA = exchanges.get(i);
                    ExchangeService exchangeB = exchanges.get(j);
                    
                    // Get the exchange-specific symbols
                    if (!exchangeSymbolMap.containsKey(exchangeA) || !exchangeSymbolMap.containsKey(exchangeB)) {
                        continue;
                    }
                    
                    String symbolA = exchangeSymbolMap.get(exchangeA).get(normalizedSymbol);
                    String symbolB = exchangeSymbolMap.get(exchangeB).get(normalizedSymbol);
                    
                    if (symbolA == null || symbolB == null) {
                        continue;
                    }
                    
                    try {
                        // Get ticker data for both exchanges
                        Ticker tickerA = exchangeA.getTickerData(symbolA);
                        Ticker tickerB = exchangeB.getTickerData(symbolB);
                        
                        if (tickerA == null || tickerB == null) {
                            continue;
                        }
                        
                        // Check for arbitrage opportunity A -> B (buy on A, sell on B)
                        double buyPrice = tickerA.getAskPrice();
                        double sellPrice = tickerB.getBidPrice();
                        double profitPercent = (sellPrice / buyPrice - 1) * 100;
                        
                        if (profitPercent > MIN_PROFIT_PERCENT) {
                            // Create risk calculator for this opportunity
                            RiskCalculator riskCalc = new RiskCalculator(MIN_PROFIT_PERCENT / 100);
                            
                            // Calculate risk assessment
                            RiskAssessment riskAssessment = riskCalc.calculateRisk(tickerA, tickerB, 
                                    exchangeA.getTradingFees(), exchangeB.getTradingFees());
                            
                            ArbitrageOpportunity opportunity = new ArbitrageOpportunity(
                                    normalizedSymbol,
                                    symbolA,
                                    symbolB,
                                    exchangeA.getExchangeName(),
                                    exchangeB.getExchangeName(),
                                    buyPrice,
                                    sellPrice,
                                    profitPercent
                            );
                            
                            // Set the risk assessment on the opportunity
                            opportunity.setRiskAssessment(riskAssessment);
                            
                            opportunities.add(opportunity);
                            System.out.println("Found opportunity: " + opportunity);
                            System.out.println("  Risk Assessment:");
                            System.out.println("    Liquidity Score: " + String.format("%.2f", riskAssessment.getLiquidityScore()) + " (higher is better)");
                            System.out.println("    Volatility Score: " + String.format("%.2f", riskAssessment.getVolatilityScore()) + " (higher is better)");
                            System.out.println("    Fee Impact: " + String.format("%.2f", riskAssessment.getFeeImpact()) + " (higher is better)");
                            System.out.println("    Market Depth: " + String.format("%.2f", riskAssessment.getMarketDepthScore()) + " (higher is better)");
                            System.out.println("    Execution Speed Risk: " + String.format("%.2f", riskAssessment.getExecutionSpeedRisk()) + " (higher is better)");
                            System.out.println("    Overall Risk Score: " + String.format("%.2f", riskAssessment.getOverallRiskScore()) + " (higher is better)");
                        }
                        
                        // Check for arbitrage opportunity B -> A (buy on B, sell on A)
                        buyPrice = tickerB.getAskPrice();
                        sellPrice = tickerA.getBidPrice();
                        profitPercent = (sellPrice / buyPrice - 1) * 100;
                        
                        if (profitPercent > MIN_PROFIT_PERCENT) {
                            // Create risk calculator for this opportunity
                            RiskCalculator riskCalc = new RiskCalculator(MIN_PROFIT_PERCENT / 100);
                            
                            // Calculate risk assessment
                            RiskAssessment riskAssessment = riskCalc.calculateRisk(tickerB, tickerA, 
                                    exchangeB.getTradingFees(), exchangeA.getTradingFees());
                            
                            ArbitrageOpportunity opportunity = new ArbitrageOpportunity(
                                    normalizedSymbol,
                                    symbolB,
                                    symbolA,
                                    exchangeB.getExchangeName(),
                                    exchangeA.getExchangeName(),
                                    buyPrice,
                                    sellPrice,
                                    profitPercent
                            );
                            
                            // Set the risk assessment on the opportunity
                            opportunity.setRiskAssessment(riskAssessment);
                            
                            opportunities.add(opportunity);
                            System.out.println("Found opportunity: " + opportunity);
                            System.out.println("  Risk Assessment:");
                            System.out.println("    Liquidity Score: " + String.format("%.2f", riskAssessment.getLiquidityScore()) + " (higher is better)");
                            System.out.println("    Volatility Score: " + String.format("%.2f", riskAssessment.getVolatilityScore()) + " (higher is better)");
                            System.out.println("    Fee Impact: " + String.format("%.2f", riskAssessment.getFeeImpact()) + " (higher is better)");
                            System.out.println("    Market Depth: " + String.format("%.2f", riskAssessment.getMarketDepthScore()) + " (higher is better)");
                            System.out.println("    Execution Speed Risk: " + String.format("%.2f", riskAssessment.getExecutionSpeedRisk()) + " (higher is better)");
                            System.out.println("    Overall Risk Score: " + String.format("%.2f", riskAssessment.getOverallRiskScore()) + " (higher is better)");
                        }
                    } catch (Exception e) {
                        System.err.println("Error comparing " + symbolA + " on " + exchangeA.getExchangeName() + 
                                " with " + symbolB + " on " + exchangeB.getExchangeName() + ": " + e.getMessage());
                    }
                }
            }
        }
        
        if (opportunities.isEmpty()) {
            System.out.println("No arbitrage opportunities found above " + MIN_PROFIT_PERCENT + "% profit threshold.");
        } else {
            System.out.println("Found " + opportunities.size() + " arbitrage opportunities!");
            
            // Sort opportunities by profit percentage (descending)
            opportunities.sort((o1, o2) -> Double.compare(o2.getProfitPercent(), o1.getProfitPercent()));
            
            // Display the top opportunities with risk assessment
            int displayCount = Math.min(5, opportunities.size());
            System.out.println("\nTop " + displayCount + " opportunities with risk assessment:");
            for (int i = 0; i < displayCount; i++) {
                ArbitrageOpportunity opportunity = opportunities.get(i);
                RiskAssessment risk = opportunity.getRiskAssessment();
                
                System.out.println((i+1) + ". " + opportunity);
                System.out.println("   Risk Assessment Summary:");
                System.out.println("     Liquidity: " + String.format("%.2f", risk.getLiquidityScore()) + 
                                   ", Volatility: " + String.format("%.2f", risk.getVolatilityScore()) + 
                                   ", Fee Impact: " + String.format("%.2f", risk.getFeeImpact()));
                System.out.println("     Market Depth: " + String.format("%.2f", risk.getMarketDepthScore()) + 
                                   ", Execution Speed: " + String.format("%.2f", risk.getExecutionSpeedRisk()) + 
                                   ", Overall Risk: " + String.format("%.2f", risk.getOverallRiskScore()));
            }
        }
    }
}
