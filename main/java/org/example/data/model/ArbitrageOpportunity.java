package org.example.data.model;

import java.util.Arrays;

public class ArbitrageOpportunity {
    private TradingPair pair;
    private String exchangeBuy;
    private String exchangeSell;
    private double potentialProfit;
    private RiskAssessment riskAssessment;

    // Additional fields to store more detailed information
    private String normalizedSymbol;
    private String symbolBuy;
    private String symbolSell;
    private double buyPrice;
    private double sellPrice;
    private double profitPercent;
    private double successfulArbitragePercent; // New property for successful arbitrage percentage
    
    // Fee-related fields
    private double buyFeePercentage;
    private double sellFeePercentage;
    private boolean isBuyMaker;  // Whether buy order is expected to be a maker order
    private boolean isSellMaker; // Whether sell order is expected to be a maker order
    
    // Slippage-related fields
    private double buySlippage;  // Expected slippage for the buy side
    private double sellSlippage; // Expected slippage for the sell side
    private Ticker buyTicker;    // Ticker data for the buy side
    private Ticker sellTicker;   // Ticker data for the sell side
    
    // Advanced metrics for better decision making
    private double priceDifferencePercentage;
    private double netProfitPercentage;
    private double riskScore;
    private double liquidity;
    private double volatility;
    private boolean isViable;

    public ArbitrageOpportunity(RiskAssessment riskAssessment, double potentialProfit, String exchangeSell, String exchangeBuy, TradingPair pair) {
        this.riskAssessment = riskAssessment;
        this.potentialProfit = potentialProfit;
        this.exchangeSell = exchangeSell;
        this.exchangeBuy = exchangeBuy;
        this.pair = pair;
        this.successfulArbitragePercent = computeMedianRisk(riskAssessment); // Compute median risk value
    }

    /**
     * New constructor for direct exchange-to-exchange comparison with more detailed data
     *
     * @param normalizedSymbol The normalized symbol used for comparison
     * @param symbolBuy The symbol on the buy exchange
     * @param symbolSell The symbol on the sell exchange
     * @param exchangeBuy The name of the exchange to buy on
     * @param exchangeSell The name of the exchange to sell on
     * @param buyPrice The price to buy at
     * @param sellPrice The price to sell at
     * @param profitPercent The percentage profit of this opportunity
     */
    public ArbitrageOpportunity(
            String normalizedSymbol,
            String symbolBuy,
            String symbolSell,
            String exchangeBuy,
            String exchangeSell,
            double buyPrice,
            double sellPrice,
            double profitPercent) {
        this.normalizedSymbol = normalizedSymbol;
        this.symbolBuy = symbolBuy;
        this.symbolSell = symbolSell;
        this.exchangeBuy = exchangeBuy;
        this.exchangeSell = exchangeSell;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.profitPercent = profitPercent;
        this.potentialProfit = profitPercent;
        this.pair = new TradingPair(normalizedSymbol);
        this.successfulArbitragePercent = 0.0; // Default value; risk assessment can override this later
    }

    /**
     * Comprehensive constructor for arbitrage opportunities with fee and order type details.
     *
     * @param exchangeBuy      The exchange to buy from
     * @param exchangeSell     The exchange to sell on
     * @param tradingPair      The trading pair symbol
     * @param amount           The amount to trade
     * @param buyPrice         The price to buy at
     * @param sellPrice        The price to sell at
     * @param profit           The raw profit amount
     * @param profitPercent    The profit as a percentage
     * @param successRate      The calculated success rate (0-100)
     * @param buyFeePercentage The buy fee as a percentage
     * @param sellFeePercentage The sell fee as a percentage
     * @param isBuyMaker       Whether the buy order is a maker order
     * @param isSellMaker      Whether the sell order is a maker order
     * @param priceDiffPercent The price difference percentage
     * @param netProfitPercent The net profit percentage after fees
     * @param riskScore        The risk score
     * @param liquidity        The liquidity assessment
     * @param volatility       The volatility assessment
     * @param isViable         Whether the opportunity is considered viable
     */
    public ArbitrageOpportunity(
            String exchangeBuy,
            String exchangeSell,
            String tradingPair,
            double amount,
            double buyPrice,
            double sellPrice,
            double profit,
            double profitPercent,
            int successRate,
            double buyFeePercentage,
            double sellFeePercentage,
            boolean isBuyMaker,
            boolean isSellMaker,
            double priceDiffPercent,
            double netProfitPercent,
            double riskScore,
            double liquidity,
            double volatility,
            boolean isViable) {
        
        this.normalizedSymbol = tradingPair;
        this.symbolBuy = tradingPair;
        this.symbolSell = tradingPair;
        this.exchangeBuy = exchangeBuy;
        this.exchangeSell = exchangeSell;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.profitPercent = profitPercent;
        this.potentialProfit = profit;
        this.pair = new TradingPair(tradingPair);
        this.successfulArbitragePercent = successRate;
        
        // Set fee-related properties
        this.buyFeePercentage = buyFeePercentage;
        this.sellFeePercentage = sellFeePercentage;
        this.isBuyMaker = isBuyMaker;
        this.isSellMaker = isSellMaker;
        
        // Set advanced metrics
        this.priceDifferencePercentage = priceDiffPercent;
        this.netProfitPercentage = netProfitPercent;
        this.riskScore = riskScore;
        this.liquidity = liquidity;
        this.volatility = volatility;
        this.isViable = isViable;
    }

    public ArbitrageOpportunity(){}

    public TradingPair getPair() {
        return pair;
    }

    public String getExchangeBuy() {
        return exchangeBuy;
    }

    public String getExchangeSell() {
        return exchangeSell;
    }

    public double getPotentialProfit() {
        return potentialProfit;
    }

    public RiskAssessment getRiskAssessment() {
        return riskAssessment;
    }

    public void setPair(TradingPair pair) {
        this.pair = pair;
    }

    public void setExchangeBuy(String exchangeBuy) {
        this.exchangeBuy = exchangeBuy;
    }

    public void setExchangeSell(String exchangeSell) {
        this.exchangeSell = exchangeSell;
    }

    public void setPotentialProfit(double potentialProfit) {
        this.potentialProfit = potentialProfit;
    }

    /**
     * Updated setter to compute the successfulArbitragePercent property based on the
     * median value of all risk properties in the RiskAssessment.
     */
    public void setRiskAssessment(RiskAssessment riskAssessment) {
        this.riskAssessment = riskAssessment;
        if (riskAssessment != null) {
            this.successfulArbitragePercent = computeMedianRisk(riskAssessment);
        }
    }

    public String getNormalizedSymbol() {
        return normalizedSymbol;
    }

    public String getSymbolBuy() {
        return symbolBuy;
    }

    public String getSymbolSell() {
        return symbolSell;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    public double getProfitPercent() {
        return profitPercent;
    }

    /**
     * Gets the successful arbitrage percentage calculated as the median
     * of all risk assessment properties.
     *
     * @return The percentage likelihood of successfully executing the arbitrage.
     */
    public double getSuccessfulArbitragePercent() {
        return successfulArbitragePercent;
    }

    /**
     * Sets the successful arbitrage percentage.
     *
     * @param successfulArbitragePercent The percentage likelihood.
     */
    public void setSuccessfulArbitragePercent(double successfulArbitragePercent) {
        this.successfulArbitragePercent = successfulArbitragePercent;
    }

    /**
     * Get the buy fee percentage.
     * 
     * @return The buy fee percentage
     */
    public double getBuyFeePercentage() {
        return buyFeePercentage;
    }
    
    /**
     * Get the sell fee percentage.
     * 
     * @return The sell fee percentage
     */
    public double getSellFeePercentage() {
        return sellFeePercentage;
    }
    
    /**
     * Check if the buy order is expected to be a maker order.
     * 
     * @return True if the buy order is a maker order, false for taker
     */
    public boolean isBuyMaker() {
        return isBuyMaker;
    }
    
    /**
     * Check if the sell order is expected to be a maker order.
     * 
     * @return True if the sell order is a maker order, false for taker
     */
    public boolean isSellMaker() {
        return isSellMaker;
    }
    
    /**
     * Get the price difference percentage between buy and sell prices.
     * 
     * @return The price difference as a percentage
     */
    public double getPriceDifferencePercentage() {
        return priceDifferencePercentage;
    }
    
    /**
     * Get the net profit percentage after accounting for all fees.
     * 
     * @return The net profit percentage
     */
    public double getNetProfitPercentage() {
        return netProfitPercentage;
    }
    
    /**
     * Get the calculated risk score.
     * 
     * @return The risk score
     */
    public double getRiskScore() {
        return riskScore;
    }
    
    /**
     * Get the liquidity assessment.
     * 
     * @return The liquidity score
     */
    public double getLiquidity() {
        return liquidity;
    }
    
    /**
     * Get the volatility assessment.
     * 
     * @return The volatility score
     */
    public double getVolatility() {
        return volatility;
    }
    
    /**
     * Check if the opportunity is considered viable.
     * 
     * @return True if viable, false otherwise
     */
    public boolean isViable() {
        return isViable;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(normalizedSymbol).append(": Buy on ").append(exchangeBuy)
          .append(" (").append(isBuyMaker ? "maker" : "taker").append(" fee: ")
          .append(String.format("%.4f%%", buyFeePercentage * 100)).append(") at ")
          .append(buyPrice).append(", Sell on ").append(exchangeSell)
          .append(" (").append(isSellMaker ? "maker" : "taker").append(" fee: ")
          .append(String.format("%.4f%%", sellFeePercentage * 100)).append(") at ")
          .append(sellPrice).append(System.lineSeparator())
          .append("Profit: ").append(String.format("%.2f%%", profitPercent))
          .append(", Net: ").append(String.format("%.2f%%", netProfitPercentage))
          .append(", Success Rate: ").append(String.format("%.0f%%", successfulArbitragePercent))
          .append(", Viable: ").append(isViable);
        
        return sb.toString();
    }

    /**
     * Helper method to compute the median risk value from a RiskAssessment's properties.
     *
     * @param riskAssessment An instance of RiskAssessment.
     * @return The median value.
     */
    private double computeMedianRisk(RiskAssessment riskAssessment) {
        double[] risks = new double[] {
                riskAssessment.getLiquidityScore(),
                riskAssessment.getVolatilityScore(),
                riskAssessment.getFeeImpact(),
                riskAssessment.getMarketDepthScore(),
                riskAssessment.getExecutionSpeedRisk(),
                riskAssessment.getSlippageRisk(),
                riskAssessment.getMarketRegimeScore(),
                riskAssessment.getSentimentScore(),
                riskAssessment.getAnomalyScore(),
                riskAssessment.getCorrelationScore()
        };
        Arrays.sort(risks);
        int n = risks.length;
        if (n % 2 == 1) {
            return risks[n / 2];
        } else {
            return ((risks[(n / 2) - 1] + risks[n / 2]) / 2.0) * 100;
        }
    }

    /**
     * Gets the buy symbol for this arbitrage opportunity
     * @return The buy symbol
     */
    public String getBuySymbol() {
        return symbolBuy;
    }
    
    /**
     * Gets the sell symbol for this arbitrage opportunity
     * @return The sell symbol
     */
    public String getSellSymbol() {
        return symbolSell;
    }

    /**
     * Gets the buy slippage for this arbitrage opportunity
     * @return The buy slippage as a decimal (e.g., 0.001 for 0.1%)
     */
    public double getBuySlippage() {
        return buySlippage;
    }
    
    /**
     * Sets the buy slippage for this arbitrage opportunity
     * @param buySlippage The buy slippage as a decimal (e.g., 0.001 for 0.1%)
     */
    public void setBuySlippage(double buySlippage) {
        this.buySlippage = buySlippage;
    }
    
    /**
     * Gets the sell slippage for this arbitrage opportunity
     * @return The sell slippage as a decimal (e.g., 0.001 for 0.1%)
     */
    public double getSellSlippage() {
        return sellSlippage;
    }
    
    /**
     * Sets the sell slippage for this arbitrage opportunity
     * @param sellSlippage The sell slippage as a decimal (e.g., 0.001 for 0.1%)
     */
    public void setSellSlippage(double sellSlippage) {
        this.sellSlippage = sellSlippage;
    }
    
    /**
     * Gets the ticker data for the buy side of this arbitrage opportunity
     * @return The buy ticker
     */
    public Ticker getBuyTicker() {
        return buyTicker;
    }
    
    /**
     * Sets the ticker data for the buy side of this arbitrage opportunity
     * @param buyTicker The buy ticker
     */
    public void setBuyTicker(Ticker buyTicker) {
        this.buyTicker = buyTicker;
    }
    
    /**
     * Gets the ticker data for the sell side of this arbitrage opportunity
     * @return The sell ticker
     */
    public Ticker getSellTicker() {
        return sellTicker;
    }
    
    /**
     * Sets the ticker data for the sell side of this arbitrage opportunity
     * @param sellTicker The sell ticker
     */
    public void setSellTicker(Ticker sellTicker) {
        this.sellTicker = sellTicker;
    }
}