package org.example.domain.engine;

import org.example.data.model.ArbitrageOpportunity;
import org.example.data.model.RiskAssessment;
import org.example.data.model.Ticker;
import org.example.data.model.TradingPair;
import org.example.data.service.ExchangeService;
import org.example.domain.risk.RiskCalculator;

import java.util.Date;

/**
 * This class encapsulates the logic for detecting an arbitrage opportunity
 * between two exchanges for a given trading pair, taking fees into account.
 * It now outputs detailed logging at each computation step.
 *
 * All log messages are accumulated internally and can be retrieved via getLogMessages().
 */
public class ExchangeToExchangeArbitrage {
    private ExchangeService exchangeA;
    private ExchangeService exchangeB;
    private RiskCalculator riskCalculator;
    private double minProfitPercent = 0.1; // Default minimum profit percentage (0.1%)
    private StringBuilder logBuilder = new StringBuilder();

    /**
     * Constructor that initializes the two exchange services.
     *
     * @param exchangeA First exchange service instance.
     * @param exchangeB Second exchange service instance.
     */
    public ExchangeToExchangeArbitrage(ExchangeService exchangeA, ExchangeService exchangeB) {
        this.exchangeA = exchangeA;
        this.exchangeB = exchangeB;
        // Convert minProfitPercent to decimal for the risk calculator calculation.
        this.riskCalculator = new RiskCalculator(minProfitPercent / 100);
    }

    /**
     * Returns all accumulated log messages.
     *
     * @return A string containing the log messages.
     */
    public String getLogMessages() {
        return logBuilder.toString();
    }

    /**
     * Calculates the potential arbitrage opportunity between two exchanges for a given trading pair.
     *
     * @param pair The trading pair to analyze.
     * @return An ArbitrageOpportunity object if an opportunity exists, null otherwise.
     */
    public ArbitrageOpportunity calculateArbitrage(TradingPair pair) {
        if (pair == null) {
            logBuilder.append("Trading pair is null; aborting arbitrage calculation.\n");
            return null;
        }

        String symbol = pair.getSymbol();
        logBuilder.append("Analyzing arbitrage for symbol: ").append(symbol).append("\n");

        // Get ticker data from both exchanges
        Ticker tickerA = exchangeA.getTickerData(symbol);
        Ticker tickerB = exchangeB.getTickerData(symbol);

        if (tickerA == null || tickerB == null) {
            logBuilder.append("Insufficient ticker data from one or both exchanges.\n");
            return null;  // Can't compare if we don't have data from both exchanges
        }

        // Get the trading fees for each exchange
        double feesA = exchangeA.getTradingFees();
        double feesB = exchangeB.getTradingFees();

        logBuilder.append("Comparing ").append(exchangeA.getExchangeName())
                .append(" (fees: ").append(feesA * 100).append("%) vs ")
                .append(exchangeB.getExchangeName()).append(" (fees: ")
                .append(feesB * 100).append("%)\n");

        // Case 1: Buy on A, sell on B
        double buyOnAPrice = tickerA.getAskPrice();
        double sellOnBPrice = tickerB.getBidPrice();

        // Calculate net profit after fees for case 1
        double netProfitAB = calculateNetProfit(buyOnAPrice, sellOnBPrice, feesA, feesB);
        double profitPercentAB = (netProfitAB / buyOnAPrice) * 100;

        logBuilder.append("Buy on ").append(exchangeA.getExchangeName()).append(" at ").append(buyOnAPrice)
                .append(", Sell on ").append(exchangeB.getExchangeName()).append(" at ").append(sellOnBPrice)
                .append(" = ").append(String.format("%.4f", profitPercentAB))
                .append("% profit after fees\n");

        // Case 2: Buy on B, sell on A
        double buyOnBPrice = tickerB.getAskPrice();
        double sellOnAPrice = tickerA.getBidPrice();

        // Calculate net profit after fees for case 2
        double netProfitBA = calculateNetProfit(buyOnBPrice, sellOnAPrice, feesB, feesA);
        double profitPercentBA = (netProfitBA / buyOnBPrice) * 100;

        logBuilder.append("Buy on ").append(exchangeB.getExchangeName()).append(" at ").append(buyOnBPrice)
                .append(", Sell on ").append(exchangeA.getExchangeName()).append(" at ").append(sellOnAPrice)
                .append(" = ").append(String.format("%.4f", profitPercentBA))
                .append("% profit after fees\n");

        // Determine which direction has the higher profit potential and meets the minimum profit threshold
        if (profitPercentAB > profitPercentBA && profitPercentAB > minProfitPercent) {
            // Risk assessment is calculated here (unused in this example but could be used further)
            RiskAssessment riskAssessment = riskCalculator.calculateRisk(tickerA, tickerB, feesA, feesB);
            logBuilder.append("Arbitrage opportunity detected: Buy on ").append(exchangeA.getExchangeName())
                    .append(", sell on ").append(exchangeB.getExchangeName()).append("\n");
            
            ArbitrageOpportunity opportunity = new ArbitrageOpportunity(
                    pair.getSymbol(),           // Normalized symbol
                    symbol,                     // Buy symbol
                    symbol,                     // Sell symbol
                    exchangeA.getExchangeName(),// Buy exchange
                    exchangeB.getExchangeName(),// Sell exchange
                    buyOnAPrice,                // Buy price
                    sellOnBPrice,               // Sell price
                    profitPercentAB             // Profit percentage after fees
            );
            opportunity.setRiskAssessment(riskAssessment);
            return opportunity;

        } else if (profitPercentBA > minProfitPercent) {
            RiskAssessment riskAssessment = riskCalculator.calculateRisk(tickerB, tickerA, feesB, feesA);
            logBuilder.append("Arbitrage opportunity detected: Buy on ").append(exchangeB.getExchangeName())
                    .append(", sell on ").append(exchangeA.getExchangeName()).append("\n");
            
            ArbitrageOpportunity opportunity = new ArbitrageOpportunity(
                    pair.getSymbol(),            // Normalized symbol
                    symbol,                      // Buy symbol
                    symbol,                      // Sell symbol
                    exchangeB.getExchangeName(), // Buy exchange
                    exchangeA.getExchangeName(), // Sell exchange
                    buyOnBPrice,                 // Buy price
                    sellOnAPrice,                // Sell price
                    profitPercentBA              // Profit percentage after fees
            );
            opportunity.setRiskAssessment(riskAssessment);
            return opportunity;
        }

        logBuilder.append("No profitable arbitrage opportunity found for symbol: ").append(symbol).append("\n");
        // No profitable arbitrage opportunity found
        return null;
    }

    /**
     * Calculates the net profit after accounting for fees.
     * Assumes that fees are expressed as decimals.
     *
     * @param buyPrice  The price at which the asset is bought.
     * @param sellPrice The price at which the asset is sold.
     * @param feeBuy    The fee percentage for the buying exchange (e.g. 0.001 for 0.1%).
     * @param feeSell   The fee percentage for the selling exchange (e.g. 0.001 for 0.1%).
     * @return The net profit after fees.
     */
    private double calculateNetProfit(double buyPrice, double sellPrice, double feeBuy, double feeSell) {
        // Calculate the amount spent including fees on the buy side
        double amountSpent = buyPrice * (1 + feeBuy);

        // Calculate the amount received after fees on the sell side
        double amountReceived = sellPrice * (1 - feeSell);

        // Net profit is the difference
        double netProfit = amountReceived - amountSpent;

        logBuilder.append("Net profit calculation:\n");
        logBuilder.append("  Buy price: ").append(buyPrice).append("\n");
        logBuilder.append("  + Buy fee: ").append(buyPrice * feeBuy).append(" (").append(feeBuy * 100).append("%)\n");
        logBuilder.append("  = Total cost: ").append(amountSpent).append("\n");
        logBuilder.append("  Sell price: ").append(sellPrice).append("\n");
        logBuilder.append("  - Sell fee: ").append(sellPrice * feeSell).append(" (").append(feeSell * 100).append("%)\n");
        logBuilder.append("  = Net received: ").append(amountReceived).append("\n");
        logBuilder.append("  Net profit: ").append(netProfit).append("\n");

        return netProfit;
    }

    /**
     * Utility method to format ticker information for printing.
     *
     * @param ticker The Ticker object.
     * @return A string representation of the ticker data.
     */
    private String tickerToString(Ticker ticker) {
        if (ticker == null) return "null";
        return "[Ask Price: " + ticker.getAskPrice() + ", Bid Price: " + ticker.getBidPrice() + "]";
    }
}