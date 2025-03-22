package org.example.data.fee;

/**
 * Represents a percentage-based fee that scales with the transaction amount.
 * This is the most common type of fee in cryptocurrency exchanges.
 */
public class PercentageFee implements Fee {
    
    private final double percentage;
    private final boolean isMakerFee;
    private final String description;
    
    /**
     * Constructs a percentage-based fee with the specified rate.
     *
     * @param percentage The fee percentage as a decimal (e.g., 0.001 for 0.1%)
     * @param isMakerFee Whether this is a maker fee (true) or taker fee (false)
     * @param description A description of the fee
     */
    public PercentageFee(double percentage, boolean isMakerFee, String description) {
        this.percentage = percentage;
        this.isMakerFee = isMakerFee;
        this.description = description != null ? description : 
                (isMakerFee ? "Maker fee " : "Taker fee ") + 
                String.format("%.4f%%", percentage * 100);
    }
    
    /**
     * Constructs a percentage-based fee with default description.
     *
     * @param percentage The fee percentage as a decimal (e.g., 0.001 for 0.1%)
     * @param isMakerFee Whether this is a maker fee (true) or taker fee (false)
     */
    public PercentageFee(double percentage, boolean isMakerFee) {
        this(percentage, isMakerFee, null);
    }
    
    @Override
    public double calculateFee(double amount) {
        return amount * percentage;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public FeeType getType() {
        return FeeType.PERCENTAGE;
    }
    
    /**
     * Gets the percentage rate of this fee.
     *
     * @return The fee percentage as a decimal
     */
    public double getPercentage() {
        return percentage;
    }
    
    /**
     * Determines if this is a maker fee.
     *
     * @return true if this is a maker fee, false if it's a taker fee
     */
    public boolean isMakerFee() {
        return isMakerFee;
    }
} 