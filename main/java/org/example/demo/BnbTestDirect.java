package org.example.demo;

/**
 * A direct test for BNB fees that bypasses all the complex framework
 */
public class BnbTestDirect {
    
    public static void main(String[] args) {
        System.out.println("===== DIRECT BNB FEE TEST =====");
        System.out.println("Testing zero fees for BNB pairs and BNB discount for other pairs");
        System.out.println("==========================================================");
        
        // Test trading pairs
        String[] pairs = {"BNBUSDT", "ETHBNB", "BTCUSDT"};
        
        // Process each pair
        for (String pair : pairs) {
            boolean isBinance = true;  // Assuming Binance exchange
            boolean isBnbPair = isBnbPair(pair);
            double amount = 10000.0;  // $10,000 transaction
            
            // Calculate fee based on our rule
            double feeAmount;
            String feeDescription;
            
            if (isBnbPair && isBinance) {
                // Zero fees for BNB pairs on Binance
                feeAmount = 0.0;
                feeDescription = "Zero fee (BNB pair)";
            } else if (isBinance) {
                // Regular fee with BNB discount (0.1% - 25% = 0.075%)
                feeAmount = amount * 0.00075;
                feeDescription = "Discounted fee (non-BNB pair with BNB discount)";
            } else {
                // Regular fee for other exchanges (0.1%)
                feeAmount = amount * 0.001;
                feeDescription = "Regular fee";
            }
            
            double feePercentage = feeAmount / amount * 100;
            
            // Print individual result
            System.out.println("\nPair: " + pair);
            System.out.println("  Is BNB Pair: " + isBnbPair);
            System.out.println("  Fee Amount: $" + feeAmount);
            System.out.println("  Fee Percentage: " + feePercentage + "%");
            System.out.println("  Fee Description: " + feeDescription);
            System.out.println("----------------------------------------");
        }
        
        // Print comparison
        System.out.println("\nFor comparison, regular taker fee without BNB handling:");
        System.out.println("  Regular Fee Amount: $" + (10000.0 * 0.00075));
        System.out.println("  Regular Fee Percentage: " + 0.075 + "%");
        System.out.println("========================================");
    }
    
    /**
     * Check if a trading pair involves BNB
     */
    private static boolean isBnbPair(String tradingPair) {
        if (tradingPair == null) {
            return false;
        }
        
        String pair = tradingPair.toUpperCase();
        return pair.startsWith("BNB") || pair.endsWith("BNB") || 
               pair.contains("BNB-") || pair.contains("-BNB") ||
               pair.contains("BNB/") || pair.contains("/BNB");
    }
} 