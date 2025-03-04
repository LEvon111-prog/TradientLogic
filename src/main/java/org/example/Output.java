package org.example;

import org.example.data.model.OrderBook;
import org.example.data.model.Ticker;
import org.example.data.model.TradingPair;
import org.example.data.service.BinanceExchangeService;
import org.example.data.service.BybitV5ExchangeService;
import org.example.data.service.CoinbaseExchangeService;
import org.example.data.service.ExchangeService;
import org.example.data.service.KrakenExchangeService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Output {
    // Flag to track if WebSocket is connected and receiving data
    private static AtomicBoolean webSocketConnected = new AtomicBoolean(false);
    
    public static void main(String[] args) {
        System.out.println("=== Crypto Exchange WebSocket Data Monitor ===");
        
        // Choose which exchange service to test
        ExchangeService exchangeService = selectExchangeService();
        System.out.println("Using " + exchangeService.getExchangeName() + " exchange service");

        // Fetch trading pairs (still using REST API for this initial step)
        List<TradingPair> tradingPairs = fetchTradingPairs(exchangeService);
        if (tradingPairs.isEmpty()) {
            System.out.println("No trading pairs available. Exiting.");
            return;
        }

        // Display some of the trading pairs
        displayTradingPairs(tradingPairs);

        // Allow user to select a trading pair or use a default
        TradingPair selectedPair = selectTradingPair(tradingPairs, exchangeService);
        String symbol = selectedPair.getSymbol();
        System.out.println("\nSelected trading pair: " + symbol);
        
        // Initialize WebSocket connection with proper symbol formatting
        String formattedSymbol = formatSymbolForWebSocket(symbol, exchangeService);
        
        // Try to initialize WebSocket connection with a timeout
        initializeWebSocketWithTimeout(exchangeService, formattedSymbol);
        
        if (webSocketConnected.get()) {
            // Start monitoring data if WebSocket is connected
            monitorMarketData(exchangeService, symbol);
        } else {
            System.out.println("WebSocket connection could not be established. Please check your network or try another exchange.");
        }
        
        System.out.println("\n=== Exchange WebSocket Data Monitor Complete ===");
    }
    
    private static String formatSymbolForWebSocket(String symbol, ExchangeService exchangeService) {
        // Different exchanges require different symbol formats for WebSocket
        String exchangeName = exchangeService.getExchangeName().toLowerCase();
        
        if (exchangeName.contains("binance")) {
            // Binance WebSocket usually uses lowercase symbols
            return symbol.toLowerCase();
        } else if (exchangeName.contains("coinbase")) {
            // Coinbase uses product_ids like "BTC-USD"
            if (symbol.contains("-")) {
                return symbol;
            } else if (symbol.contains("USDT")) {
                return symbol.replace("USDT", "-USDT");
            } else if (symbol.contains("USD")) {
                return symbol.replace("USD", "-USD");
            }
        } else if (exchangeName.contains("kraken")) {
            // Kraken might use different format like XBT/USD instead of BTC/USD
            if (symbol.startsWith("BTC")) {
                return symbol.replace("BTC", "XBT");
            }
        } else if (exchangeName.contains("bybit")) {
            // Bybit format is typically the same as the REST API
            return symbol;
        }
        
        // Default: return the original symbol
        return symbol;
    }
    
    private static List<TradingPair> fetchTradingPairs(ExchangeService exchangeService) {
        System.out.println("\nFetching trading pairs from " + exchangeService.getExchangeName() + "...");
        try {
            List<TradingPair> pairs = exchangeService.fetchTradingPairs();
            System.out.println("Successfully fetched " + pairs.size() + " trading pairs");
            return pairs;
        } catch (Exception e) {
            System.err.println("Error fetching trading pairs: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    private static void displayTradingPairs(List<TradingPair> tradingPairs) {
        System.out.println("\nSample trading pairs:");
        int count = 0;
        for (TradingPair pair : tradingPairs) {
            System.out.println(" * " + pair.getSymbol());
            count++;
            if (count >= 10) {
                System.out.println(" * ... and " + (tradingPairs.size() - 10) + " more");
                break;
            }
        }
    }
    
    private static void initializeWebSocketWithTimeout(ExchangeService exchangeService, String symbol) {
        System.out.println("\nInitializing WebSocket connection for " + symbol + "...");
        
        // Create a latch to wait for initial WebSocket response
        CountDownLatch connectionLatch = new CountDownLatch(1);
        
        // Start WebSocket connection attempt in a separate thread
        Thread wsThread = new Thread(() -> {
            try {
                boolean success = exchangeService.initializeWebSocket(Arrays.asList(symbol));
                if (success) {
                    System.out.println("WebSocket connection request sent successfully!");
                    webSocketConnected.set(true);
                } else {
                    System.out.println("WebSocket connection initialization failed.");
                    webSocketConnected.set(false);
                }
            } catch (Exception e) {
                System.err.println("Error during WebSocket initialization: " + e.getMessage());
                e.printStackTrace();
                webSocketConnected.set(false);
            } finally {
                connectionLatch.countDown();
            }
        });
        
        wsThread.start();
        
        try {
            // Wait up to 10 seconds for the WebSocket connection to be established
            boolean completed = connectionLatch.await(10, TimeUnit.SECONDS);
            if (!completed) {
                System.out.println("WebSocket initialization timed out after 10 seconds");
                webSocketConnected.set(false);
            }
            
            // If connection was successful, wait a bit more for initial data
            if (webSocketConnected.get()) {
                System.out.println("WebSocket connected! Waiting for initial data...");
                Thread.sleep(5000);  // Wait 5 seconds for initial data
            }
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting for WebSocket connection: " + e.getMessage());
            webSocketConnected.set(false);
        }
    }
    
    private static void monitorMarketData(ExchangeService exchangeService, String symbol) {
        // Start a monitoring loop to display real-time data
        System.out.println("\n=== WebSocket Market Data Monitor Started ===");
        System.out.println("Press ENTER to stop monitoring");
        
        // Create a separate thread for the monitor
        Thread monitorThread = new Thread(() -> {
            try {
                int updateCount = 0;
                while (!Thread.currentThread().isInterrupted() && updateCount < 100) {
                    try {
                        // Get ticker data from the WebSocket cache
                        Ticker ticker = exchangeService.getTickerData(symbol);
                        if (ticker != null) {
                            System.out.println("\n[WebSocket UPDATE #" + (++updateCount) + " at " + new java.util.Date() + "]");
                            System.out.println("Ticker for " + symbol + ":");
                            System.out.println("  Bid Price: " + ticker.getBidPrice());
                            System.out.println("  Ask Price: " + ticker.getAskPrice());
                            System.out.println("  Last Price: " + ticker.getLastPrice());
                        } else {
                            System.out.println("\n[UPDATE #" + (++updateCount) + " at " + new java.util.Date() + "]");
                            System.out.println("No ticker data available for " + symbol + " yet");
                        }
                        
                        // Get order book from the WebSocket cache
                        OrderBook orderBook = exchangeService.getOrderBook(symbol);
        if (orderBook != null) {
                            System.out.println("Order Book for " + symbol + ":");
                            System.out.println("  Best Bid: " + (orderBook.getBestBid() != null ? orderBook.getBestBid().getPrice() : "N/A") + 
                                              " (Volume: " + (orderBook.getBestBid() != null ? orderBook.getBestBid().getVolume() : "N/A") + ")");
                            System.out.println("  Best Ask: " + (orderBook.getBestAsk() != null ? orderBook.getBestAsk().getPrice() : "N/A") + 
                                              " (Volume: " + (orderBook.getBestAsk() != null ? orderBook.getBestAsk().getVolume() : "N/A") + ")");
                            System.out.println("  Spread: " + orderBook.getSpread());
                        } else {
                            System.out.println("No order book data available for " + symbol + " yet");
                        }
                    } catch (Exception e) {
                        System.err.println("Error during update: " + e.getMessage());
                    }
                    
                    // Wait before getting next update
                    Thread.sleep(2000);
                }
                
                System.out.println("\nMonitor thread completed after " + updateCount + " updates");
            } catch (InterruptedException e) {
                System.out.println("\nMonitor thread stopped");
            }
        });
        
        monitorThread.start();
        
        // Wait for user input to stop
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
        
        // Stop the monitor thread
        monitorThread.interrupt();
        
        // Close WebSocket connection
        System.out.println("Closing WebSocket connection...");
        try {
            exchangeService.closeWebSocket();
        } catch (Exception e) {
            System.err.println("Error closing WebSocket: " + e.getMessage());
        }
        
        // Wait for thread to finish
        try {
            monitorThread.join(5000);
        } catch (InterruptedException e) {
            System.err.println("Error waiting for monitor thread to finish: " + e.getMessage());
        }
    }
    
    private static ExchangeService selectExchangeService() {
        System.out.println("Select an exchange to monitor via WebSocket:");
        System.out.println("1. Binance (WebSocket API)");
        System.out.println("2. Coinbase (WebSocket API)");
        System.out.println("3. Kraken (WebSocket API)");
        System.out.println("4. Bybit (WebSocket API)");
        
        Scanner scanner = new Scanner(System.in);
        int choice = 0;
        
        while (choice < 1 || choice > 4) {
            System.out.print("Enter your choice (1-4) [default=1]: ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                choice = 1;
                break;
            }
            
            try {
                choice = Integer.parseInt(input);
                if (choice < 1 || choice > 4) {
                    System.out.println("Invalid choice. Please enter a number between 1 and 4.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
        
        switch (choice) {
            case 1:
                return new BinanceExchangeService(0.001);
            case 2:
                return new CoinbaseExchangeService(0.001);
            case 3:
                return new KrakenExchangeService(0.002);
            case 4:
                return new BybitV5ExchangeService(0.001);
            default:
                return new BinanceExchangeService(0.001);
        }
    }
    
    private static TradingPair selectTradingPair(List<TradingPair> tradingPairs, ExchangeService exchangeService) {
        if (tradingPairs.isEmpty()) {
            throw new IllegalArgumentException("No trading pairs available");
        }
        
        String exchangeName = exchangeService.getExchangeName().toLowerCase();
        
        System.out.println("\nSelect a trading pair for " + exchangeService.getExchangeName() + " WebSocket:");
        System.out.println("1. Use recommended pair (optimized for " + exchangeService.getExchangeName() + ")");
        System.out.println("2. Enter a custom pair");
        
        Scanner scanner = new Scanner(System.in);
        int choice = 0;
        
        while (choice < 1 || choice > 2) {
            System.out.print("Enter your choice (1-2) [default=1]: ");
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                choice = 1;
                break;
            }
            
            try {
                choice = Integer.parseInt(input);
                if (choice < 1 || choice > 2) {
                    System.out.println("Invalid choice. Please enter 1 or 2.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
        
        if (choice == 1) {
            // Try to find a recommended pair based on the exchange
            return findRecommendedPair(tradingPairs, exchangeName);
        } else {
            // Let user enter a custom pair
            return selectCustomPair(tradingPairs, exchangeName);
        }
    }
    
    private static TradingPair findRecommendedPair(List<TradingPair> tradingPairs, String exchangeName) {
        String[] popularPairs;
        
        // Customize popular pairs based on exchange
        if (exchangeName.contains("binance")) {
            popularPairs = new String[]{"BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "DOGEUSDT"};
        } else if (exchangeName.contains("coinbase")) {
            popularPairs = new String[]{"BTC-USD", "ETH-USD", "SOL-USD", "BTC-USDT", "ETH-USDT"};
        } else if (exchangeName.contains("kraken")) {
            popularPairs = new String[]{"XBTUSD", "ETHUSD", "XBTUSDT", "ETHUSDT", "SOLUSD"};
        } else if (exchangeName.contains("bybit")) {
            popularPairs = new String[]{"BTCUSDT", "ETHUSDT", "SOLUSDT", "DOGEUSDT", "MATICUSDT"};
        } else {
            popularPairs = new String[]{"BTCUSDT", "ETHUSDT", "BTCUSD", "ETHUSD", "BTC-USD", "ETH-USD"};
        }
        
        // Check for popular pairs
        for (String symbol : popularPairs) {
            for (TradingPair pair : tradingPairs) {
                if (pair.getSymbol().equalsIgnoreCase(symbol)) {
                    System.out.println("Selected recommended pair: " + symbol);
                    return pair;
                }
            }
        }
        
        // If no popular pairs, try to find a pair with USDT or USD
        for (TradingPair pair : tradingPairs) {
            String symbol = pair.getSymbol();
            if (symbol.contains("USDT") || symbol.contains("USD")) {
                System.out.println("Selected USDT/USD pair: " + symbol);
                return pair;
            }
        }
        
        // If nothing else, just return the first pair
        System.out.println("Selected default pair: " + tradingPairs.get(0).getSymbol());
        return tradingPairs.get(0);
    }
    
    private static TradingPair selectCustomPair(List<TradingPair> tradingPairs, String exchangeName) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("\nEnter a trading pair for " + exchangeName + " WebSocket:");
        
        if (exchangeName.contains("binance")) {
            System.out.println("Format example: BTCUSDT, ETHUSDT");
        } else if (exchangeName.contains("coinbase")) {
            System.out.println("Format example: BTC-USD, ETH-USDT");
        } else if (exchangeName.contains("kraken")) {
            System.out.println("Format example: XBTUSD, ETHUSD (BTC is XBT on Kraken)");
        } else if (exchangeName.contains("bybit")) {
            System.out.println("Format example: BTCUSDT, ETHUSDT");
        }
        
        while (true) {
            System.out.print("Enter the symbol: ");
            String symbol = scanner.nextLine().trim().toUpperCase();
            
            // Check if the symbol exists in the available pairs
            for (TradingPair pair : tradingPairs) {
                if (pair.getSymbol().equalsIgnoreCase(symbol)) {
                    return pair;
                }
            }
            
            System.out.println("Symbol not found. Please enter a valid symbol from the list of available trading pairs.");
            System.out.print("Try again? (y/n): ");
            String tryAgain = scanner.nextLine().trim().toLowerCase();
            if (!tryAgain.startsWith("y")) {
                // If user doesn't want to try again, return a recommended pair
                System.out.println("Using a recommended pair instead.");
                return findRecommendedPair(tradingPairs, exchangeName);
            }
        }
    }
}
