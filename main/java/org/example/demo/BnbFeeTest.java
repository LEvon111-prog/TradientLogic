package org.example.demo;

import org.example.data.fee.Fee;
import org.example.data.fee.TransactionFee;
import org.example.data.service.BinanceExchangeService;

/**
 * Simple test class to verify BNB fee changes
 */
public class BnbFeeTest {
    
    public static void main(String[] args) {
        System.out.println("===== BNB FEE TEST =====");
        System.out.println("Testing zero fees for BNB pairs and BNB discount for other pairs");
        System.out.println("==========================================================\n");
        
        try {
            // Create Binance exchange with BNB discount
            System.out.println("Creating Binance exchange with BNB discount...");
            BinanceExchangeService binance = new BinanceExchangeService(0.001);
            binance.updateFeesTiers(5_000_000.0);
            binance.setBnbDiscount(true);
            
            // Test fee calculation for different pair types
            double amount = 10000.0; // $10,000
            
            System.out.println("\n1. TESTING BNB PAIR (BNBUSDT)");
            System.out.println("------------------------------");
            // 1. Test BNB pair (should be zero fee)
            String bnbPair = "BNBUSDT";
            double bnbFeeAmount = binance.calculateAndTrackFee(bnbPair, amount, false);
            TransactionFee bnbFee = binance.getFeeTracker().getLastFee();
            
            System.out.println("\n2. TESTING NON-BNB PAIR (BTCUSDT)");
            System.out.println("---------------------------------");
            // 2. Test non-BNB pair (should have BNB discount)
            String nonBnbPair = "BTCUSDT";
            double nonBnbFeeAmount = binance.calculateAndTrackFee(nonBnbPair, amount, false);
            TransactionFee nonBnbFee = binance.getFeeTracker().getLastFee();
            
            System.out.println("\n3. TESTING ALTERNATIVE BNB PAIR (ETHBNB)");
            System.out.println("---------------------------------------");
            // 3. Test another format of BNB pair
            String altBnbPair = "ETHBNB";
            double altBnbFeeAmount = binance.calculateAndTrackFee(altBnbPair, amount, false);
            TransactionFee altBnbFee = binance.getFeeTracker().getLastFee();
            
            // Print results with clear section headers
            System.out.println("\n\n===== RESULTS SUMMARY =====");
            System.out.println("========================================");
            
            System.out.println("\nBNB Pair (" + bnbPair + "):");
            System.out.println("  Fee Amount: $" + bnbFeeAmount);
            System.out.println("  Fee Percentage: " + (bnbFee.getFeePercentage() * 100) + "%");
            System.out.println("  Fee Description: " + bnbFee.getDescription());
            
            System.out.println("\nNon-BNB Pair (" + nonBnbPair + ") with BNB Discount:");
            System.out.println("  Fee Amount: $" + nonBnbFeeAmount);
            System.out.println("  Fee Percentage: " + (nonBnbFee.getFeePercentage() * 100) + "%");
            System.out.println("  Discount Percentage: " + (nonBnbFee.getDiscountPercentage() * 100) + "%");
            System.out.println("  Fee Description: " + nonBnbFee.getDescription());
            
            System.out.println("\nAlternative BNB Pair (" + altBnbPair + "):");
            System.out.println("  Fee Amount: $" + altBnbFeeAmount);
            System.out.println("  Fee Percentage: " + (altBnbFee.getFeePercentage() * 100) + "%");
            System.out.println("  Fee Description: " + altBnbFee.getDescription());
            
            // Calculate what the fee would have been without our changes
            Fee takerFee = binance.getTakerFee();
            double regularFeeAmount = takerFee.calculateFee(amount);
            System.out.println("\nFor comparison, regular taker fee without special handling:");
            System.out.println("  Regular Fee Amount: $" + regularFeeAmount);
            System.out.println("  Regular Fee Percentage: " + (regularFeeAmount / amount * 100) + "%");
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 