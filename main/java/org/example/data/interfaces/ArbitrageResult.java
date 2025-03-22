package org.example.data.interfaces;

import org.example.data.model.ArbitrageOpportunity;
import java.util.List;

/**
 * Interface representing the result of an arbitrage operation.
 * This follows Interface Segregation Principle by providing only the methods
 * needed for arbitrage results.
 */
public interface ArbitrageResult {
    /**
     * Get the list of arbitrage opportunities found.
     *
     * @return List of ArbitrageOpportunity objects
     */
    List<ArbitrageOpportunity> getOpportunities();

    /**
     * Get the timestamp when the arbitrage scan was performed.
     *
     * @return Timestamp of the scan
     */
    long getTimestamp();

    /**
     * Get the total number of opportunities found.
     *
     * @return Number of opportunities
     */
    int getOpportunityCount();

    /**
     * Get the best opportunity found (highest profit).
     *
     * @return Best ArbitrageOpportunity or null if no opportunities found
     */
    ArbitrageOpportunity getBestOpportunity();
    
    /**
     * Check if any opportunities were found.
     *
     * @return true if opportunities exist, false otherwise
     */
    default boolean hasOpportunities() {
        return getOpportunityCount() > 0;
    }
} 