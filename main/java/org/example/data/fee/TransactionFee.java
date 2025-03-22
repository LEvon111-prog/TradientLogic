package org.example.data.fee;

/**
 * Represents a fee applied to a specific transaction.
 * This stores information about a fee that was actually charged
 * for tracking and reporting purposes.
 */
public class TransactionFee {
    private String id;
    private String exchangeName;
    private String tradingPair;
    private double amount;
    private FeeType type;
    private String currency;
    private String description;
    private double feePercentage;
    private double discountPercentage;
    private boolean isMakerFee;
    
    /**
     * Constructor for a transaction fee record.
     *
     * @param id The unique identifier for this fee record
     * @param exchangeName The exchange where the fee was charged
     * @param tradingPair The trading pair for the transaction
     * @param amount The fee amount
     * @param type The fee type
     * @param currency The currency of the fee (can be null if same as trading pair)
     * @param description A description of the fee
     * @param feePercentage The fee as a percentage of the transaction amount
     * @param discountPercentage The discount percentage applied (if any)
     * @param isMakerFee Whether this was a maker fee (true) or taker fee (false)
     */
    public TransactionFee(String id, String exchangeName, String tradingPair, 
                         double amount, FeeType type, String currency, 
                         String description, double feePercentage,
                         double discountPercentage, boolean isMakerFee) {
        this.id = id;
        this.exchangeName = exchangeName;
        this.tradingPair = tradingPair;
        this.amount = amount;
        this.type = type;
        this.currency = currency;
        this.description = description;
        this.feePercentage = feePercentage;
        this.discountPercentage = discountPercentage;
        this.isMakerFee = isMakerFee;
    }
    
    /**
     * Get the unique identifier for this fee record.
     *
     * @return The ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Get the exchange name where the fee was charged.
     *
     * @return The exchange name
     */
    public String getExchangeName() {
        return exchangeName;
    }
    
    /**
     * Get the trading pair for the transaction.
     *
     * @return The trading pair
     */
    public String getTradingPair() {
        return tradingPair;
    }
    
    /**
     * Get the fee amount.
     *
     * @return The fee amount
     */
    public double getAmount() {
        return amount;
    }
    
    /**
     * Get the fee type.
     *
     * @return The fee type
     */
    public FeeType getType() {
        return type;
    }
    
    /**
     * Get the currency of the fee.
     *
     * @return The currency, or null if same as trading pair
     */
    public String getCurrency() {
        return currency;
    }
    
    /**
     * Get the description of the fee.
     *
     * @return The fee description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the fee as a percentage of the transaction amount.
     *
     * @return The fee percentage
     */
    public double getFeePercentage() {
        return feePercentage;
    }
    
    /**
     * Get the discount percentage applied (if any).
     *
     * @return The discount percentage
     */
    public double getDiscountPercentage() {
        return discountPercentage;
    }
    
    /**
     * Check if this was a maker fee (true) or taker fee (false).
     *
     * @return true if this was a maker fee, false if it was a taker fee
     */
    public boolean isMakerFee() {
        return isMakerFee;
    }
    
    /**
     * Get the fee type.
     * Alias for getType() for backward compatibility.
     *
     * @return The fee type
     */
    public FeeType getFeeType() {
        return type;
    }
    
    /**
     * Get the original fee rate before any discounts.
     * For backward compatibility with existing code.
     *
     * @return The original fee percentage
     */
    public double getOriginalFeeRate() {
        return feePercentage;
    }
    
    /**
     * Calculate the fee savings from discounts.
     *
     * @return The amount saved due to applied discounts
     */
    public double getDiscountSavings() {
        if (discountPercentage > 0 && amount > 0) {
            // Calculate what the fee would have been without the discount
            double undiscountedAmount = amount / (1 - discountPercentage);
            return undiscountedAmount - amount;
        }
        return 0.0;
    }
    
    @Override
    public String toString() {
        return "TransactionFee{" +
                "id='" + id + '\'' +
                ", exchange='" + exchangeName + '\'' +
                ", pair='" + tradingPair + '\'' +
                ", amount=" + amount +
                ", type=" + type +
                ", currency='" + currency + '\'' +
                ", feePercentage=" + (feePercentage * 100) + "%" +
                ", isMaker=" + isMakerFee +
                '}';
    }
} 