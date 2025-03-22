package org.example.demo;

/**
 * A very simple test for BNB pair detection
 */
public class SimpleBnbTest {
    
    public static void main(String[] args) {
        System.out.println("===== SIMPLE BNB PAIR DETECTION TEST =====");
        
        String[] testPairs = {
            "BNBUSDT",
            "ETHBNB",
            "BNB-USDT",
            "ETH-BNB",
            "BNB/USDT",
            "ETH/BNB",
            "BTCUSDT"  // Not a BNB pair
        };
        
        for (String pair : testPairs) {
            boolean isBnbPair = isBnbPair(pair);
            System.out.println("Pair: " + pair + " -> BNB Pair? " + isBnbPair);
        }
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