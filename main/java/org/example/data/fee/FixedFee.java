package org.example.data.fee;

/**
 * Represents a fixed fee that doesn't scale with the transaction amount.
 * This is sometimes used for withdrawal fees by exchanges.
 */
public class FixedFee implements Fee {
    
    private final double feeAmount;
    private final String description;
    
    /**
     * Constructs a fixed fee with the specified amount.
     *
     * @param feeAmount The fixed fee amount
     * @param description A description of the fee
     */
    public FixedFee(double feeAmount, String description) {
        this.feeAmount = feeAmount;
        this.description = description != null ? description : 
                "Fixed fee of " + feeAmount;
    }
    
    /**
     * Constructs a fixed fee with a default description.
     *
     * @param feeAmount The fixed fee amount
     */
    public FixedFee(double feeAmount) {
        this(feeAmount, null);
    }
    
    @Override
    public double calculateFee(double amount) {
        // Fixed fee doesn't depend on the transaction amount
        return feeAmount;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public FeeType getType() {
        return FeeType.FIXED;
    }
    
    /**
     * Gets the fixed fee amount.
     *
     * @return The fee amount
     */
    public double getFeeAmount() {
        return feeAmount;
    }
} 