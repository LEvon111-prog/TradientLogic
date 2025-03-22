package org.example.data.fee;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Factory class for creating exchange-specific fee structures.
 * This class encapsulates the knowledge of fee structures for different exchanges
 * and provides methods to create appropriate Fee objects for each exchange.
 */
public class ExchangeFeeFactory {
    
    // Singleton instance
    private static ExchangeFeeFactory instance;
    
    // Map of exchange names to their default maker fees
    private final Map<String, Fee> defaultMakerFees;
    
    // Map of exchange names to their default taker fees
    private final Map<String, Fee> defaultTakerFees;
    
    /**
     * Private constructor for singleton pattern.
     */
    private ExchangeFeeFactory() {
        defaultMakerFees = new HashMap<>();
        defaultTakerFees = new HashMap<>();
        initializeDefaultFees();
    }
    
    /**
     * Get the singleton instance.
     *
     * @return The ExchangeFeeFactory instance
     */
    public static synchronized ExchangeFeeFactory getInstance() {
        if (instance == null) {
            instance = new ExchangeFeeFactory();
        }
        return instance;
    }
    
    /**
     * Initialize the default fee structures for all supported exchanges.
     */
    private void initializeDefaultFees() {
        // Binance fees (0.10% maker, 0.10% taker for non-BNB pairs)
        defaultMakerFees.put("Binance", new PercentageFee(0.001, true, "Binance maker fee"));
        defaultTakerFees.put("Binance", new PercentageFee(0.001, false, "Binance taker fee"));
        
        // Coinbase fees (starting tier: 0.40% maker, 0.60% taker)
        defaultMakerFees.put("Coinbase", new PercentageFee(0.004, true, "Coinbase maker fee"));
        defaultTakerFees.put("Coinbase", new PercentageFee(0.006, false, "Coinbase taker fee"));
        
        // Kraken fees (starting tier: 0.16% maker, 0.26% taker)
        defaultMakerFees.put("Kraken", new PercentageFee(0.0016, true, "Kraken maker fee"));
        defaultTakerFees.put("Kraken", new PercentageFee(0.0026, false, "Kraken taker fee"));
        
        // Bybit fees (0.10% maker, 0.10% taker)
        defaultMakerFees.put("Bybit", new PercentageFee(0.001, true, "Bybit maker fee"));
        defaultTakerFees.put("Bybit", new PercentageFee(0.001, false, "Bybit taker fee"));
    }
    
    /**
     * Get the default maker fee for an exchange.
     *
     * @param exchangeName The name of the exchange
     * @return The default maker fee, or null if the exchange is not supported
     */
    public Fee getDefaultMakerFee(String exchangeName) {
        return defaultMakerFees.get(exchangeName);
    }
    
    /**
     * Get the default taker fee for an exchange.
     *
     * @param exchangeName The name of the exchange
     * @return The default taker fee, or null if the exchange is not supported
     */
    public Fee getDefaultTakerFee(String exchangeName) {
        return defaultTakerFees.get(exchangeName);
    }
    
    /**
     * Create a tiered fee structure for Binance based on 30-day trading volume.
     *
     * @param thirtyDayVolume The 30-day trading volume in USD
     * @param isMaker Whether to create maker fees (true) or taker fees (false)
     * @param isBnbPair Whether this is for a BNB trading pair (applies special rates)
     * @param hasBnbDiscount Whether BNB payment discount is applied
     * @return A Fee object with the appropriate structure
     */
    public Fee createBinanceFee(double thirtyDayVolume, boolean isMaker, boolean isBnbPair, boolean hasBnbDiscount) {
        // Special case: BNB pairs have zero fees
        if (isBnbPair) {
            return new FixedFee(0.0, "Zero fees for BNB pair");
        }
        
        // For all other pairs, use the standard tiered structure
        Map<Double, Double> tierRates = new TreeMap<>();
        
        if (isMaker) {
            // Maker fees for different VIP levels
            tierRates.put(0.0, 0.001);          // Regular: 0.10%
            tierRates.put(1_000_000.0, 0.0009); // VIP 1: 0.09%
            tierRates.put(5_000_000.0, 0.0008); // VIP 2: 0.08%
            tierRates.put(10_000_000.0, 0.0007); // VIP 3: 0.07%
            tierRates.put(50_000_000.0, 0.0006); // VIP 4: 0.06%
            tierRates.put(100_000_000.0, 0.0005); // VIP 5: 0.05%
            tierRates.put(500_000_000.0, 0.0004); // VIP 6: 0.04%
            tierRates.put(1_000_000_000.0, 0.0003); // VIP 7: 0.03%
            tierRates.put(5_000_000_000.0, 0.0002); // VIP 8: 0.02%
            tierRates.put(10_000_000_000.0, 0.0001); // VIP 9: 0.01%
        } else {
            // Taker fees for different VIP levels
            tierRates.put(0.0, 0.001);          // Regular: 0.10%
            tierRates.put(1_000_000.0, 0.001);  // VIP 1: 0.10%
            tierRates.put(5_000_000.0, 0.0009); // VIP 2: 0.09%
            tierRates.put(10_000_000.0, 0.0008); // VIP 3: 0.08%
            tierRates.put(50_000_000.0, 0.0007); // VIP 4: 0.07%
            tierRates.put(100_000_000.0, 0.0006); // VIP 5: 0.06%
            tierRates.put(500_000_000.0, 0.0005); // VIP 6: 0.05%
            tierRates.put(1_000_000_000.0, 0.0004); // VIP 7: 0.04%
            tierRates.put(5_000_000_000.0, 0.0003); // VIP 8: 0.03%
            tierRates.put(10_000_000_000.0, 0.0002); // VIP 9: 0.02%
        }
        
        TieredFee tieredFee = new TieredFee(tierRates, thirtyDayVolume, isMaker);
        
        // If BNB payment discount is applied, apply the 25% discount
        if (hasBnbDiscount) {
            // Apply the BNB payment discount (25% off the calculated fee)
            return new DiscountedFee(tieredFee, 0.25, "BNB payment discount (25%)");
        }
        
        String feeDescription = isMaker ? "Binance maker fee" : "Binance taker fee";
        tieredFee.setDescription(feeDescription);
        
        return tieredFee;
    }
    
    /**
     * Create a tiered fee structure for Binance based on 30-day trading volume.
     *
     * @param thirtyDayVolume The 30-day trading volume in USD
     * @param isMaker Whether to create maker fees (true) or taker fees (false)
     * @param isBnbPair Whether this is for a BNB trading pair (applies special rates)
     * @return A Fee object with the appropriate structure
     */
    public Fee createBinanceFee(double thirtyDayVolume, boolean isMaker, boolean isBnbPair) {
        return createBinanceFee(thirtyDayVolume, isMaker, isBnbPair, false);
    }
    
    /**
     * Create a tiered fee structure for Coinbase based on 30-day trading volume.
     *
     * @param thirtyDayVolume The 30-day trading volume in USD
     * @param isMaker Whether to create maker fees (true) or taker fees (false)
     * @return A Fee object with the appropriate structure
     */
    public Fee createCoinbaseFee(double thirtyDayVolume, boolean isMaker) {
        Map<Double, Double> tierRates = new TreeMap<>();
        
        if (isMaker) {
            // Maker fees
            tierRates.put(0.0, 0.004);          // 0-$10k: 0.40%
            tierRates.put(10_000.0, 0.0035);    // $10k-$50k: 0.35%
            tierRates.put(50_000.0, 0.0025);    // $50k-$100k: 0.25%
            tierRates.put(100_000.0, 0.002);    // $100k-$1M: 0.20%
            tierRates.put(1_000_000.0, 0.0018); // $1M-$5M: 0.18%
            tierRates.put(5_000_000.0, 0.0016); // $5M-$15M: 0.16%
            tierRates.put(15_000_000.0, 0.0012); // $15M-$75M: 0.12%
            tierRates.put(75_000_000.0, 0.0008); // $75M-$100M: 0.08%
            tierRates.put(100_000_000.0, 0.0005); // $100M-$400M: 0.05%
            tierRates.put(400_000_000.0, 0.0); // $400M+: 0.00%
        } else {
            // Taker fees
            tierRates.put(0.0, 0.006);          // 0-$10k: 0.60%
            tierRates.put(10_000.0, 0.005);     // $10k-$50k: 0.50%
            tierRates.put(50_000.0, 0.0035);    // $50k-$100k: 0.35%
            tierRates.put(100_000.0, 0.003);    // $100k-$1M: 0.30%
            tierRates.put(1_000_000.0, 0.0027); // $1M-$5M: 0.27%
            tierRates.put(5_000_000.0, 0.0025); // $5M-$15M: 0.25%
            tierRates.put(15_000_000.0, 0.002);  // $15M-$75M: 0.20%
            tierRates.put(75_000_000.0, 0.0018); // $75M-$100M: 0.18%
            tierRates.put(100_000_000.0, 0.0015); // $100M-$400M: 0.15%
            tierRates.put(400_000_000.0, 0.001);  // $400M+: 0.10%
        }
        
        return new TieredFee(tierRates, thirtyDayVolume, isMaker);
    }
    
    /**
     * Create a tiered fee structure for Kraken based on 30-day trading volume.
     *
     * @param thirtyDayVolume The 30-day trading volume in USD
     * @param isMaker Whether to create maker fees (true) or taker fees (false)
     * @return A Fee object with the appropriate structure
     */
    public Fee createKrakenFee(double thirtyDayVolume, boolean isMaker) {
        Map<Double, Double> tierRates = new TreeMap<>();
        
        if (isMaker) {
            // Maker fees
            tierRates.put(0.0, 0.0016);         // 0-$50k: 0.16%
            tierRates.put(50_000.0, 0.0014);    // $50k-$100k: 0.14%
            tierRates.put(100_000.0, 0.0012);   // $100k-$250k: 0.12%
            tierRates.put(250_000.0, 0.001);    // $250k-$500k: 0.10%
            tierRates.put(500_000.0, 0.0008);   // $500k-$1M: 0.08%
            tierRates.put(1_000_000.0, 0.0006); // $1M-$2.5M: 0.06%
            tierRates.put(2_500_000.0, 0.0004); // $2.5M-$5M: 0.04%
            tierRates.put(5_000_000.0, 0.0002); // $5M-$10M: 0.02%
            tierRates.put(10_000_000.0, 0.0);   // $10M+: 0.00%
        } else {
            // Taker fees
            tierRates.put(0.0, 0.0026);         // 0-$50k: 0.26%
            tierRates.put(50_000.0, 0.0024);    // $50k-$100k: 0.24%
            tierRates.put(100_000.0, 0.0022);   // $100k-$250k: 0.22%
            tierRates.put(250_000.0, 0.002);    // $250k-$500k: 0.20%
            tierRates.put(500_000.0, 0.0018);   // $500k-$1M: 0.18%
            tierRates.put(1_000_000.0, 0.0016); // $1M-$2.5M: 0.16%
            tierRates.put(2_500_000.0, 0.0014); // $2.5M-$5M: 0.14%
            tierRates.put(5_000_000.0, 0.0012); // $5M-$10M: 0.12%
            tierRates.put(10_000_000.0, 0.001); // $10M+: 0.10%
        }
        
        return new TieredFee(tierRates, thirtyDayVolume, isMaker);
    }
    
    /**
     * Create a tiered fee structure for Bybit based on 30-day trading volume.
     *
     * @param thirtyDayVolume The 30-day trading volume in USD
     * @param isMaker Whether to create maker fees (true) or taker fees (false)
     * @return A Fee object with the appropriate structure
     */
    public Fee createBybitFee(double thirtyDayVolume, boolean isMaker) {
        Map<Double, Double> tierRates = new TreeMap<>();
        
        if (isMaker) {
            // Maker fees - same 0.1% across tiers for spot trading
            tierRates.put(0.0, 0.001);          // All tiers: 0.10%
        } else {
            // Taker fees - same 0.1% across tiers for spot trading
            tierRates.put(0.0, 0.001);          // All tiers: 0.10%
        }
        
        return new TieredFee(tierRates, thirtyDayVolume, isMaker);
    }
    
    /**
     * Create the appropriate fee structure for an exchange based on all parameters.
     *
     * @param exchangeName The name of the exchange
     * @param thirtyDayVolume The 30-day trading volume in USD
     * @param isMaker Whether to create maker fees (true) or taker fees (false)
     * @param bnbPayment Whether BNB payment discount is applied (for Binance only)
     * @param isBnbPair Whether this is a BNB trading pair (for Binance only)
     * @return A Fee object with the appropriate structure
     */
    public Fee createFee(String exchangeName, double thirtyDayVolume, boolean isMaker, boolean bnbPayment, boolean isBnbPair) {
        switch (exchangeName) {
            case "Binance":
                return createBinanceFee(thirtyDayVolume, isMaker, isBnbPair, bnbPayment);
            case "Coinbase":
                return createCoinbaseFee(thirtyDayVolume, isMaker);
            case "Kraken":
                return createKrakenFee(thirtyDayVolume, isMaker);
            case "Bybit":
                return createBybitFee(thirtyDayVolume, isMaker);
            default:
                // Return default fee if exchange is not explicitly supported
                return isMaker ? defaultMakerFees.getOrDefault(exchangeName, 
                               new PercentageFee(0.001, true)) : 
                               defaultTakerFees.getOrDefault(exchangeName,
                               new PercentageFee(0.001, false));
        }
    }
    
    /**
     * Create the appropriate fee structure for an exchange based on volume and maker/taker status.
     *
     * @param exchangeName The name of the exchange
     * @param thirtyDayVolume The 30-day trading volume in USD
     * @param isMaker Whether to create maker fees (true) or taker fees (false)
     * @param bnbPayment Whether BNB payment discount is applied (for Binance only)
     * @return A Fee object with the appropriate structure
     */
    public Fee createFee(String exchangeName, double thirtyDayVolume, boolean isMaker, boolean bnbPayment) {
        return createFee(exchangeName, thirtyDayVolume, isMaker, bnbPayment, false);
    }
    
    /**
     * Create the appropriate fee structure for an exchange based on volume and maker/taker status.
     *
     * @param exchangeName The name of the exchange
     * @param thirtyDayVolume The 30-day trading volume in USD
     * @param isMaker Whether to create maker fees (true) or taker fees (false)
     * @return A Fee object with the appropriate structure
     */
    public Fee createFee(String exchangeName, double thirtyDayVolume, boolean isMaker) {
        return createFee(exchangeName, thirtyDayVolume, isMaker, false, false);
    }
    
    /**
     * Create the default fee structure for an exchange based on maker/taker status.
     *
     * @param exchangeName The name of the exchange
     * @param isMaker Whether to create maker fees (true) or taker fees (false)
     * @return A Fee object with the appropriate structure
     */
    public Fee createFee(String exchangeName, boolean isMaker) {
        return createFee(exchangeName, 0.0, isMaker, false, false);
    }
} 