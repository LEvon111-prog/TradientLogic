package org.example.domain.engine;

import org.example.data.model.ArbitrageOpportunity;
import org.example.data.model.TradingPair;
import org.example.data.model.Ticker;
import org.example.data.service.ExchangeService;
import org.example.data.model.RiskAssessment;
import org.example.domain.risk.RiskCalculator;

/**
 * This class encapsulates the logic for detecting an arbitrage opportunity
 * between two exchanges for a given trading pair, taking fees into account.
 * It now outputs detailed logging at each computation step.
 */
public class ExchangeToExchangeArbitrage {
    private ExchangeService exchangeA;
    private ExchangeService exchangeB;
    private RiskCalculator riskCalculator;
    private double minProfitPercent = 0.5; // Default minimum profit percentage (0.5%)

    /**
     * Constructor that initializes the two exchange services.
     *
     * @param exchangeA First exchange service instance.
     * @param exchangeB Second exchange service instance.
     */
    public ExchangeToExchangeArbitrage(ExchangeService exchangeA, ExchangeService exchangeB) {
        this.exchangeA = exchangeA;
        this.exchangeB = exchangeB;
        this.riskCalculator = new RiskCalculator(minProfitPercent / 100); // Convert to decimal
    }

    /**
     * Calculates the potential arbitrage opportunity between two exchanges for a given trading pair.
     * @param pair The trading pair to analyze
     * @return An ArbitrageOpportunity object if an opportunity exists, null otherwise
     */
    public ArbitrageOpportunity calculateArbitrage(TradingPair pair) {
        if (pair == null) {
            return null;
        }

        String symbol = pair.getSymbol();
        
        // Get ticker data from both exchanges
        Ticker tickerA = exchangeA.getTickerData(symbol);
        Ticker tickerB = exchangeB.getTickerData(symbol);
        
        if (tickerA == null || tickerB == null) {
            return null;  // Can't compare if we don't have data from both exchanges
        }
        
        // Get the actual trading fees for each exchange
        double feesA = exchangeA.getTradingFees();
        double feesB = exchangeB.getTradingFees();
        
        System.out.println("Comparing " + exchangeA.getExchangeName() + " (fees: " + (feesA * 100) + "%) vs " + 
                           exchangeB.getExchangeName() + " (fees: " + (feesB * 100) + "%)");
        
        // Case 1: Buy on A, sell on B
        double buyOnAPrice = tickerA.getAskPrice();
        double sellOnBPrice = tickerB.getBidPrice();
        
        // Calculate net profit after fees
        double netProfitAB = calculateNetProfit(buyOnAPrice, sellOnBPrice, feesA, feesB);
        double profitPercentAB = (netProfitAB / buyOnAPrice) * 100;
        
        System.out.println("Buy on " + exchangeA.getExchangeName() + " at " + buyOnAPrice + 
                          ", Sell on " + exchangeB.getExchangeName() + " at " + sellOnBPrice + 
                          " = " + String.format("%.4f", profitPercentAB) + "% profit after fees");
        
        // Case 2: Buy on B, sell on A
        double buyOnBPrice = tickerB.getAskPrice();
        double sellOnAPrice = tickerA.getBidPrice();
        
        // Calculate net profit after fees
        double netProfitBA = calculateNetProfit(buyOnBPrice, sellOnAPrice, feesB, feesA);
        double profitPercentBA = (netProfitBA / buyOnBPrice) * 100;
        
        System.out.println("Buy on " + exchangeB.getExchangeName() + " at " + buyOnBPrice + 
                          ", Sell on " + exchangeA.getExchangeName() + " at " + sellOnAPrice + 
                          " = " + String.format("%.4f", profitPercentBA) + "% profit after fees");
        
        // Determine which direction has higher profit potential
        if (profitPercentAB > profitPercentBA && profitPercentAB > minProfitPercent) {
            // Create a risk assessment object using the risk calculator
            RiskAssessment riskAssessment = riskCalculator.calculateRisk(tickerA, tickerB, feesA, feesB);
            
            // Return opportunity to buy on A and sell on B with detailed information
            return new ArbitrageOpportunity(
                    pair.getSymbol(),  // Normalized symbol
                    symbol,            // Buy symbol
                    symbol,            // Sell symbol
                    exchangeA.getExchangeName(),  // Buy exchange
                    exchangeB.getExchangeName(),  // Sell exchange
                    buyOnAPrice,       // Buy price
                    sellOnBPrice,      // Sell price
                    profitPercentAB    // Profit percentage after fees
            );
            
        } else if (profitPercentBA > minProfitPercent) {
            // Create a risk assessment object using the risk calculator
            RiskAssessment riskAssessment = riskCalculator.calculateRisk(tickerB, tickerA, feesB, feesA);
            
            // Return opportunity to buy on B and sell on A with detailed information
            return new ArbitrageOpportunity(
                    pair.getSymbol(),  // Normalized symbol
                    symbol,            // Buy symbol
                    symbol,            // Sell symbol
                    exchangeB.getExchangeName(),  // Buy exchange
                    exchangeA.getExchangeName(),  // Sell exchange
                    buyOnBPrice,       // Buy price
                    sellOnAPrice,      // Sell price
                    profitPercentBA    // Profit percentage after fees
            );
        }
        
        // No profitable arbitrage opportunity found
        return null;
    }

    /**
     * Calculates the net profit after accounting for fees.
     * Assumes that fees are expressed as a decimal.
     *
     * @param buyPrice  The price at which the asset is bought.
     * @param sellPrice The price at which the asset is sold.
     * @param feeBuy    The fee percentage for the buying exchange (e.g. 0.001 for 0.1%).
     * @param feeSell   The fee percentage for the selling exchange (e.g. 0.001 for 0.1%).
     * @return The net profit after fees.
     */
    private double calculateNetProfit(double buyPrice, double sellPrice, double feeBuy, double feeSell) {
        // Calculate the amount spent including fees (buy side)
        double amountSpent = buyPrice * (1 + feeBuy);
        
        // Calculate the amount received after fees (sell side)
        double amountReceived = sellPrice * (1 - feeSell);
        
        // Net profit is the difference
        double netProfit = amountReceived - amountSpent;
        
        System.out.println("Net profit calculation:");
        System.out.println("  Buy price: " + buyPrice);
        System.out.println("  + Buy fee: " + (buyPrice * feeBuy) + " (" + (feeBuy * 100) + "%)");
        System.out.println("  = Total cost: " + amountSpent);
        System.out.println("  Sell price: " + sellPrice);
        System.out.println("  - Sell fee: " + (sellPrice * feeSell) + " (" + (feeSell * 100) + "%)");
        System.out.println("  = Net received: " + amountReceived);
        System.out.println("  Net profit: " + netProfit);
        
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
