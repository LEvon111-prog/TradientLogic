package org.example.domain.engine;

import org.example.data.model.ArbitrageOpportunity;
import org.example.data.model.TradingPair;
import org.example.data.service.ExchangeService;
import org.example.data.interfaces.IArbitrageEngine;
import org.example.data.interfaces.IExchangeService;
import org.example.data.interfaces.INotificationService;
import org.example.data.interfaces.IRiskManager;
import org.example.data.interfaces.ArbitrageResult;
import org.example.domain.risk.RiskCalculator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main engine for arbitrage detection across multiple exchanges.
 * This class coordinates the detection of arbitrage opportunities across
 * all configured exchanges and provides methods to scan for and analyze them.
 */
public class ArbitrageEngine implements IArbitrageEngine {
    
    private final List<ExchangeService> exchanges;
    private double minProfitThreshold;
    private final RiskCalculator riskCalculator;
    private final INotificationService notificationService;
    
    /**
     * Constructor with notification service.
     *
     * @param minProfitThreshold Minimum profit threshold for opportunities
     * @param riskCalculator Risk calculator for opportunity assessment
     * @param notificationService Notification service for logging
     */
    public ArbitrageEngine(double minProfitThreshold, 
                         RiskCalculator riskCalculator,
                         INotificationService notificationService) {
        this.exchanges = new ArrayList<>();
        this.minProfitThreshold = minProfitThreshold;
        this.riskCalculator = riskCalculator;
        this.notificationService = notificationService;
    }
    
    /**
     * Simple constructor without notification service.
     *
     * @param minProfitThreshold Minimum profit threshold for opportunities
     * @param riskCalculator Risk calculator for opportunity assessment
     */
    public ArbitrageEngine(double minProfitThreshold, RiskCalculator riskCalculator) {
        this(minProfitThreshold, riskCalculator, null);
    }
    
    @Override
    public void addExchange(ExchangeService exchange) {
        if (exchange != null && !exchanges.contains(exchange)) {
            exchanges.add(exchange);
            logInfo("Added exchange: " + exchange.getExchangeName());
        }
    }
    
    @Override
    public void removeExchange(ExchangeService exchange) {
        if (exchanges.remove(exchange)) {
            logInfo("Removed exchange: " + exchange.getExchangeName());
        }
    }
    
    @Override
    public List<ExchangeService> getExchanges() {
        return new ArrayList<>(exchanges);
    }
    
    @Override
    public void setMinProfitThreshold(double threshold) {
        this.minProfitThreshold = threshold;
        logInfo("Set minimum profit threshold to: " + threshold + "%");
    }
    
    @Override
    public ArbitrageResult scanForOpportunities() {
        // Check if exchanges are configured
        if (exchanges.isEmpty()) {
            logWarning("No exchanges configured for arbitrage scanning");
            return new ArbitrageResultImpl(new ArrayList<>());
        }
        
        // Map to track which exchanges support each trading pair
        Map<TradingPair, Set<ExchangeService>> pairExchangeMap = new HashMap<>();
        
        // Collect all trading pairs from all exchanges and track supporting exchanges
        for (ExchangeService exchange : exchanges) {
            List<TradingPair> exchangePairs = exchange.getTradingPairs();
            if (exchangePairs == null) {
                continue;
            }
            
            for (TradingPair pair : exchangePairs) {
                pairExchangeMap.computeIfAbsent(pair, k -> new HashSet<>()).add(exchange);
            }
        }
        
        // Filter to pairs available on at least two exchanges (viable for arbitrage)
        List<TradingPair> viablePairs = pairExchangeMap.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= 2)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        logInfo("Found " + viablePairs.size() + " trading pairs available on at least two exchanges");
        
        return scanForOpportunities(viablePairs);
    }
    
    @Override
    public ArbitrageResult scanForOpportunities(List<TradingPair> pairs) {
        List<ArbitrageOpportunity> opportunities = new ArrayList<>();

        logInfo("Scanning for arbitrage opportunities across " + exchanges.size() + 
                " exchanges for " + pairs.size() + " trading pairs");
        
        // Create arbitrage engines for each exchange pair
        for (int i = 0; i < exchanges.size(); i++) {
            for (int j = i + 1; j < exchanges.size(); j++) {
                ExchangeService exchangeA = exchanges.get(i);
                ExchangeService exchangeB = exchanges.get(j);
                
                ExchangeToExchangeArbitrage arbitrageEngine = new ExchangeToExchangeArbitrage(
                        exchangeA, exchangeB, riskCalculator, minProfitThreshold, notificationService);
                
                // Scan each trading pair
                for (TradingPair pair : pairs) {
                    ArbitrageOpportunity opportunity = arbitrageEngine.calculateArbitrage(pair);
                        if (opportunity != null) {
                            opportunities.add(opportunity);
                        }
                    }
                }
            }
        
        logInfo("Found " + opportunities.size() + " arbitrage opportunities");
        return new ArbitrageResultImpl(opportunities);
    }

    @Override
    public ArbitrageOpportunity calculateArbitrage(
            IExchangeService fromExchange, IExchangeService toExchange,
            String tradingPair, double amount, IRiskManager riskManager,
            INotificationService notificationService) {
        
        // Cast to ExchangeService if possible
        if (!(fromExchange instanceof ExchangeService) || !(toExchange instanceof ExchangeService)) {
            logError("Cannot calculate arbitrage - unsupported exchange type", 
                    new IllegalArgumentException("Exchanges must be instance of ExchangeService"));
            return null;
        }
        
        ExchangeService from = (ExchangeService) fromExchange;
        ExchangeService to = (ExchangeService) toExchange;
        
        // Create a temporary arbitrage engine for these exchanges
        ExchangeToExchangeArbitrage arbitrageEngine = new ExchangeToExchangeArbitrage(
                from, to, (RiskCalculator) riskManager, minProfitThreshold, notificationService);
        
        // Calculate arbitrage using the dedicated engine
        return arbitrageEngine.calculateArbitrage(
                fromExchange, toExchange, tradingPair, amount, riskManager, notificationService);
    }
    
    /**
     * Log an info message if notification service is available.
     *
     * @param message The message to log
     */
    private void logInfo(String message) {
        if (notificationService != null) {
            notificationService.logInfo("ArbitrageEngine: " + message);
        }
    }
    
    /**
     * Log a warning message if notification service is available.
     *
     * @param message The message to log
     */
    private void logWarning(String message) {
        if (notificationService != null) {
            notificationService.logWarning("ArbitrageEngine: " + message);
        }
    }
    
    /**
     * Log an error message if notification service is available.
     *
     * @param message The message to log
     * @param error The exception associated with the error
     */
    private void logError(String message, Throwable error) {
        if (notificationService != null) {
            notificationService.logError("ArbitrageEngine: " + message, error);
        }
    }
}
