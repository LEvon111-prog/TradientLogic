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

    @Override
    public String toString() {
        if (normalizedSymbol != null) {
            return normalizedSymbol + ": Buy on " + exchangeBuy + " at " + buyPrice +
                    ", Sell on " + exchangeSell + " at " + sellPrice +
                    " (Profit: " + String.format("%.2f", profitPercent) + "%, Success Rate: " +
                    String.format("%.2f", successfulArbitragePercent) + "%)";
        } else {
            return "ArbitrageOpportunity{" +
                    "pair=" + (pair != null ? pair.getSymbol() : "null") +
                    ", buy=" + exchangeBuy +
                    ", sell=" + exchangeSell +
                    ", profit=" + String.format("%.2f", potentialProfit) + "%" +
                    ", success=" + String.format("%.2f", successfulArbitragePercent) + "%" +
                    "}";
        }
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
}