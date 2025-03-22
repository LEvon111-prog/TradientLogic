package org.example.data.fee;

/**
 * Enum defining the various types of fees in the system.
 * This should align with the Fee.FeeType values for compatibility.
 */
public enum FeeType {
    /**
     * A fixed fee that doesn't change with the transaction amount
     */
    FIXED,
    
    /**
     * A percentage-based fee that scales with the transaction amount
     */
    PERCENTAGE,
    
    /**
     * A tiered fee structure that changes based on trading volume
     */
    TIERED,
    
    /**
     * A combination of multiple fee types
     */
    COMPOSITE
} 