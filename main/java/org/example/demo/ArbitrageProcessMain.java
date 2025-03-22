package org.example.demo;

import org.example.data.model.ArbitrageOpportunity;
import org.example.data.model.Ticker;
import org.example.data.model.TradingPair;
import org.example.data.model.RiskAssessment;
import org.example.data.fee.FeeReportGenerator;
import org.example.data.fee.Fee;
import org.example.data.fee.FeeCalculator;
import org.example.data.service.BinanceExchangeService;
import org.example.data.service.BybitV5ExchangeService;
import org.example.data.service.CoinbaseExchangeService;
import org.example.data.service.ExchangeService;
import org.example.data.service.KrakenExchangeService;
import org.example.domain.engine.ExchangeToExchangeArbitrage;
import org.example.domain.risk.RiskCalculator;
import org.example.domain.risk.SlippageAnalyticsBuilder;
import org.example.domain.risk.SlippageManagerService;
import org.example.domain.risk.SlippageStressTester;
import org.example.config.ConfigurationFactory;
import org.example.data.model.ArbitrageConfiguration;
import org.example.data.model.ExchangeConfiguration;
import org.example.data.model.RiskConfiguration;

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

public class ArbitrageProcessMain {
    // Replace hard-coded values with configuration
    private static double MIN_PROFIT_PERCENT;
    private static double AVAILABLE_CAPITAL;
    private static double MAX_POSITION_PERCENT;
    private static double MAX_SLIPPAGE_PERCENT;
    private static boolean ENABLE_FEE_REPORTS;
    
    // Store exchange symbol mappings
    private static Map<ExchangeService, Map<String, String>> exchangeSymbolMap = new HashMap<>();
    
    // Add slippage manager service as a static field
    private static SlippageManagerService slippageManager;

    private static SlippageAnalyticsBuilder slippageAnalytics;

    public static void main(String[] args) {
        System.out.println("=== Starting Real-time Arbitrage Process with WebSocket Data ===");

        // Initialize configuration
        System.out.println("\n[Config] Loading configuration...");
        loadConfiguration();

        // Initialize slippage analytics system
        slippageAnalytics = SlippageAnalyticsBuilder.create();
        slippageManager = slippageAnalytics.getSlippageManager();
        
        // Step 1: Initialize all Exchange Services with advanced fee configurations
        System.out.println("\n[Step 1] Initializing Exchange Services...");
        
        // Use configuration for exchange fees
        ExchangeConfiguration exchangeConfig = ConfigurationFactory.getExchangeConfig();
        BinanceExchangeService binance = new BinanceExchangeService(exchangeConfig.getExchangeFee("binance"));
        CoinbaseExchangeService coinbase = new CoinbaseExchangeService(exchangeConfig.getExchangeFee("coinbase"));
        KrakenExchangeService kraken = new KrakenExchangeService(exchangeConfig.getExchangeFee("kraken"));
        BybitV5ExchangeService bybit = new BybitV5ExchangeService(exchangeConfig.getExchangeFee("bybit"));
        
        // Configure exchange-specific fee structures based on trading volume
        binance.updateFeesTiers(0.0);
        binance.setBnbDiscount(ConfigurationFactory.getBoolean("exchanges.binance.bnbDiscount", false));
        coinbase.updateFeesTiers(0.0);
        kraken.updateFeesTiers(0.0);
        bybit.updateFeesTiers(0.0);

        List<ExchangeService> exchanges = new ArrayList<>();
        // Add only enabled exchanges from configuration
        if (exchangeConfig.isExchangeEnabled("binance")) {
            exchanges.add(binance);
        }
        if (exchangeConfig.isExchangeEnabled("coinbase")) {
            exchanges.add(coinbase);
        }
        if (exchangeConfig.isExchangeEnabled("kraken")) {
            exchanges.add(kraken);
        }
        if (exchangeConfig.isExchangeEnabled("bybit")) {
            exchanges.add(bybit);
        }

        // Step 2: Fetch Trading Pairs from Each Exchange using REST API.
        System.out.println("\n[Step 2] Fetching Trading Pairs from each Exchange...");
        for (ExchangeService ex : exchanges) {
            System.out.println("\n[" + ex.getExchangeName() + "] Fetching Trading Pairs...");
            List<TradingPair> pairs = ex.fetchTradingPairs();
            System.out.println("[" + ex.getExchangeName() + "] Fetched " + pairs.size() + " trading pairs.");
        }

        // Step 3: Determine trading pairs available on at least two exchanges using symbol normalization
        System.out.println("\n[Step 3] Determining trading pairs available on at least two exchanges using symbol normalization...");
        List<String> tradableSymbols = findCommonSymbols(exchanges);
        
        // For demo purposes, limit to a few tradable symbols if there are too many
        int maxSymbolLimit = ConfigurationFactory.getInteger("system.performance.maxSymbolLimit", 100);
        if (tradableSymbols.size() > maxSymbolLimit) {
            tradableSymbols = tradableSymbols.subList(0, maxSymbolLimit);
            System.out.println("Limited to " + maxSymbolLimit + " tradable symbols for demonstration purposes");
        }
        
        // Step 4: Initialize WebSocket connections for all exchanges with proper error handling
        if (!tradableSymbols.isEmpty()) {
            try {
                for (ExchangeService ex : exchanges) {
                    try {
                        List<String> exchangeSpecificSymbols = new ArrayList<>();
                        Map<String, String> symbolMap = exchangeSymbolMap.get(ex);
                        
                        if (symbolMap != null) {
                            for (String normalizedSymbol : tradableSymbols) {
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Step 5: Run the real-time arbitrage process
        System.out.println("\n[Step 5] Starting Real-time Arbitrage Monitoring...");
        System.out.println("Press Ctrl+C to stop the process.\n");

        // Convert the list to a set for the runDirectArbitrageComparison method
        Set<String> tradableSymbolsSet = new HashSet<>(tradableSymbols);

        if (tradableSymbolsSet.isEmpty()) {
            System.out.println("No tradable symbols found. Ending arbitrage process.");
            return;
        }

        // Run initial comparison
        try {
            runDirectArbitrageComparison(exchanges, tradableSymbolsSet);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Schedule periodic arbitrage scan
        schedulePeriodicScans(exchanges, tradableSymbolsSet);
    }
    
    /**
     * Load configuration values from configuration service
     */
    private static void loadConfiguration() {
        // Load arbitrage configuration
        ArbitrageConfiguration arbitrageConfig = ConfigurationFactory.getArbitrageConfig();
        MIN_PROFIT_PERCENT = arbitrageConfig.getMinProfitPercent();
        AVAILABLE_CAPITAL = arbitrageConfig.getAvailableCapital();
        MAX_POSITION_PERCENT = arbitrageConfig.getMaxPositionPercent();
        
        // Load risk configuration
        RiskConfiguration riskConfig = ConfigurationFactory.getRiskConfig();
        MAX_SLIPPAGE_PERCENT = riskConfig.getMaxSlippagePercent();
        
        // Load other settings
        ENABLE_FEE_REPORTS = ConfigurationFactory.getBoolean("system.logging.feeReporting", true);
        
        System.out.println("Configuration loaded successfully:");
        System.out.println("- Min Profit %: " + MIN_PROFIT_PERCENT);
        System.out.println("- Available Capital: $" + AVAILABLE_CAPITAL);
        System.out.println("- Max Position %: " + (MAX_POSITION_PERCENT * 100) + "%");
        System.out.println("- Max Slippage %: " + (MAX_SLIPPAGE_PERCENT * 100) + "%");
    }
    
    /**
     * Prints fee reports for all exchanges.
     * 
     * @param exchanges List of exchange services
     */
    private static void printFeeReports(List<ExchangeService> exchanges) {
        if (!ENABLE_FEE_REPORTS) {
            return;
        }
        
        // Print consolidated fee report
        System.out.println(FeeReportGenerator.generateConsolidatedReport(exchanges));
        
        // Print individual exchange reports
        for (ExchangeService exchange : exchanges) {
            System.out.println("\n===== " + exchange.getExchangeName().toUpperCase() + " FEE SUMMARY =====");
            System.out.println(exchange.getFeeTracker().generateFeeSummaryReport());
        }
    }
    
    /**
     * Runs a direct comparison between exchanges for all common symbols.
     * This provides a more detailed view of the price differences.
     * Considers any pair available on at least two exchanges.
     */
    private static void runDirectArbitrageComparison(List<ExchangeService> exchanges, Set<String> tradableSymbols) {
        if (tradableSymbols.isEmpty()) {
            System.out.println("No tradable symbols to compare between exchanges.");
            return;
        }
        
        // Clear the console for better readability
        System.out.print("\033[H\033[2J");
        System.out.flush();
        
        System.out.println("=== Real-time Arbitrage Opportunities ===\n");
        
        // Create risk calculator and position sizer
        RiskCalculator riskCalculator = new RiskCalculator(MIN_PROFIT_PERCENT / 100);
        
        for (int i = 0; i < exchanges.size(); i++) {
            for (int j = i + 1; j < exchanges.size(); j++) {
                ExchangeService exA = exchanges.get(i);
                ExchangeService exB = exchanges.get(j);
                
                for (String symbol : tradableSymbols) {
                    try {
                        // Check if both exchanges support this symbol
                        if (!exchangeSymbolMap.containsKey(exA) || !exchangeSymbolMap.containsKey(exB)) {
                            continue;
                        }
                        
                        String symbolA = exchangeSymbolMap.get(exA).get(symbol);
                        String symbolB = exchangeSymbolMap.get(exB).get(symbol);
                        
                        // Skip if the symbol is not available on both exchanges
                        if (symbolA == null || symbolB == null) {
                            continue;
                        }
                        
                        TradingPair pair = new TradingPair(symbol);
                        ExchangeToExchangeArbitrage arbitrageCalc = new ExchangeToExchangeArbitrage(exA, exB);
                        ArbitrageOpportunity opportunity = arbitrageCalc.calculateArbitrage(pair);

                        if (opportunity != null && opportunity.getProfitPercent() > MIN_PROFIT_PERCENT) {
                            // Get fee information
                            String buyExchange = opportunity.getExchangeBuy();
                            String sellExchange = opportunity.getExchangeSell();
                            double buyPrice = opportunity.getBuyPrice();
                            double sellPrice = opportunity.getSellPrice();
                            
                            // Determine which exchange service is the buy/sell exchange
                            ExchangeService buyExchangeService = null;
                            ExchangeService sellExchangeService = null;
                            
                            if (buyExchange.equals(exA.getExchangeName())) {
                                buyExchangeService = exA;
                                sellExchangeService = exB;
                            } else {
                                buyExchangeService = exB;
                                sellExchangeService = exA;
                            }
                            
                            // Get fee objects
                            Fee buyFee = buyExchangeService.getMakerFee();
                            Fee sellFee = sellExchangeService.getTakerFee();
                            
                            // Calculate quantity based on price
                            double quantity;
                            if (buyPrice < 0.001) {
                                quantity = 1000000; // 1 million units for micro-priced tokens like SHIB
                            } else if (buyPrice < 1.0) {
                                quantity = 1000; // 1,000 units for tokens under $1
                            } else if (buyPrice < 100.0) {
                                quantity = 10; // 10 units for tokens under $100
                            } else {
                                quantity = 0.01; // 0.01 units for expensive tokens like BTC
                            }
                            
                            // Calculate fee percentages
                            double buyFeePercent = buyFee.calculateFee(buyPrice * quantity) / (buyPrice * quantity) * 100;
                            double sellFeePercent = sellFee.calculateFee(sellPrice * quantity) / (sellPrice * quantity) * 100;
                            double totalFeePercent = buyFeePercent + sellFeePercent;
                            
                            // Calculate raw price difference percentage
                            double priceDiffPercent = ((sellPrice / buyPrice) - 1) * 100;
                            
                            // Get tickers for risk assessment using normalized symbol
                            String buySymbol = opportunity.getNormalizedSymbol();
                            String sellSymbol = opportunity.getNormalizedSymbol();
                            Ticker buyTicker = buyExchangeService.getTickerData(buySymbol);
                            Ticker sellTicker = sellExchangeService.getTickerData(sellSymbol);
                            
                            // Calculate enhanced risk assessment with slippage information
                            RiskAssessment risk = riskCalculator.calculateRisk(
                                buyTicker, 
                                sellTicker, 
                                buyFeePercent / 100, 
                                sellFeePercent / 100
                            );
                            
                            // Add risk assessment to opportunity
                            opportunity.setRiskAssessment(risk);
                            
                            // Set ticker data in the opportunity
                            opportunity.setBuyTicker(buyTicker);
                            opportunity.setSellTicker(sellTicker);
                            
                            // Calculate optimal position size
                            double optimalPositionSize = calculateOptimalPositionSize(
                                opportunity, 
                                AVAILABLE_CAPITAL, 
                                MAX_POSITION_PERCENT
                            );
                            
                            // Calculate expected slippage
                            double tradeSize = optimalPositionSize / buyPrice;
                            double buySlippage = calculateExpectedSlippage(buyTicker, true, tradeSize, buySymbol);
                            double sellSlippage = calculateExpectedSlippage(sellTicker, false, tradeSize, sellSymbol);
                            double totalSlippage = buySlippage + sellSlippage;
                            
                            // Format the opportunity display
                            StringBuilder sb = new StringBuilder();
                            sb.append(String.format(">>> %s: Buy on %s at %s, Sell on %s at %s\n", 
                                opportunity.getNormalizedSymbol(),
                                opportunity.getExchangeBuy(),
                                formatPrice(opportunity.getBuyPrice()),
                                opportunity.getExchangeSell(),
                                formatPrice(opportunity.getSellPrice())));
                            
                            sb.append(String.format("    Profit: %.4f%% | Success Rate: %.2f%%\n", 
                                opportunity.getProfitPercent(),
                                opportunity.getSuccessfulArbitragePercent()));
                            
                            // Add fee information
                            sb.append(String.format("    Fees: Buy: %.4f%% (%s) | Sell: %.4f%% (%s) | Total: %.4f%%\n",
                                buyFeePercent, 
                                buyFee.getDescription(),
                                sellFeePercent,
                                sellFee.getDescription(),
                                totalFeePercent));
                                
                            sb.append(String.format("    Price Diff: %.4f%% | Net After Fees: %.4f%%\n",
                                priceDiffPercent,
                                priceDiffPercent - totalFeePercent));
                            
                            // Add risk assessment if available
                            if (risk != null) {
                                sb.append(String.format("    Risk Score: %.2f | Liquidity: %.2f | Volatility: %.2f\n",
                                    risk.getOverallRiskScore(),
                                    risk.getLiquidityScore(),
                                    risk.getVolatilityScore()));
                                    
                                // Add slippage information
                                sb.append(String.format("    Slippage: Buy: %.4f%% | Sell: %.4f%% | Total: %.4f%%\n",
                                    buySlippage * 100,
                                    sellSlippage * 100,
                                    totalSlippage * 100));
                            }

                            
                            System.out.println(sb.toString());
                        }
                    } catch (Exception e) {
                        System.err.println("Error calculating arbitrage for " + symbol + ": " + e.getMessage());
                    }
                }
            }
        }
        
        System.out.println("\nLast updated: " + new Date());
        System.out.println("=====================================");
    }
    
    /**
     * Calculates the optimal position size for an arbitrage opportunity based on risk assessment.
     * 
     * @param opportunity The arbitrage opportunity to size
     * @param availableCapital Total capital available for trading
     * @param maxPositionPct Maximum position size as percentage of capital
     * @return Optimal position size in base currency units
     */
    private static double calculateOptimalPositionSize(ArbitrageOpportunity opportunity, double availableCapital, double maxPositionPct) {
        // Validate inputs
        if (opportunity == null || opportunity.getRiskAssessment() == null) {
            return 0.0;
        }
        
        RiskAssessment risk = opportunity.getRiskAssessment();
        
        // Extract key risk factors
        double overallRisk = risk.getOverallRiskScore();
        double slippageRisk = risk.getSlippageRisk();
        double liquidityScore = risk.getLiquidityScore();
        double volatilityScore = risk.getVolatilityScore();
        
        // Calculate win probability (using overall risk as a proxy)
        double winProbability = Math.min(0.95, overallRisk * 0.9 + 0.05);
        
        // Calculate potential profit and loss
        double potentialProfit = opportunity.getProfitPercent() / 100.0;
        double potentialLoss = 1.0 - slippageRisk; // Use slippage risk as a proxy for potential loss
        
        // Calculate Kelly fraction (optimal bet size as fraction of capital)
        double kellyFraction = 0.0;
        if (potentialLoss > 0) {
            kellyFraction = (winProbability * (1 + potentialProfit) - 1) / potentialLoss;
        }
        
        // Apply a safety factor (using half Kelly or less is common practice)
        double safetyFactor = 0.5;
        kellyFraction *= safetyFactor;
        
        // Cap the position size
        double cappedFraction = Math.min(kellyFraction, maxPositionPct);
        
        // Apply additional risk-based scaling factors
        double liquidityAdjustment = Math.pow(liquidityScore, 1.5); // Penalize low liquidity more aggressively
        double volatilityAdjustment = Math.pow(volatilityScore, 1.2); // Slightly reduce size for high volatility
        
        // Calculate final position size with all constraints
        double optimalFraction = cappedFraction * liquidityAdjustment * volatilityAdjustment;
        
        // Convert fraction to actual position size
        double positionSize = availableCapital * optimalFraction;
        
        // Implement minimum position size threshold (to avoid dust positions)
        double minimumPositionSize = 10.0; // Example minimum size in base currency
        if (positionSize < minimumPositionSize) {
            return 0.0; // Don't trade if optimal size is too small
        }
        
        return positionSize;
    }
    
    /**
     * Calculates expected slippage for a given trade size.
     * This version uses the advanced slippage calculator with dynamic calibration.
     * 
     * @param ticker The ticker data
     * @param isBuy Whether this is a buy (true) or sell (false) operation
     * @param tradeAmount The size of the trade to execute
     * @param symbol The trading symbol
     * @return Expected slippage as a percentage of the trade value
     */
    private static double calculateExpectedSlippage(Ticker ticker, boolean isBuy, double tradeAmount, String symbol) {
        try {
            if (slippageManager != null) {
                // Use the advanced slippage calculation
                return slippageManager.calculateSlippage(ticker, tradeAmount, isBuy, symbol);
            } else {
                // Fallback to basic calculation if the manager isn't initialized
                return calculateBasicSlippage(ticker, isBuy, tradeAmount);
            }
        } catch (Exception e) {
            System.err.println("Error calculating slippage: " + e.getMessage());
            // Fallback to basic calculation
            return calculateBasicSlippage(ticker, isBuy, tradeAmount);
        }
    }
    
    /**
     * Fallback method for basic slippage calculation when advanced analytics are unavailable.
     */
    private static double calculateBasicSlippage(Ticker ticker, boolean isBuy, double tradeAmount) {
        if (ticker == null) {
            return 0.005; // Default 0.5% slippage if no market data
        }
        
        double spread = ticker.getAskPrice() - ticker.getBidPrice();
        if (spread <= 0 || ticker.getLastPrice() <= 0) {
            return 0.005; // Default to 0.5% if invalid prices
        }
        
        double relativeSpread = spread / ticker.getLastPrice();
        
        // Basic volume-based adjustment (higher volume = lower slippage)
        double volumeAdjustment = 1.0;
        if (ticker.getVolume() > 0) {
            // Normalize the trade amount relative to 24h volume
            double volumeRatio = tradeAmount / ticker.getVolume();
            volumeAdjustment = Math.min(1.0 + (volumeRatio * 10), 3.0); // Cap at 3x
        }
        
        // Calculate slippage based on spread and volume
        double baseSlippage = relativeSpread * 0.5 * volumeAdjustment;
        
        // Ensure slippage is within reasonable bounds (0.05% to 2%)
        return Math.max(0.0005, Math.min(baseSlippage, 0.02));
    }

    /**
     * Formats price values with appropriate scientific notation for small values
     */
    private static String formatPrice(double price) {
        if (price < 0.001) {
            return String.format("%.4E", price);
        } else {
            return String.format("%.8f", price);
        }
    }

    /**
     * Normalizes a symbol to a standard format.
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
     * Find trading symbols available on at least two exchanges using symbol normalization.
     * This enhances arbitrage opportunities by considering any pair traded on at least two exchanges.
     */
    private static List<String> findCommonSymbols(List<ExchangeService> exchangeServices) {
        if (exchangeServices.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Create maps to store normalized symbol -> original symbol for each exchange
        Map<ExchangeService, Map<String, String>> exchangeSymbolMaps = new HashMap<>();
        
        // Track which exchanges support each normalized symbol
        Map<String, Set<ExchangeService>> symbolExchangeMap = new HashMap<>();
        
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
                
                // Track this exchange as supporting this symbol
                symbolExchangeMap.computeIfAbsent(normalizedSymbol, k -> new HashSet<>()).add(exchange);
            }
            
            exchangeSymbolMaps.put(exchange, normalizedMap);
            System.out.println("Found " + normalizedMap.size() + " trading pairs for " + exchange.getExchangeName());
        }
        
        // Find symbols available on at least two exchanges
        Set<String> validArbitrageSymbols = new HashSet<>();
        
        for (Map.Entry<String, Set<ExchangeService>> entry : symbolExchangeMap.entrySet()) {
            String symbol = entry.getKey();
            Set<ExchangeService> supportingExchanges = entry.getValue();
            
            if (supportingExchanges.size() >= 2) {
                validArbitrageSymbols.add(symbol);
            }
        }
        
        if (validArbitrageSymbols.isEmpty()) {
            System.out.println("No symbols found available on at least two exchanges after normalization");
            return new ArrayList<>();
        }
        
        System.out.println("Found " + validArbitrageSymbols.size() + " symbols available on at least two exchanges");
        
        // Create mapping of normalized symbols to original symbols for each exchange
        // This will help us later when we need to query data for specific exchange formats
        exchangeSymbolMap.clear(); // Clear any previous mappings
        
        for (String normalizedSymbol : validArbitrageSymbols) {
            StringBuilder sb = new StringBuilder("Found tradable symbol: " + normalizedSymbol + " on exchanges: (");
            boolean first = true;
            Set<ExchangeService> exchanges = symbolExchangeMap.get(normalizedSymbol);
            
            for (ExchangeService exchange : exchanges) {
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
        
        return new ArrayList<>(validArbitrageSymbols);
    }

    /**
     * Schedules periodic arbitrage scans.
     */
    private static void schedulePeriodicScans(List<ExchangeService> exchanges, Set<String> tradableSymbols) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        
        // Get scan interval from configuration
        int scanInterval = ConfigurationFactory.getInteger("system.scheduling.arbitrageScanInterval", 5000);
        
        Runnable task = () -> {
            try {
                System.out.println("\n[" + new Date() + "] Scanning for arbitrage opportunities...");
                runDirectArbitrageComparison(exchanges, tradableSymbols);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        
        executor.scheduleAtFixedRate(task, scanInterval, scanInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * Runs a comparison using static data for testing.
     */
    private static void runDirectComparisonStatic(List<ExchangeService> exchanges, List<String> tradableSymbols) {
        System.out.println("\n=== Running Direct Exchange-to-Exchange Comparison ===");
        
        if (tradableSymbols == null || tradableSymbols.isEmpty()) {
            System.out.println("No tradable symbols to compare between exchanges.");
            return;
        }
        
        List<ArbitrageOpportunity> opportunities = new ArrayList<>();
        
        for (String normalizedSymbol : tradableSymbols) {
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
                        // We'll use the taker fee for both since market orders are typical for arbitrage execution
                        double buyPrice = tickerA.getAskPrice();
                        double sellPrice = tickerB.getBidPrice();
                        
                        // Dynamically adjust quantity based on token price
                        double quantity;
                        if (buyPrice < 0.001) {
                            quantity = 1000000; // 1 million units for micro-priced tokens like SHIB
                        } else if (buyPrice < 1.0) {
                            quantity = 1000; // 1,000 units for tokens under $1
                        } else if (buyPrice < 100.0) {
                            quantity = 10; // 10 units for tokens under $100
                        } else {
                            quantity = 0.01; // 0.01 units for expensive tokens like BTC
                        }
                        
                        // Use the new fee system to calculate fees for both exchanges
                        Fee buyFee = exchangeA.getTakerFee();
                        Fee sellFee = exchangeB.getTakerFee();
                        
                        // Calculate profit using the FeeCalculator
                        double profit = FeeCalculator.calculateArbitrageProfit(buyPrice, sellPrice, quantity, buyFee, sellFee);
                        double profitPercent = FeeCalculator.calculateArbitrageProfitPercentage(buyPrice, sellPrice, quantity, buyFee, sellFee);
                        
                        if (profitPercent > MIN_PROFIT_PERCENT) {
                            // Create risk calculator for this opportunity
                            RiskCalculator riskCalc = new RiskCalculator(MIN_PROFIT_PERCENT / 100);
                            
                            // Calculate risk assessment
                            // We'll pass the fee objects instead of just the fee percentages
                            RiskAssessment riskAssessment = riskCalc.calculateRisk(tickerA, tickerB, 
                                    buyFee.calculateFee(buyPrice * quantity) / (buyPrice * quantity), 
                                    sellFee.calculateFee(sellPrice * quantity) / (sellPrice * quantity));
                            
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
                            
                            // Set ticker data in the opportunity
                            opportunity.setBuyTicker(tickerA);
                            opportunity.setSellTicker(tickerB);
                            
                            // Calculate optimal position size
                            double optimalPositionSize = calculateOptimalPositionSize(
                                opportunity, 
                                AVAILABLE_CAPITAL, 
                                MAX_POSITION_PERCENT
                            );
                            
                            // Calculate expected slippage
                            double tradeSize = optimalPositionSize / buyPrice;
                            double buySlippage = calculateExpectedSlippage(tickerA, true, tradeSize, symbolA);
                            double sellSlippage = calculateExpectedSlippage(tickerB, false, tradeSize, symbolB);
                            double totalSlippage = buySlippage + sellSlippage;
                            
                            opportunities.add(opportunity);
                            
                            // Log detailed information about the opportunity
                            StringBuilder sb = new StringBuilder();
                            sb.append(String.format("Found opportunity: %s\n", normalizedSymbol));
                            sb.append(String.format("  Buy on %s at %s, Sell on %s at %s\n", 
                                    exchangeA.getExchangeName(), formatPrice(buyPrice), 
                                    exchangeB.getExchangeName(), formatPrice(sellPrice)));
                            
                            // Calculate fee percentages for better visibility
                            double buyFeePercent = buyFee.calculateFee(buyPrice * quantity) / (buyPrice * quantity) * 100;
                            double sellFeePercent = sellFee.calculateFee(sellPrice * quantity) / (sellPrice * quantity) * 100;
                            double totalFeePercent = buyFeePercent + sellFeePercent;
                            
                            // Calculate raw price difference percentage
                            double priceDiffPercent = ((sellPrice / buyPrice) - 1) * 100;
                            
                            // Verify that the profit calculation is consistent
                            double verifiedNetProfit = priceDiffPercent - totalFeePercent;
                            
                            sb.append(String.format("  Price difference: %.4f%%\n", priceDiffPercent));
                            sb.append(String.format("  Fee impact: Buy fee %.4f%%, Sell fee %.4f%% (total: %.4f%%)\n",
                                    buyFeePercent, sellFeePercent, totalFeePercent));
                            
                            // Add slippage information
                            sb.append(String.format("  Slippage: Buy: %.4f%%, Sell: %.4f%% (total: %.4f%%)\n",
                                    buySlippage * 100, sellSlippage * 100, totalSlippage * 100));
                                    
                            double netProfitAfterAll = priceDiffPercent - totalFeePercent - (totalSlippage * 100);
                            sb.append(String.format("  Net profit after fees and slippage: %.4f%%\n", netProfitAfterAll));
                            
                            sb.append(String.format("  Success probability: %.2f%%\n", opportunity.getSuccessfulArbitragePercent()));
                            
                            // Add position sizing information
                            sb.append(String.format("  Optimal position: $%.2f | Units: %.4f\n",
                                optimalPositionSize,
                                optimalPositionSize / buyPrice));
                                
                            System.out.println(sb.toString());
                            
                            // Track the fees for this potential trade
                            exchangeA.calculateAndTrackFee(symbolA, buyPrice * quantity, false);
                            exchangeB.calculateAndTrackFee(symbolB, sellPrice * quantity, false);
                            
                            // Only add to opportunities list if slippage is acceptable
                            if (totalSlippage * 100 < MAX_SLIPPAGE_PERCENT) {
                                opportunities.add(opportunity);
                            } else {
                                System.out.println("  ⚠ Excessive slippage! Opportunity excluded from top list.");
                            }
                        }
                        
                        // Check for arbitrage opportunity B -> A (buy on B, sell on A)
                        buyPrice = tickerB.getAskPrice();
                        sellPrice = tickerA.getBidPrice();
                        
                        // Swap the fees for the reverse direction
                        buyFee = exchangeB.getTakerFee();
                        sellFee = exchangeA.getTakerFee();
                        
                        // Calculate profit using the FeeCalculator
                        profit = FeeCalculator.calculateArbitrageProfit(buyPrice, sellPrice, quantity, buyFee, sellFee);
                        profitPercent = FeeCalculator.calculateArbitrageProfitPercentage(buyPrice, sellPrice, quantity, buyFee, sellFee);
                        
                        if (profitPercent > MIN_PROFIT_PERCENT) {
                            // Create risk calculator for this opportunity
                            RiskCalculator riskCalc = new RiskCalculator(MIN_PROFIT_PERCENT / 100);
                            
                            // Calculate risk assessment with fee objects
                            RiskAssessment riskAssessment = riskCalc.calculateRisk(tickerB, tickerA, 
                                    buyFee.calculateFee(buyPrice * quantity) / (buyPrice * quantity), 
                                    sellFee.calculateFee(sellPrice * quantity) / (sellPrice * quantity));
                            
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
                            
                            // Set ticker data in the opportunity
                            opportunity.setBuyTicker(tickerB);
                            opportunity.setSellTicker(tickerA);
                            
                            // Calculate optimal position size
                            double optimalPositionSize = calculateOptimalPositionSize(
                                opportunity, 
                                AVAILABLE_CAPITAL, 
                                MAX_POSITION_PERCENT
                            );
                            
                            // Calculate expected slippage
                            double tradeSize = optimalPositionSize / buyPrice;
                            double buySlippage = calculateExpectedSlippage(tickerB, true, tradeSize, symbolB);
                            double sellSlippage = calculateExpectedSlippage(tickerA, false, tradeSize, symbolA);
                            double totalSlippage = buySlippage + sellSlippage;
                            
                            opportunities.add(opportunity);
                            
                            // Log detailed information about the opportunity
                            StringBuilder sb = new StringBuilder();
                            sb.append(String.format("Found opportunity: %s\n", normalizedSymbol));
                            sb.append(String.format("  Buy on %s at %s, Sell on %s at %s\n", 
                                    exchangeB.getExchangeName(), formatPrice(buyPrice), 
                                    exchangeA.getExchangeName(), formatPrice(sellPrice)));
                            
                            // Calculate fee percentages for better visibility
                            double buyFeePercent = buyFee.calculateFee(buyPrice * quantity) / (buyPrice * quantity) * 100;
                            double sellFeePercent = sellFee.calculateFee(sellPrice * quantity) / (sellPrice * quantity) * 100;
                            double totalFeePercent = buyFeePercent + sellFeePercent;
                            
                            // Calculate raw price difference percentage
                            double priceDiffPercent = ((sellPrice / buyPrice) - 1) * 100;
                            
                            // Verify that the profit calculation is consistent
                            double verifiedNetProfit = priceDiffPercent - totalFeePercent;
                            
                            sb.append(String.format("  Price difference: %.4f%%\n", priceDiffPercent));
                            sb.append(String.format("  Fee impact: Buy fee %.4f%%, Sell fee %.4f%% (total: %.4f%%)\n",
                                    buyFeePercent, sellFeePercent, totalFeePercent));
                            sb.append(String.format("  Net profit after fees: %.4f%%\n", verifiedNetProfit));
                            sb.append(String.format("  Success probability: %.2f%%\n", opportunity.getSuccessfulArbitragePercent()));
                            System.out.println(sb.toString());
                            
                            // Track the fees for this potential trade
                            exchangeB.calculateAndTrackFee(symbolB, buyPrice * quantity, false);
                            exchangeA.calculateAndTrackFee(symbolA, sellPrice * quantity, false);
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
                                   ", Slippage Risk: " + String.format("%.2f", risk.getSlippageRisk()));
                System.out.println("     Overall Risk: " + String.format("%.2f", risk.getOverallRiskScore()));
                
                // Add position size information
                double optimalPositionSize = calculateOptimalPositionSize(
                    opportunity, 
                    AVAILABLE_CAPITAL, 
                    MAX_POSITION_PERCENT
                );
                
                System.out.println("   Position Sizing:");
                System.out.println("     Optimal Position: $" + String.format("%.2f", optimalPositionSize));
                System.out.println("     Units to Trade: " + String.format("%.6f", optimalPositionSize / opportunity.getBuyPrice()));
                
                // Print detailed fee information
                String buyExchange = opportunity.getExchangeBuy();
                String sellExchange = opportunity.getExchangeSell();
                ExchangeService buyExchangeService = exchanges.stream()
                    .filter(e -> e.getExchangeName().equals(buyExchange))
                    .findFirst().orElse(null);
                ExchangeService sellExchangeService = exchanges.stream()
                    .filter(e -> e.getExchangeName().equals(sellExchange))
                    .findFirst().orElse(null);
                
                if (buyExchangeService != null && sellExchangeService != null) {
                    System.out.println("   Fee Information:");
                    System.out.println("     Buy Exchange: " + buyExchange + " - Taker Fee: " + 
                                      buyExchangeService.getTakerFee().getDescription());
                    System.out.println("     Sell Exchange: " + sellExchange + " - Taker Fee: " + 
                                      sellExchangeService.getTakerFee().getDescription());
                }
            }
            
            // If fee reporting is enabled, show a fee summary for the discovered opportunities
            if (ENABLE_FEE_REPORTS) {
                System.out.println("\n=== Fee Impact on Arbitrage Opportunities ===");
                System.out.println("Fees can significantly impact arbitrage profitability. Current fee structures:");
                for (ExchangeService exchange : exchanges) {
                    System.out.println(exchange.getExchangeName() + ": Maker " + 
                                      exchange.getMakerFee().getDescription() + ", Taker " + 
                                      exchange.getTakerFee().getDescription());
                }
            }
        }
    }

    /**
     * Process arbitrage opportunities with enhanced slippage calculation and analytics.
     */
    private static void processArbitrageOpportunities(List<ArbitrageOpportunity> opportunities) {
        if (opportunities == null || opportunities.isEmpty()) {
            System.out.println("No opportunities to process.");
            return;
        }
        
        // Update volatility tracking with latest data
        updateVolatilityTracking(opportunities);
        
        // Occasionally run stress tests (e.g., every 100 calls or on startup)
        if (Math.random() < 0.01) {  // 1% chance to run stress test
            performSlippageStressTest(opportunities);
        }
        
        // Process each opportunity
        for (ArbitrageOpportunity opportunity : opportunities) {
            // Calculate expected slippage for both buy and sell sides
            if (opportunity.getBuyTicker() != null && opportunity.getSellTicker() != null) {
                // Calculate optimal position size
                double optimalPositionSize = calculateOptimalPositionSize(
                    opportunity, 
                    AVAILABLE_CAPITAL, 
                    MAX_POSITION_PERCENT
                );
                
                // Calculate trade sizes
                double buyTradeSize = optimalPositionSize / opportunity.getBuyPrice();
                double sellTradeSize = buyTradeSize; // Assuming 1:1 trade ratio
                
                // Calculate expected slippage
                double buySlippage = calculateExpectedSlippage(
                    opportunity.getBuyTicker(), 
                    true, 
                    buyTradeSize, 
                    opportunity.getBuySymbol()
                );
                
                double sellSlippage = calculateExpectedSlippage(
                    opportunity.getSellTicker(), 
                    false, 
                    sellTradeSize, 
                    opportunity.getSellSymbol()
                );
                
                // Store slippage estimates in the opportunity
                opportunity.setBuySlippage(buySlippage);
                opportunity.setSellSlippage(sellSlippage);
                
                // Update risk assessment with slippage information
                if (opportunity.getRiskAssessment() != null) {
                    opportunity.getRiskAssessment().setSlippageRisk(buySlippage + sellSlippage);
                }
                
                // If opportunity is good enough to execute, record it for the feedback loop
                double totalSlippage = buySlippage + sellSlippage;
                double netProfitAfterAll = opportunity.getProfitPercent() - (totalSlippage * 100);
                
                if (netProfitAfterAll > MIN_PROFIT_PERCENT && optimalPositionSize > 0) {
                    // In a real system, this would be followed by trade execution
                    String tradeId = "arb-" + System.currentTimeMillis();
                    recordPendingTrade(tradeId, opportunity, buyTradeSize, sellTradeSize);
                    
                    // After execution, you would call:
                    // recordTradeExecution(tradeId, 
                    //                     opportunity.getBuyPrice(), actualBuyPrice,
                    //                     opportunity.getSellPrice(), actualSellPrice);
                }
            }
        }
    }
    
    /**
     * Perform stress testing on slippage calculation system using current market data.
     * This helps validate that our slippage estimates remain reliable under various market conditions.
     */
    private static void performSlippageStressTest(List<ArbitrageOpportunity> opportunities) {
        if (slippageAnalytics == null || opportunities.isEmpty()) {
            return;
        }
        
        System.out.println("\n==== SLIPPAGE STRESS TEST ====");
        System.out.println("Running stress tests to validate slippage calculations...");
        
        // Use the first opportunity's tickers as base data for stress testing
        ArbitrageOpportunity firstOpp = opportunities.get(0);
        if (firstOpp != null && firstOpp.getBuyTicker() != null && firstOpp.getSellTicker() != null) {
            String buySymbol = firstOpp.getBuySymbol();
            String sellSymbol = firstOpp.getSellSymbol();
            
            // Perform stress test for buy side
            SlippageStressTester.StressTestReport buyReport = 
                slippageAnalytics.performStressTest(buySymbol, firstOpp.getBuyTicker());
            
            // Perform stress test for sell side
            SlippageStressTester.StressTestReport sellReport = 
                slippageAnalytics.performStressTest(sellSymbol, firstOpp.getSellTicker());
            
            // Display results
            System.out.println("\nBuy Side (Symbol: " + buySymbol + "):");
            System.out.println(buyReport.toString());
            
            System.out.println("\nSell Side (Symbol: " + sellSymbol + "):");
            System.out.println(sellReport.toString());
        }
        
        System.out.println("==== END STRESS TEST ====\n");
    }
    
    /**
     * Update our volatility tracking with the latest price data.
     * This helps improve slippage estimates by accounting for market volatility.
     */
    private static void updateVolatilityTracking(List<ArbitrageOpportunity> opportunities) {
        if (slippageAnalytics == null) {
            return;
        }
        
        long currentTimestamp = System.currentTimeMillis();
        
        for (ArbitrageOpportunity opportunity : opportunities) {
            if (opportunity.getBuyTicker() != null) {
                slippageAnalytics.updateVolatility(
                    opportunity.getBuySymbol(),
                    opportunity.getBuyTicker().getLastPrice(),
                    currentTimestamp
                );
            }
            
            if (opportunity.getSellTicker() != null) {
                slippageAnalytics.updateVolatility(
                    opportunity.getSellSymbol(),
                    opportunity.getSellTicker().getLastPrice(),
                    currentTimestamp
                );
            }
        }
    }
    
    /**
     * Records a pending trade for slippage analysis feedback loop.
     */
    private static void recordPendingTrade(String tradeId, ArbitrageOpportunity opportunity, 
                                          double buySize, double sellSize) {
        if (slippageAnalytics == null || opportunity == null) {
            return;
        }
        
        // Record buy side
        if (opportunity.getBuyTicker() != null) {
            slippageAnalytics.recordPendingTrade(
                tradeId + "-buy",
                opportunity.getBuySymbol(),
                buySize,
                true,
                opportunity.getBuySlippage()
            );
        }
        
        // Record sell side
        if (opportunity.getSellTicker() != null) {
            slippageAnalytics.recordPendingTrade(
                tradeId + "-sell",
                opportunity.getSellSymbol(),
                sellSize,
                false,
                opportunity.getSellSlippage()
            );
        }
    }
    
    /**
     * Records actual trade execution results for slippage analysis feedback loop.
     * In a real system, this would be called after receiving execution confirmation.
     */
    private static void recordTradeExecution(String tradeId, 
                                            double buyExpectedPrice, double buyActualPrice,
                                            double sellExpectedPrice, double sellActualPrice) {
        if (slippageAnalytics == null) {
            return;
        }
        
        // Record buy execution
        slippageAnalytics.recordTradeExecution(
            tradeId + "-buy",
            buyActualPrice,
            buyExpectedPrice
        );
        
        // Record sell execution
        slippageAnalytics.recordTradeExecution(
            tradeId + "-sell",
            sellActualPrice,
            sellExpectedPrice
        );
    }
}
