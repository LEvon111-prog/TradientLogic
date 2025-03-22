package org.example.data.fee;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages and tracks transaction fees across exchanges and trading pairs.
 * This class provides reporting and analytics capabilities for fee management.
 */
public class FeeTracker {
    
    private final Map<String, List<TransactionFee>> feesByExchange;
    private final Map<String, List<TransactionFee>> feesByTradingPair;
    private final List<TransactionFee> allFees;
    
    // The most recent fee tracked
    private TransactionFee lastFee;
    
    /**
     * Constructs a new FeeTracker instance.
     */
    public FeeTracker() {
        this.feesByExchange = new ConcurrentHashMap<>();
        this.feesByTradingPair = new ConcurrentHashMap<>();
        this.allFees = Collections.synchronizedList(new ArrayList<>());
        this.lastFee = null;
    }
    
    /**
     * Track a new transaction fee.
     *
     * @param fee The transaction fee to track
     */
    public void trackFee(TransactionFee fee) {
        if (fee == null) {
            return;
        }
        
        allFees.add(fee);
        
        // Track by exchange
        String exchange = fee.getExchangeName();
        if (exchange != null) {
            feesByExchange.computeIfAbsent(exchange, k -> Collections.synchronizedList(new ArrayList<>()))
                        .add(fee);
        }
        
        // Track by trading pair
        String tradingPair = fee.getTradingPair();
        if (tradingPair != null) {
            feesByTradingPair.computeIfAbsent(tradingPair, k -> Collections.synchronizedList(new ArrayList<>()))
                           .add(fee);
        }
        
        // Update last fee
        this.lastFee = fee;
    }
    
    /**
     * Calculate the total fees paid across all exchanges and trading pairs.
     *
     * @return The total fees paid
     */
    public double getTotalFeesPaid() {
        return allFees.stream()
                     .mapToDouble(TransactionFee::getAmount)
                     .sum();
    }
    
    /**
     * Calculate the total fees paid to a specific exchange.
     *
     * @param exchangeName The name of the exchange
     * @return The total fees paid to the exchange
     */
    public double getTotalFeesPaidToExchange(String exchangeName) {
        List<TransactionFee> fees = feesByExchange.get(exchangeName);
        if (fees == null) {
            return 0.0;
        }
        
        return fees.stream()
                  .mapToDouble(TransactionFee::getAmount)
                  .sum();
    }
    
    /**
     * Calculate the total fees paid for a specific trading pair.
     *
     * @param tradingPair The trading pair
     * @return The total fees paid for the trading pair
     */
    public double getTotalFeesPaidForTradingPair(String tradingPair) {
        List<TransactionFee> fees = feesByTradingPair.get(tradingPair);
        if (fees == null) {
            return 0.0;
        }
        
        return fees.stream()
                  .mapToDouble(TransactionFee::getAmount)
                  .sum();
    }
    
    /**
     * Calculate the total discount savings across all fees.
     *
     * @return The total amount saved from discounts
     */
    public double getTotalDiscountSavings() {
        return allFees.stream()
                     .mapToDouble(TransactionFee::getDiscountSavings)
                     .sum();
    }
    
    /**
     * Get the average fee rate paid across all transactions.
     *
     * @return The average fee rate (only for percentage fees)
     */
    public double getAverageFeeRate() {
        List<TransactionFee> percentageFees = allFees.stream()
                                                    .filter(fee -> fee.getFeeType() == FeeType.PERCENTAGE)
                                                    .collect(Collectors.toList());
        
        if (percentageFees.isEmpty()) {
            return 0.0;
        }
        
        double totalRate = percentageFees.stream()
                                        .mapToDouble(TransactionFee::getOriginalFeeRate)
                                        .sum();
        
        return totalRate / percentageFees.size();
    }
    
    /**
     * Generate a report of fees by exchange.
     *
     * @return A map of exchange names to total fees paid
     */
    public Map<String, Double> getFeesByExchangeReport() {
        Map<String, Double> report = new HashMap<>();
        
        for (Map.Entry<String, List<TransactionFee>> entry : feesByExchange.entrySet()) {
            String exchange = entry.getKey();
            double totalFees = entry.getValue().stream()
                                               .mapToDouble(TransactionFee::getAmount)
                                               .sum();
            report.put(exchange, totalFees);
        }
        
        return report;
    }
    
    /**
     * Generate a report of fees by trading pair.
     *
     * @return A map of trading pairs to total fees paid
     */
    public Map<String, Double> getFeesByTradingPairReport() {
        Map<String, Double> report = new HashMap<>();
        
        for (Map.Entry<String, List<TransactionFee>> entry : feesByTradingPair.entrySet()) {
            String pair = entry.getKey();
            double totalFees = entry.getValue().stream()
                                               .mapToDouble(TransactionFee::getAmount)
                                               .sum();
            report.put(pair, totalFees);
        }
        
        return report;
    }
    
    /**
     * Get a list of all tracked transaction fees.
     *
     * @return An unmodifiable list of all transaction fees
     */
    public List<TransactionFee> getAllFees() {
        return Collections.unmodifiableList(allFees);
    }
    
    /**
     * Get a list of transaction fees for a specific exchange.
     *
     * @param exchangeName The name of the exchange
     * @return An unmodifiable list of transaction fees for the exchange
     */
    public List<TransactionFee> getFeesByExchange(String exchangeName) {
        List<TransactionFee> fees = feesByExchange.get(exchangeName);
        return fees != null ? Collections.unmodifiableList(fees) : Collections.emptyList();
    }
    
    /**
     * Get a list of transaction fees for a specific trading pair.
     *
     * @param tradingPair The trading pair
     * @return An unmodifiable list of transaction fees for the trading pair
     */
    public List<TransactionFee> getFeesByTradingPair(String tradingPair) {
        List<TransactionFee> fees = feesByTradingPair.get(tradingPair);
        return fees != null ? Collections.unmodifiableList(fees) : Collections.emptyList();
    }
    
    /**
     * Get the most recent fee tracked.
     *
     * @return The last fee tracked, or null if none
     */
    public TransactionFee getLastFee() {
        return lastFee;
    }
    
    /**
     * Generate a detailed fee summary report with statistics.
     *
     * @return A string containing the fee summary report
     */
    public String generateFeeSummaryReport() {
        StringBuilder report = new StringBuilder();
        report.append("===== FEE SUMMARY REPORT =====\n\n");
        
        // Overall statistics
        report.append("OVERALL STATISTICS:\n");
        report.append(String.format("Total Fees Paid: %.8f\n", getTotalFeesPaid()));
        report.append(String.format("Total Transactions: %d\n", allFees.size()));
        report.append(String.format("Total Discount Savings: %.8f\n", getTotalDiscountSavings()));
        report.append(String.format("Average Fee Rate: %.6f%%\n\n", getAverageFeeRate() * 100));
        
        // By Exchange
        report.append("FEES BY EXCHANGE:\n");
        Map<String, Double> exchangeReport = getFeesByExchangeReport();
        for (Map.Entry<String, Double> entry : exchangeReport.entrySet()) {
            report.append(String.format("%s: %.8f\n", entry.getKey(), entry.getValue()));
        }
        report.append("\n");
        
        // By Trading Pair
        report.append("FEES BY TRADING PAIR:\n");
        Map<String, Double> pairReport = getFeesByTradingPairReport();
        for (Map.Entry<String, Double> entry : pairReport.entrySet()) {
            report.append(String.format("%s: %.8f\n", entry.getKey(), entry.getValue()));
        }
        report.append("\n");
        
        // Fee type distribution
        Map<FeeType, Long> feeTypeCounts = allFees.stream()
                                                     .collect(Collectors.groupingBy(
                                                             TransactionFee::getFeeType, Collectors.counting()));
        
        report.append("FEE TYPE DISTRIBUTION:\n");
        for (Map.Entry<FeeType, Long> entry : feeTypeCounts.entrySet()) {
            report.append(String.format("%s: %d transactions\n", entry.getKey(), entry.getValue()));
        }
        report.append("\n");
        
        // Maker/Taker distribution
        long makerCount = allFees.stream().filter(TransactionFee::isMakerFee).count();
        long takerCount = allFees.size() - makerCount;
        
        report.append("MAKER/TAKER DISTRIBUTION:\n");
        report.append(String.format("Maker Fees: %d (%.2f%%)\n", 
                                  makerCount, (double) makerCount / allFees.size() * 100));
        report.append(String.format("Taker Fees: %d (%.2f%%)\n", 
                                  takerCount, (double) takerCount / allFees.size() * 100));
        
        return report.toString();
    }
    
    /**
     * Get the total fee amount across all tracked fees.
     *
     * @return The total fee amount
     */
    public double getTotalFeeAmount() {
        return allFees.stream().mapToDouble(TransactionFee::getAmount).sum();
    }
    
    /**
     * Get the total fee amount for a specific exchange.
     *
     * @param exchange The exchange name
     * @return The total fee amount for the exchange
     */
    public double getTotalFeeAmountForExchange(String exchange) {
        List<TransactionFee> fees = feesByExchange.get(exchange);
        return fees != null ? fees.stream().mapToDouble(TransactionFee::getAmount).sum() : 0.0;
    }
    
    /**
     * Clear all tracked fees.
     */
    public void clearAll() {
        allFees.clear();
        feesByExchange.clear();
        feesByTradingPair.clear();
        lastFee = null;
    }
    
    /**
     * Get the number of tracked fees.
     *
     * @return The count of tracked fees
     */
    public int getFeeCount() {
        return allFees.size();
    }
} 