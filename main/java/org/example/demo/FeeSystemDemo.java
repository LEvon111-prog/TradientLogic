package org.example.demo;

import org.example.data.fee.*;
import org.example.data.service.BinanceExchangeService;
import org.example.data.service.CoinbaseExchangeService;
import org.example.data.service.ExchangeService;
import org.example.data.service.KrakenExchangeService;

import java.util.Arrays;
import java.util.List;

/**
 * Demo application that showcases the fee system.
 */
public class FeeSystemDemo {
    
    public static void main(String[] args) {
        System.out.println("===== CRYPTOCURRENCY EXCHANGE FEE SYSTEM DEMO =====");
        System.out.println("Note: All fees shown reflect actual exchange fee tiers and structures.\n");
        
        // Print the fee system demo
        System.out.println(FeeReportGenerator.generateFeeDemo());
        
        // Create some exchange services with different fee configurations
        ExchangeService binance = createBinanceExchange();
        ExchangeService coinbase = createCoinbaseExchange();
        ExchangeService kraken = createKrakenExchange();
        
        // Simulate some trades on each exchange
        simulateTrades(binance);
        simulateTrades(coinbase);
        simulateTrades(kraken);
        
        // Generate a consolidated fee report
        List<ExchangeService> exchanges = Arrays.asList(binance, coinbase, kraken);
        System.out.println(FeeReportGenerator.generateConsolidatedReport(exchanges));
        
        // Show individual exchange fee summaries
        System.out.println("===== BINANCE FEE SUMMARY =====");
        System.out.println(binance.getFeeTracker().generateFeeSummaryReport());
        
        System.out.println("===== COINBASE FEE SUMMARY =====");
        System.out.println(coinbase.getFeeTracker().generateFeeSummaryReport());
        
        System.out.println("===== KRAKEN FEE SUMMARY =====");
        System.out.println(kraken.getFeeTracker().generateFeeSummaryReport());
    }
    
    /**
     * Create a Binance exchange with BNB discount.
     *
     * @return A configured BinanceExchangeService
     */
    private static ExchangeService createBinanceExchange() {
        // Create with default fee of 0.1% (0.001)
        BinanceExchangeService binance = new BinanceExchangeService(0.001);
        
        // Update to use volume-based tier - $5M volume gets 0.08% maker and 0.09% taker
        binance.updateFeesTiers(5_000_000.0);
        
        // Enable BNB discount (25% off fees when paying with BNB token)
        binance.setBnbDiscount(true);
        
        return binance;
    }
    
    /**
     * Create a Coinbase exchange.
     *
     * @return A configured CoinbaseExchangeService
     */
    private static ExchangeService createCoinbaseExchange() {
        // Create with default fee of 0.6% taker (0.006)
        CoinbaseExchangeService coinbase = new CoinbaseExchangeService(0.006);
        
        // Update to use volume-based tier - $100K volume gets 0.2% maker and 0.3% taker
        coinbase.updateFeesTiers(100_000.0);
        
        return coinbase;
    }
    
    /**
     * Create a Kraken exchange.
     *
     * @return A configured KrakenExchangeService
     */
    private static ExchangeService createKrakenExchange() {
        // Create with default fee of 0.26% taker (0.0026)
        KrakenExchangeService kraken = new KrakenExchangeService(0.0026);
        
        // Update to use volume-based tier - $250K volume gets 0.1% maker and 0.2% taker
        kraken.updateFeesTiers(250_000.0);
        
        return kraken;
    }
    
    /**
     * Simulate various trades on an exchange.
     * Using smaller transaction sizes that are more typical for a retail trader.
     *
     * @param exchange The exchange service
     */
    private static void simulateTrades(ExchangeService exchange) {
        // Simulate some market buys (taker fees)
        exchange.calculateAndTrackFee("BTCUSDT", 5000.0, false);  // $5K BTC purchase
        exchange.calculateAndTrackFee("ETHUSDT", 2500.0, false);  // $2.5K ETH purchase
        exchange.calculateAndTrackFee("SOLUSDT", 1000.0, false);  // $1K SOL purchase
        
        // Simulate some limit orders (maker fees)
        exchange.calculateAndTrackFee("BTCUSDT", 7500.0, true);   // $7.5K BTC limit sell
        exchange.calculateAndTrackFee("ETHUSDT", 1500.0, true);   // $1.5K ETH limit buy
        
        // Simulate a larger trade
        exchange.calculateAndTrackFee("BTCUSDT", 30000.0, false); // $30K BTC market sell
    }
} 