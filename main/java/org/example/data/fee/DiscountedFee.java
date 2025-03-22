package org.example.data.fee;

/**
 * A decorator class that wraps another Fee and applies a discount to it.
 * This allows for easily adding discounts (like Binance's BNB payment discount)
 * to any existing fee structure.
 */
public class DiscountedFee implements Fee {
    private final Fee baseFee;
    private final double discountRate;
    private final String description;

    /**
     * Creates a new discounted fee wrapper.
     *
     * @param baseFee The original fee to apply a discount to
     * @param discountRate The discount rate (e.g., 0.25 for 25% discount)
     * @param discountDescription A description of the discount
     */
    public DiscountedFee(Fee baseFee, double discountRate, String discountDescription) {
        this.baseFee = baseFee;
        this.discountRate = discountRate;
        this.description = baseFee.getDescription() + " with " + discountDescription;
    }

    /**
     * Calculates the fee after applying the discount.
     *
     * @param amount The transaction amount
     * @return The discounted fee amount
     */
    @Override
    public double calculateFee(double amount) {
        return baseFee.calculateFee(amount) * (1 - discountRate);
    }

    /**
     * Gets the description of this fee, including the discount information.
     *
     * @return The fee description
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Gets the fee type of the underlying fee.
     *
     * @return The fee type
     */
    @Override
    public FeeType getType() {
        return baseFee.getType();
    }

    /**
     * Gets the discount rate applied by this wrapper.
     *
     * @return The discount rate (e.g., 0.25 for 25% discount)
     */
    public double getDiscountRate() {
        return discountRate;
    }

    /**
     * Gets the base fee before discount.
     *
     * @return The base fee
     */
    public Fee getBaseFee() {
        return baseFee;
    }
} 