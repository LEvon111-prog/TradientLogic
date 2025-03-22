package org.example.domain.engine;

import org.example.data.model.ArbitrageOpportunity;
import org.example.data.model.OrderBook;
import org.example.data.model.Ticker;
import org.example.data.model.TradingPair;
import org.example.data.fee.Fee;
import org.example.data.fee.FeeCalculator;
import org.example.data.fee.TransactionFee;
import org.example.data.service.ExchangeService;
import org.example.data.interfaces.*;
import org.example.domain.risk.RiskCalculator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class encapsulates the logic for detecting an arbitrage opportunity
 * between two exchanges for a given trading pair, taking fees into account.
 * All log statements have been removed and replaced with proper return values.
 */
public class ExchangeToExchangeArbitrage implements IArbitrageEngine {
    private final ExchangeService exchangeA;
    private final ExchangeService exchangeB;
    private final RiskCalculator riskCalculator;
    private double minProfitPercent;
    private final INotificationService notificationService;
    
    // Track configured exchanges
    private final List<ExchangeService> exchanges;
    
    // Minimum success rate to consider an arbitrage opportunity viable
    private static final int MINIMUM_SUCCESS_RATE = 70;

    /**
     * Constructor with notification service for detailed logging if needed
     * 
     * @param exchangeA First exchange service
     * @param exchangeB Second exchange service
     * @param riskCalculator Risk calculator for opportunity assessment
     * @param minProfitPercent Minimum profit percentage to consider
     * @param notificationService Notification service for logging
     */
    public ExchangeToExchangeArbitrage(ExchangeService exchangeA, ExchangeService exchangeB, 
                                     RiskCalculator riskCalculator, double minProfitPercent,
                                     INotificationService notificationService) {
        this.exchangeA = exchangeA;
        this.exchangeB = exchangeB;
        this.riskCalculator = riskCalculator;
        this.minProfitPercent = minProfitPercent;
        this.notificationService = notificationService;
        
        // Initialize exchanges list
        this.exchanges = new ArrayList<>();
        this.exchanges.add(exchangeA);
        this.exchanges.add(exchangeB);
    }
    
    /**
     * Constructor without notification service for simpler usage
     * 
     * @param exchangeA First exchange service
     * @param exchangeB Second exchange service
     * @param riskCalculator Risk calculator for opportunity assessment
     * @param minProfitPercent Minimum profit percentage to consider
     */
    public ExchangeToExchangeArbitrage(ExchangeService exchangeA, ExchangeService exchangeB,
                                     RiskCalculator riskCalculator, double minProfitPercent) {
        this(exchangeA, exchangeB, riskCalculator, minProfitPercent, null);
    }
    
    /**
     * Simple constructor for backward compatibility
     * 
     * @param exchangeA First exchange service
     * @param exchangeB Second exchange service
     */
    public ExchangeToExchangeArbitrage(ExchangeService exchangeA, ExchangeService exchangeB) {
        this(exchangeA, exchangeB, new RiskCalculator(0.1 / 100), 0.1, null);
    }

    /**
     * Calculates the potential arbitrage opportunity between two exchanges for a given trading pair.
     *
     * @param pair The trading pair to analyze.
     * @return An ArbitrageOpportunity object if an opportunity exists, null otherwise.
     */
    public ArbitrageOpportunity calculateArbitrage(TradingPair pair) {
        if (pair == null) {
            logDebug("Trading pair is null; aborting arbitrage calculation.");
            return null;
        }

        String symbol = pair.getSymbol();
        logDebug("Analyzing arbitrage for symbol: " + symbol);

        // Get ticker data from both exchanges
        Ticker tickerA = exchangeA.getTickerData(symbol);
        Ticker tickerB = exchangeB.getTickerData(symbol);

        if (tickerA == null || tickerB == null) {
            logDebug("Insufficient ticker data from one or both exchanges.");
            return null;  // Can't compare if we don't have data from both exchanges
        }

        // Get order books for maker/taker determination
        OrderBook orderBookA = exchangeA.getOrderBook(symbol);
        OrderBook orderBookB = exchangeB.getOrderBook(symbol);

        if (orderBookA == null || orderBookB == null) {
            logDebug("Order book data missing; using taker fees for both sides.");
        }

        // Determine appropriate quantity based on token price
        double buyOnAPrice = tickerA.getAskPrice();
        double quantity = determineAppropriateQuantity(buyOnAPrice);

        try {
        // Case 1: Buy on A, sell on B
        double sellOnBPrice = tickerB.getBidPrice();

            // Determine if these would be maker or taker orders
            // For buying at the ask price, it's typically a taker order
            boolean isBuyMakerA = false; // Buying at market price is almost always a taker
            
            // For selling at the bid price, it's typically a taker order
            boolean isSellMakerB = false; // Selling at market price is almost always a taker
            
            // Get appropriate fees based on maker/taker status
            Fee buyFeeA = isBuyMakerA ? exchangeA.getMakerFee() : exchangeA.getTakerFee();
            Fee sellFeeB = isSellMakerB ? exchangeB.getMakerFee() : exchangeB.getTakerFee();
    
            // Calculate net profit after fees using the FeeCalculator
            double profitAB = FeeCalculator.calculateArbitrageProfit(buyOnAPrice, sellOnBPrice, quantity, buyFeeA, sellFeeB);
            double profitPercentAB = FeeCalculator.calculateArbitrageProfitPercentage(buyOnAPrice, sellOnBPrice, quantity, buyFeeA, sellFeeB);
    
            // Calculate fee percentages directly from fee amounts
            double buyFeeAmountA = buyFeeA.calculateFee(buyOnAPrice * quantity);
            double sellFeeAmountB = sellFeeB.calculateFee(sellOnBPrice * quantity);
            
            double buyFeePercentA = (buyOnAPrice * quantity > 0) ? (buyFeeAmountA / (buyOnAPrice * quantity) * 100) : 0;
            double sellFeePercentB = (sellOnBPrice * quantity > 0) ? (sellFeeAmountB / (sellOnBPrice * quantity) * 100) : 0;
    
            String buyFeeTypeA = isBuyMakerA ? "Maker" : "Taker";
            String sellFeeTypeB = isSellMakerB ? "Maker" : "Taker";
            
            logDebug("Buy on " + exchangeA.getExchangeName() + " at " + formatPrice(buyOnAPrice)
                    + " (" + buyFeeTypeA + " fee: " + String.format("%.4f", buyFeePercentA) + "%)"
                    + ", Sell on " + exchangeB.getExchangeName() + " at " + formatPrice(sellOnBPrice)
                    + " (" + sellFeeTypeB + " fee: " + String.format("%.4f", sellFeePercentB) + "%)"
                    + " = " + String.format("%.4f", profitPercentAB)
                    + "% profit after fees");

        // Case 2: Buy on B, sell on A
        double buyOnBPrice = tickerB.getAskPrice();
        double sellOnAPrice = tickerA.getBidPrice();

            // Determine if these would be maker or taker orders
            boolean isBuyMakerB = false; // Buying at market price is almost always a taker
            boolean isSellMakerA = false; // Selling at market price is almost always a taker
            
            // Get appropriate fees based on maker/taker status
            Fee buyFeeB = isBuyMakerB ? exchangeB.getMakerFee() : exchangeB.getTakerFee();
            Fee sellFeeA = isSellMakerA ? exchangeA.getMakerFee() : exchangeA.getTakerFee();
    
            // Calculate net profit after fees
            double profitBA = FeeCalculator.calculateArbitrageProfit(buyOnBPrice, sellOnAPrice, quantity, buyFeeB, sellFeeA);
            double profitPercentBA = FeeCalculator.calculateArbitrageProfitPercentage(buyOnBPrice, sellOnAPrice, quantity, buyFeeB, sellFeeA);
    
            // Calculate fee percentages directly from fee amounts
            double buyFeeAmountB = buyFeeB.calculateFee(buyOnBPrice * quantity);
            double sellFeeAmountA = sellFeeA.calculateFee(sellOnAPrice * quantity);
            
            double buyFeePercentB = (buyOnBPrice * quantity > 0) ? (buyFeeAmountB / (buyOnBPrice * quantity) * 100) : 0;
            double sellFeePercentA = (sellOnAPrice * quantity > 0) ? (sellFeeAmountA / (sellOnAPrice * quantity) * 100) : 0;
    
            String buyFeeTypeB = isBuyMakerB ? "Maker" : "Taker";
            String sellFeeTypeA = isSellMakerA ? "Maker" : "Taker";
            
            logDebug("Buy on " + exchangeB.getExchangeName() + " at " + formatPrice(buyOnBPrice)
                    + " (" + buyFeeTypeB + " fee: " + String.format("%.4f", buyFeePercentB) + "%)"
                    + ", Sell on " + exchangeA.getExchangeName() + " at " + formatPrice(sellOnAPrice)
                    + " (" + sellFeeTypeA + " fee: " + String.format("%.4f", sellFeePercentA) + "%)"
                    + " = " + String.format("%.4f", profitPercentBA)
                    + "% profit after fees");

        // Determine which direction has the higher profit potential and meets the minimum profit threshold
        if (profitPercentAB > profitPercentBA && profitPercentAB > minProfitPercent) {
                return createArbitrageOpportunity(pair, exchangeA, exchangeB, buyOnAPrice, sellOnBPrice, 
                        profitPercentAB, buyFeePercentA, sellFeePercentB, isBuyMakerA, isSellMakerB);
            } else if (profitPercentBA > minProfitPercent) {
                return createArbitrageOpportunity(pair, exchangeB, exchangeA, buyOnBPrice, sellOnAPrice, 
                        profitPercentBA, buyFeePercentB, sellFeePercentA, isBuyMakerB, isSellMakerA);
            }
        } catch (Exception e) {
            logDebug("Error calculating arbitrage for " + symbol + ": " + e.getMessage());
        }

        logDebug("No profitable arbitrage opportunity found for symbol: " + symbol);
        // No profitable arbitrage opportunity found
        return null;
    }

    /**
     * Determines the appropriate quantity to trade based on the token price
     * 
     * @param price The token price
     * @return Appropriate quantity for trading
     */
    private double determineAppropriateQuantity(double price) {
        if (price < 0.001) {
            logDebug("Low-value token detected. Using 1,000,000 units for calculations.");
            return 1000000; // 1 million units for micro-priced tokens like SHIB
        } else if (price < 1.0) {
            logDebug("Medium-value token detected. Using 1,000 units for calculations.");
            return 1000; // 1,000 units for tokens under $1
        } else if (price < 100.0) {
            logDebug("High-value token detected. Using 10 units for calculations.");
            return 10; // 10 units for tokens under $100
        } else {
            logDebug("Very high-value token detected. Using 0.01 units for calculations.");
            return 0.01; // 0.01 units for expensive tokens like BTC
        }
    }

    /**
     * Create an arbitrage opportunity object with risk assessment.
     * Older overloaded version for backward compatibility.
     *
     * @param pair The trading pair object
     * @param buyExchange The exchange to buy from
     * @param sellExchange The exchange to sell to
     * @param buyPrice The buy price
     * @param sellPrice The sell price
     * @param profitPercentage The profit percentage
     * @param buyFeePercentage The buy fee percentage
     * @param sellFeePercentage The sell fee percentage
     * @param isBuyMaker Whether the buy order is a maker order
     * @param isSellMaker Whether the sell order is a maker order
     * @return The arbitrage opportunity
     */
    private ArbitrageOpportunity createArbitrageOpportunity(
            TradingPair pair, 
            ExchangeService buyExchange,
            ExchangeService sellExchange, 
            double buyPrice, 
            double sellPrice,
            double profitPercentage, 
            double buyFeePercentage, 
            double sellFeePercentage,
            boolean isBuyMaker,
            boolean isSellMaker) {
        
        if (pair == null) {
            logError("Trading pair is null", null);
            return null;
        }
        
        String tradingPair = pair.getSymbol();
        double amount = determineAppropriateQuantity(buyPrice);
        double profit = (sellPrice - buyPrice) * amount;
        
        return createArbitrageOpportunity(
            buyExchange, sellExchange, tradingPair, amount, buyPrice, sellPrice,
            profit, profitPercentage, riskCalculator, buyFeePercentage, sellFeePercentage,
            isBuyMaker, isSellMaker);
    }

    /**
     * Create an arbitrage opportunity object with risk assessment.
     *
     * @param fromExchange     The exchange to buy from
     * @param toExchange       The exchange to sell to
     * @param tradingPair      The trading pair
     * @param amount           The amount to trade
     * @param buyPrice         The buy price
     * @param sellPrice        The sell price
     * @param profit           The calculated profit
     * @param profitPercentage The profit percentage
     * @param riskManager      The risk manager
     * @param buyFeePercentage The buy fee percentage
     * @param sellFeePercentage The sell fee percentage
     * @param isBuyMaker       Whether the buy order is a maker order
     * @param isSellMaker      Whether the sell order is a maker order
     * @return The arbitrage opportunity
     */
    private ArbitrageOpportunity createArbitrageOpportunity(
            ExchangeService fromExchange, ExchangeService toExchange,
            String tradingPair, double amount, double buyPrice, double sellPrice,
            double profit, double profitPercentage, IRiskManager riskManager,
            double buyFeePercentage, double sellFeePercentage, 
            boolean isBuyMaker, boolean isSellMaker) {
        
        // Convert string trading pair to TradingPair object for backward compatibility
        TradingPair pair = new TradingPair(tradingPair);
        
        try {
            // Get fresh ticker data for risk assessment
            Ticker buyTicker = fromExchange.getTicker(tradingPair);
            Ticker sellTicker = toExchange.getTicker(tradingPair);
            
            if (buyTicker == null || sellTicker == null) {
                logError("Missing ticker data for risk assessment", null);
                return null;
            }
            
            // Calculate price difference
            double priceDifference = sellPrice - buyPrice;
            double priceDifferencePercentage = (priceDifference / buyPrice) * 100;
            
            // Calculate net profit after fees
            double buyFeeAmount = amount * buyPrice * buyFeePercentage;
            double sellFeeAmount = amount * sellPrice * sellFeePercentage;
            double totalFees = buyFeeAmount + sellFeeAmount;
            double netProfit = profit - totalFees;
            double netProfitPercentage = (netProfit / (amount * buyPrice)) * 100;
            
            // Calculate risk metrics
            double riskScore = riskManager.calculateRisk(buyTicker, sellTicker);
            double liquidity = riskManager.assessLiquidity(buyTicker, sellTicker);
            double volatility = riskManager.assessVolatility(tradingPair);
            
            // Check if the opportunity is still viable after risk assessment
            int successRate = riskManager.calculateSuccessRate(profitPercentage, riskScore, volatility);
            boolean isViable = successRate > MINIMUM_SUCCESS_RATE;
            
            // Create the arbitrage opportunity using pair constructor for backward compatibility
            ArbitrageOpportunity opportunity = new ArbitrageOpportunity(
                    fromExchange.getExchangeName(),
                    toExchange.getExchangeName(),
                    tradingPair,
                    amount,
                    buyPrice,
                    sellPrice,
                    profit,
                    profitPercentage,
                    successRate,
                    buyFeePercentage,
                    sellFeePercentage,
                    isBuyMaker,
                    isSellMaker,
                    priceDifferencePercentage,
                    netProfitPercentage,
                    riskScore,
                    liquidity,
                    volatility,
                    isViable
            );
            
            return opportunity;
        } catch (Exception e) {
            logError("Error creating arbitrage opportunity: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Formats price values with appropriate scientific notation for small values
     * 
     * @param price The price to format
     * @return A formatted string representation of the price
     */
    private String formatPrice(double price) {
        if (price < 0.001) {
            return String.format("%.4E", price);
        } else {
            return String.format("%.8f", price);
        }
    }
    
    /**
     * Log a debug message if notification service is available
     * 
     * @param message The message to log
     */
    private void logDebug(String message) {
        if (notificationService != null) {
            notificationService.logDebug(message);
        }
    }

    /**
     * Calculate the arbitrage opportunity for a specific trading pair between two exchanges.
     *
     * @param fromExchange  The exchange to buy from
     * @param toExchange    The exchange to sell to
     * @param tradingPair   The trading pair (e.g., "BTCUSDT")
     * @param amount        The amount to trade
     * @param riskManager   The risk manager to evaluate risk
     * @param notifService  The notification service for logging
     * @return An ArbitrageOpportunity object, or null if no opportunity exists
     */
    @Override
    public ArbitrageOpportunity calculateArbitrage(
            IExchangeService fromExchange, IExchangeService toExchange,
            String tradingPair, double amount, IRiskManager riskManager,
            INotificationService notifService) {
        
        // Cast to ExchangeService if possible
        if (!(fromExchange instanceof ExchangeService) || !(toExchange instanceof ExchangeService)) {
            logError("Cannot calculate arbitrage - unsupported exchange type", 
                    new IllegalArgumentException("Exchanges must be instance of ExchangeService"));
            notifService.logError("Cannot calculate arbitrage - unsupported exchange type", 
                    new IllegalArgumentException("Exchanges must be instance of ExchangeService"));
            return null;
        }
        
        ExchangeService fromExchangeService = (ExchangeService) fromExchange;
        ExchangeService toExchangeService = (ExchangeService) toExchange;
        
        return calculateArbitrage(fromExchangeService, toExchangeService, tradingPair, 
                amount, riskManager, notifService);
    }
    
    /**
     * Determines if an order would be a maker or taker order based on the order book and price.
     * 
     * @param orderBook The order book for the trading pair
     * @param price The price at which the order would be placed
     * @param isBuy Whether this is a buy (true) or sell (false) order
     * @return true if this would be a maker order, false if it would be a taker order
     */
    private boolean isMakerOrder(OrderBook orderBook, double price, boolean isBuy) {
        if (orderBook == null) {
            // Conservative approach: if we don't have order book data, assume taker
            return false;
        }
        
        if (isBuy) {
            // For buy orders: if our price is below the lowest ask, it's a maker order
            double lowestAsk = orderBook.getAsks().isEmpty() ? Double.MAX_VALUE 
                    : orderBook.getAsks().get(0).getPrice();
            return price < lowestAsk;
        } else {
            // For sell orders: if our price is above the highest bid, it's a maker order
            double highestBid = orderBook.getBids().isEmpty() ? 0 
                    : orderBook.getBids().get(0).getPrice();
            return price > highestBid;
        }
    }
    
    /**
     * Internal implementation of arbitrage calculation for ExchangeService types.
     *
     * @param fromExchange  The exchange to buy from
     * @param toExchange    The exchange to sell to
     * @param tradingPair   The trading pair (e.g., "BTCUSDT")
     * @param amount        The amount to trade
     * @param riskManager   The risk manager to evaluate risk
     * @param notifService  The notification service for logging
     * @return An ArbitrageOpportunity object, or null if no opportunity exists
     */
    private ArbitrageOpportunity calculateArbitrage(
            ExchangeService fromExchange, ExchangeService toExchange,
            String tradingPair, double amount, IRiskManager riskManager,
            INotificationService notifService) {
        
        try {
            // Get fresh ticker data to ensure accurate pricing
            Ticker buyTicker = fromExchange.getTicker(tradingPair);
            Ticker sellTicker = toExchange.getTicker(tradingPair);
            
            // Get order books to determine if orders will be maker or taker
            OrderBook buyOrderBook = fromExchange.getOrderBook(tradingPair);
            OrderBook sellOrderBook = toExchange.getOrderBook(tradingPair);
            
            if (buyTicker == null || sellTicker == null) {
                notifService.logWarning("Cannot calculate arbitrage for " + tradingPair + 
                        " - Missing ticker data from " + 
                        (buyTicker == null ? fromExchange.getExchangeName() : toExchange.getExchangeName()));
                return null;
            }
            
            if (buyOrderBook == null || sellOrderBook == null) {
                notifService.logWarning("Cannot determine maker/taker status for " + tradingPair + 
                        " - Missing order book data");
                // We can still continue with a conservative assumption (taker fees)
            }
            
            double buyPrice = buyTicker.getAskPrice();
            double sellPrice = sellTicker.getBidPrice();
            
            // Determine if the orders will be maker or taker
            boolean isBuyMaker = isMakerOrder(buyOrderBook, buyPrice, true);
            boolean isSellMaker = isMakerOrder(sellOrderBook, sellPrice, false);
            
            notifService.logInfo("Order type for " + tradingPair + " on " + 
                    fromExchange.getExchangeName() + ": " + (isBuyMaker ? "Maker" : "Taker"));
            notifService.logInfo("Order type for " + tradingPair + " on " + 
                    toExchange.getExchangeName() + ": " + (isSellMaker ? "Maker" : "Taker"));
            
            // Calculate the fees for buying and selling with correct maker/taker status
            double buyFeeAmount = fromExchange.calculateAndTrackFee(tradingPair, amount * buyPrice, isBuyMaker);
            double sellFeeAmount = toExchange.calculateAndTrackFee(tradingPair, amount * sellPrice, isSellMaker);
            
            // Calculate the profit in the quote currency
            double buyCost = amount * buyPrice + buyFeeAmount;
            double sellRevenue = amount * sellPrice - sellFeeAmount;
            double profit = sellRevenue - buyCost;
            
            // Get the effective fee percentages for logging
            TransactionFee lastBuyFee = fromExchange.getFeeTracker().getLastFee();
            TransactionFee lastSellFee = toExchange.getFeeTracker().getLastFee();
            
            double buyFeePercentage = lastBuyFee != null ? lastBuyFee.getFeePercentage() : 0;
            double sellFeePercentage = lastSellFee != null ? lastSellFee.getFeePercentage() : 0;
            
            // Log the fee details for transparency
            notifService.logInfo(tradingPair + " fee details - Buy: " + 
                    fromExchange.getExchangeName() + " " + 
                    (isBuyMaker ? "maker" : "taker") + " fee " + 
                    (buyFeePercentage * 100) + "%, Sell: " + 
                    toExchange.getExchangeName() + " " + 
                    (isSellMaker ? "maker" : "taker") + " fee " + 
                    (sellFeePercentage * 100) + "%");
            
            // Calculate the profit percentage
            double profitPercentage = (profit / buyCost) * 100;
            
            // If there's a profit, create an arbitrage opportunity
            if (profit > 0) {
                notifService.logInfo("Found profitable arbitrage for " + tradingPair + ": " +
                        String.format("%.2f%%", profitPercentage));
                
                // Get fresh ticker data for risk assessment
                buyTicker = fromExchange.getTicker(tradingPair);
                sellTicker = toExchange.getTicker(tradingPair);
                
                if (buyTicker == null || sellTicker == null) {
                    notifService.logWarning("Cannot assess risk for " + tradingPair + 
                            " - Missing ticker data for risk assessment");
                    return null;
                }
                
                return createArbitrageOpportunity(fromExchange, toExchange, tradingPair,
                        amount, buyPrice, sellPrice, profit, profitPercentage,
                        riskManager, buyFeePercentage, sellFeePercentage,
                        isBuyMaker, isSellMaker);
            } else {
                notifService.logDebug("No arbitrage opportunity for " + tradingPair + 
                        " between " + fromExchange.getExchangeName() + 
                        " and " + toExchange.getExchangeName() + 
                        " (Profit: " + String.format("%.6f", profit) + ")");
                return null;
            }
        } catch (Exception e) {
            notifService.logError("Error calculating arbitrage for " + tradingPair + 
                    " between " + fromExchange.getExchangeName() + 
                    " and " + toExchange.getExchangeName() + ": " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public ArbitrageResult scanForOpportunities() {
        if (exchanges.isEmpty()) {
            if (notificationService != null) {
                notificationService.logWarning("No exchanges configured for arbitrage scanning");
            }
            return new ArbitrageResultImpl(new ArrayList<>());
        }
        
        // Map to track which exchanges support each trading pair
        Map<TradingPair, Set<ExchangeService>> pairExchangeMap = new HashMap<>();
        
        // Collect all trading pairs from all exchanges and track supporting exchanges
        for (ExchangeService exchange : exchanges) {
            List<TradingPair> exchangePairs = exchange.getTradingPairs();
            if (exchangePairs == null || exchangePairs.isEmpty()) {
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
        
        if (notificationService != null) {
            notificationService.logInfo("Found " + viablePairs.size() + 
                    " trading pairs available on at least two exchanges");
        }
        
        return scanForOpportunities(viablePairs);
    }
    
    @Override
    public ArbitrageResult scanForOpportunities(List<TradingPair> pairs) {
        List<ArbitrageOpportunity> opportunities = new ArrayList<>();
        
        for (TradingPair pair : pairs) {
            ArbitrageOpportunity opportunity = calculateArbitrage(pair);
            if (opportunity != null) {
                opportunities.add(opportunity);
            }
        }
        
        // Return the arbitrage result with collected opportunities
        return new ArbitrageResultImpl(opportunities);
    }
    
    @Override
    public void addExchange(ExchangeService exchange) {
        if (exchange != null && !exchanges.contains(exchange)) {
            exchanges.add(exchange);
            if (notificationService != null) {
                notificationService.logInfo("Added exchange: " + exchange.getExchangeName());
            }
        }
    }
    
    @Override
    public void removeExchange(ExchangeService exchange) {
        if (exchange != null) {
            exchanges.remove(exchange);
            if (notificationService != null) {
                notificationService.logInfo("Removed exchange: " + exchange.getExchangeName());
            }
        }
    }
    
    @Override
    public List<ExchangeService> getExchanges() {
        return new ArrayList<>(exchanges);
    }
    
    @Override
    public void setMinProfitThreshold(double threshold) {
        this.minProfitPercent = threshold;
        if (notificationService != null) {
            notificationService.logInfo("Set minimum profit threshold to: " + threshold + "%");
        }
    }

    /**
     * Helper method to log an error with and without an exception.
     * This handles the signature difference in the INotificationService.
     *
     * @param message The error message
     * @param error The exception, or null if none
     */
    private void logError(String message, Throwable error) {
        if (notificationService != null) {
            notificationService.logError(message, error);
        }
    }
}