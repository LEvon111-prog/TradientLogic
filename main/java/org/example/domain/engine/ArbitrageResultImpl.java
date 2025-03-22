package org.example.domain.engine;

import org.example.data.model.ArbitrageOpportunity;
import org.example.data.interfaces.ArbitrageResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Represents the result of an arbitrage opportunity scan.
 * This class encapsulates a list of opportunities and provides methods to analyze them.
 * Implements the ArbitrageResult interface for compatibility.
 */
public class ArbitrageResultImpl implements ArbitrageResult {
    
    private final List<ArbitrageOpportunity> opportunities;
    private final long timestamp;
    
    /**
     * Constructor for the arbitrage scan result.
     *
     * @param opportunities List of arbitrage opportunities found
     */
    public ArbitrageResultImpl(List<ArbitrageOpportunity> opportunities) {
        this.opportunities = new ArrayList<>(opportunities);
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Get all the opportunities found.
     *
     * @return Unmodifiable list of all opportunities
     */
    @Override
    public List<ArbitrageOpportunity> getOpportunities() {
        return Collections.unmodifiableList(opportunities);
    }
    
    /**
     * Get the top N opportunities sorted by profit percentage.
     *
     * @param n Number of opportunities to return
     * @return Sorted list of the top N opportunities
     */
    public List<ArbitrageOpportunity> getTopOpportunities(int n) {
        return opportunities.stream()
                .sorted(Comparator.comparing(ArbitrageOpportunity::getProfitPercent).reversed())
                .limit(n)
                .toList();
    }
    
    /**
     * Get viable opportunities (those with sufficient success rate).
     *
     * @return List of viable opportunities
     */
    public List<ArbitrageOpportunity> getViableOpportunities() {
        return opportunities.stream()
                .filter(ArbitrageOpportunity::isViable)
                .sorted(Comparator.comparing(ArbitrageOpportunity::getProfitPercent).reversed())
                .toList();
    }
    
    /**
     * Get the timestamp when this result was created.
     *
     * @return The timestamp in milliseconds
     */
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Check if any opportunities were found.
     *
     * @return true if opportunities exist, false otherwise
     */
    public boolean hasOpportunities() {
        return !opportunities.isEmpty();
    }
    
    /**
     * Get the number of opportunities found.
     *
     * @return The count of opportunities
     */
    @Override
    public int getOpportunityCount() {
        return opportunities.size();
    }
    
    /**
     * Get the number of viable opportunities.
     *
     * @return The count of viable opportunities
     */
    public int getViableOpportunityCount() {
        return (int) opportunities.stream()
                .filter(ArbitrageOpportunity::isViable)
                .count();
    }
    
    /**
     * Get the best opportunity (highest profit percentage).
     *
     * @return The best arbitrage opportunity or null if none found
     */
    @Override
    public ArbitrageOpportunity getBestOpportunity() {
        if (opportunities.isEmpty()) {
            return null;
        }
        return opportunities.stream()
                .max(Comparator.comparing(ArbitrageOpportunity::getProfitPercent))
                .orElse(null);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ArbitrageResult with ").append(opportunities.size()).append(" opportunities\n");
        
        if (!opportunities.isEmpty()) {
            sb.append("Top opportunities:\n");
            getTopOpportunities(Math.min(5, opportunities.size())).forEach(
                    opp -> sb.append("  ").append(opp.toString()).append("\n")
            );
        } else {
            sb.append("No arbitrage opportunities found.\n");
        }
        
        return sb.toString();
    }
} 