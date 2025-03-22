package org.example.data.fee;

/**
 * Interface for fee calculations.
 * This defines the contract for various fee types in the system.
 */
public interface Fee {
    
    /**
     * Calculate the fee amount for a given transaction amount.
     *
     * @param amount The transaction amount
     * @return The calculated fee amount
     */
    double calculateFee(double amount);
    
    /**
     * Get the description of this fee.
     *
     * @return The fee description
     */
    String getDescription();
    
    /**
     * Get the type of this fee.
     *
     * @return The fee type
     */
    FeeType getType();
} 