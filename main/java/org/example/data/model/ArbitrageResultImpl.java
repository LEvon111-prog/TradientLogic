package org.example.data.model;

import org.example.data.interfaces.ArbitrageResult;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Concrete implementation of ArbitrageResult interface.
 * This class follows Single Responsibility Principle by only handling
 * arbitrage result data.
 */
public class ArbitrageResultImpl implements ArbitrageResult {
    private final List<ArbitrageOpportunity> opportunities;
    private final long timestamp;
    private final int opportunityCount;
    private final ArbitrageOpportunity bestOpportunity;

    public ArbitrageResultImpl(List<ArbitrageOpportunity> opportunities) {
        this.opportunities = new ArrayList<>(opportunities);
        this.timestamp = System.currentTimeMillis();
        this.opportunityCount = opportunities.size();
        
        // Find the best opportunity (highest profit)
        this.bestOpportunity = opportunities.stream()
                .max((a, b) -> Double.compare(a.getPotentialProfit(), b.getPotentialProfit()))
                .orElse(null);
    }

    @Override
    public List<ArbitrageOpportunity> getOpportunities() {
        return Collections.unmodifiableList(opportunities);
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public int getOpportunityCount() {
        return opportunityCount;
    }

    @Override
    public ArbitrageOpportunity getBestOpportunity() {
        return bestOpportunity;
    }
} 