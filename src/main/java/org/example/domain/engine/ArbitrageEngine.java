package org.example.domain.engine;

import org.example.data.model.ArbitrageOpportunity;
import org.example.data.model.TradingPair;
import org.example.data.service.ExchangeService;
import org.example.domain.risk.RiskCalculator;

import java.util.List;
import java.util.ArrayList;

/**
 * The ArbitrageEngine is responsible for orchestrating the process of scanning for,
 * detecting, and evaluating arbitrage opportunities across multiple exchanges.
 *
 * It leverages ExchangeService implementations to fetch market data and uses specialized
 * classes (such as ExchangeToExchangeArbitrage) to perform low-level arbitrage calculations.
 *
 * The engine then applies a risk evaluation (in this case, a simple profit threshold check)
 * and ranks the opportunities by their potential profit.
 */
public class ArbitrageEngine {

    // A list of all available exchange services
    private List<ExchangeService> exchangeServices;

    // An instance of RiskCalculator used for assessing risk in opportunities
    private RiskCalculator riskCalculator;

    /**
     * Constructor for ArbitrageEngine.
     *
     * @param exchangeServices A list of ExchangeService implementations.
     * @param riskCalculator   A RiskCalculator instance to evaluate risk.
     */
    public ArbitrageEngine(List<ExchangeService> exchangeServices, RiskCalculator riskCalculator) {
        this.exchangeServices = exchangeServices;
        this.riskCalculator = riskCalculator;
    }

    /**
     * Orchestrates the scanning process.
     * It gathers data from all exchanges, detects arbitrage opportunities,
     * evaluates them based on risk, and then ranks the acceptable ones.
     */
    public void scanForOpportunities() {
        // Aggregate opportunities from different detection strategies
        List<ArbitrageOpportunity> opportunities = new ArrayList<>();

        // Detect exchange-to-exchange arbitrage opportunities
        opportunities.addAll(detectExchangeToExchangeArbitrage());

        // Detect triangle arbitrage opportunities (future extension; currently returns empty list)
        opportunities.addAll(detectTriangleArbitrage());

        // Evaluate and rank the aggregated opportunities based on risk and profit potential
        evaluateAndRankOpportunities(opportunities);
    }

    /**
     * Iterates over all pairs of exchange services and their trading pairs to detect
     * arbitrage opportunities based on exchange-to-exchange price discrepancies.
     *
     * @return A list of detected ArbitrageOpportunity instances.
     */
    public List<ArbitrageOpportunity> detectExchangeToExchangeArbitrage() {
        List<ArbitrageOpportunity> opportunities = new ArrayList<>();

        int numExchanges = exchangeServices.size();
        // Loop through each pair of exchanges
        for (int i = 0; i < numExchanges; i++) {
            ExchangeService serviceA = exchangeServices.get(i);
            for (int j = i + 1; j < numExchanges; j++) {
                ExchangeService serviceB = exchangeServices.get(j);

                // Retrieve the cached trading pairs from both exchanges
                List<TradingPair> pairsA = serviceA.getTradingPairs();
                List<TradingPair> pairsB = serviceB.getTradingPairs();

                // For each trading pair in serviceA, check if the same pair exists in serviceB
                for (TradingPair pair : pairsA) {
                    if (pairsB.contains(pair)) {
                        // Use a dedicated class to calculate arbitrage between these two exchanges
                        ExchangeToExchangeArbitrage arbitrage = new ExchangeToExchangeArbitrage(serviceA, serviceB);
                        ArbitrageOpportunity opportunity = arbitrage.calculateArbitrage(pair);
                        if (opportunity != null) {
                            opportunities.add(opportunity);
                        }
                    }
                }
            }
        }
        return opportunities;
    }

    /**
     * Placeholder for triangle arbitrage detection.
     * This method will later be implemented to detect arbitrage opportunities that involve
     * three currency pairs (or exchanges) simultaneously.
     *
     * @return Currently returns an empty list.
     */
    public List<ArbitrageOpportunity> detectTriangleArbitrage() {
        // Future implementation: detect arbitrage opportunities involving three legs.
        return new ArrayList<>();
    }

    /**
     * Evaluates the list of arbitrage opportunities using the risk calculator,
     * filters out opportunities with unacceptable risk, and then ranks the remaining
     * opportunities by their profit potential.
     *
     * @param opportunities The list of arbitrage opportunities to evaluate.
     */
    public void evaluateAndRankOpportunities(List<ArbitrageOpportunity> opportunities) {
        // Create a list to hold opportunities deemed acceptable after risk evaluation
        List<ArbitrageOpportunity> acceptableOpportunities = new ArrayList<>();

        for (ArbitrageOpportunity opportunity : opportunities) {
            // Evaluate each opportunity's risk using the simplified RiskCalculator
            if (riskCalculator.isOpportunityAcceptable(opportunity)) {
                acceptableOpportunities.add(opportunity);
            }
        }

        // Sort the acceptable opportunities in descending order of potential profit
        acceptableOpportunities.sort((opp1, opp2) ->
                Double.compare(opp2.getPotentialProfit(), opp1.getPotentialProfit()));

        // For demonstration, print out the ranked opportunities.
        for (ArbitrageOpportunity opportunity : acceptableOpportunities) {
            System.out.println("Arbitrage Opportunity Detected: " + opportunity);
        }
    }
}
