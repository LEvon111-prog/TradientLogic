package org.example.data.fee;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Represents a tiered fee structure where the fee rate varies based on
 * volume thresholds. This is commonly used by exchanges where higher
 * trading volumes qualify for lower fee rates.
 */
public class TieredFee implements Fee {
    
    private final NavigableMap<Double, Double> tierRates;
    private String description;
    private final boolean isMakerFee;
    private double thirtyDayTradingVolume;
    
    /**
     * Constructs a tiered fee structure with the specified tier rates.
     *
     * @param tierRates Map of volume thresholds to fee rates (as decimals)
     * @param thirtyDayTradingVolume The 30-day trading volume for this user
     * @param isMakerFee Whether this is a maker fee (true) or taker fee (false)
     * @param description Optional description of the fee structure
     */
    public TieredFee(Map<Double, Double> tierRates, double thirtyDayTradingVolume, 
                     boolean isMakerFee, String description) {
        this.tierRates = new TreeMap<>(tierRates);
        this.thirtyDayTradingVolume = thirtyDayTradingVolume;
        this.isMakerFee = isMakerFee;
        this.description = description != null ? description : 
                generateDefaultDescription();
    }
    
    /**
     * Constructs a tiered fee structure with the specified tier rates.
     *
     * @param tierRates Map of volume thresholds to fee rates (as decimals)
     * @param thirtyDayTradingVolume The 30-day trading volume for this user
     * @param isMakerFee Whether this is a maker fee (true) or taker fee (false)
     */
    public TieredFee(Map<Double, Double> tierRates, double thirtyDayTradingVolume, boolean isMakerFee) {
        this(tierRates, thirtyDayTradingVolume, isMakerFee, null);
    }
    
    @Override
    public double calculateFee(double amount) {
        Map.Entry<Double, Double> tier = tierRates.floorEntry(thirtyDayTradingVolume);
        if (tier == null) {
            // If no tier match (unlikely, but just in case), use the first tier
            tier = tierRates.firstEntry();
        }
        
        return amount * tier.getValue();
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    /**
     * Sets the description for this fee structure.
     * 
     * @param description The new description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public FeeType getType() {
        return FeeType.TIERED;
    }
    
    /**
     * Gets the current fee rate based on the 30-day trading volume.
     *
     * @return The current fee rate as a decimal
     */
    public double getCurrentFeeRate() {
        Map.Entry<Double, Double> tier = tierRates.floorEntry(thirtyDayTradingVolume);
        if (tier == null) {
            tier = tierRates.firstEntry();
        }
        return tier.getValue();
    }
    
    /**
     * Updates the 30-day trading volume and recalculates the applicable fee tier.
     *
     * @param volume The new 30-day trading volume
     */
    public void updateTradingVolume(double volume) {
        this.thirtyDayTradingVolume = volume;
    }
    
    /**
     * Gets the tier rates map.
     *
     * @return The tier rates map with volume thresholds and fee rates
     */
    public NavigableMap<Double, Double> getTierRates() {
        return new TreeMap<>(tierRates);
    }
    
    /**
     * Determines if this is a maker fee.
     *
     * @return true if this is a maker fee, false if it's a taker fee
     */
    public boolean isMakerFee() {
        return isMakerFee;
    }
    
    /**
     * Gets the current 30-day trading volume.
     *
     * @return The 30-day trading volume
     */
    public double getThirtyDayTradingVolume() {
        return thirtyDayTradingVolume;
    }
    
    /**
     * Generates a default description for the fee structure.
     *
     * @return A string describing the fee structure
     */
    private String generateDefaultDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(isMakerFee ? "Maker" : "Taker")
          .append(" tiered fee structure based on 30-day volume");
        return sb.toString();
    }
} 