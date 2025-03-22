package org.example.data.service;

import org.example.data.model.OrderBook;
import org.example.data.model.OrderBookEntry;
import org.example.data.model.Ticker;
import org.example.data.model.TradingPair;
import org.example.data.fee.Fee;
import org.example.data.fee.ExchangeFeeFactory;
import org.example.data.fee.TransactionFee;
import org.example.data.interfaces.INotificationService;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Binance exchange service implementation.
 * Follows SOLID principles with proper dependency injection and no direct console output.
 */
public class BinanceExchangeService extends ExchangeService {

    // API endpoints
    private static final String BASE_URL = "https://api.binance.com";
    private static final String WS_BASE_URL = "wss://stream.binance.com/ws";

    // HTTP and WebSocket clients
    private HttpClient wsClient;
    private WebSocket webSocket;
    private BinanceWebSocketListener webSocketListener;

    // Add these fee-related fields
    private Fee bnbMakerFee;
    private Fee bnbTakerFee;
    private Fee nonBnbMakerFee;
    private Fee nonBnbTakerFee;
    
    /**
     * Constructor with notification service.
     *
     * @param fees Default fee rate
     * @param notificationService Notification service for logging
     */
    public BinanceExchangeService(double fees, INotificationService notificationService) {
        super("Binance", fees);
        setNotificationService(notificationService);
        
        // Initialize websocket client
        this.wsClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
                
        // Initialize special fee handling for BNB pairs
        initializeSpecialFees();
    }
    
    /**
     * Simple constructor without notification service.
     *
     * @param fees Default fee rate
     */
    public BinanceExchangeService(double fees) {
        this(fees, null);
    }
    
    /**
     * Initialize special fees for BNB pairs vs regular pairs.
     * This is needed because BNB pairs have different fee structures.
     */
    private void initializeSpecialFees() {
        // Standard fee factory
        ExchangeFeeFactory feeFactory = ExchangeFeeFactory.getInstance();
        
        // Get the current 30-day volume
        double volume = getThirtyDayTradingVolume();
        
        // Check if BNB discount is enabled
        boolean hasBnbDiscount = super.hasBnbDiscount;
        
        // Create specialized fees for BNB pairs
        this.bnbMakerFee = feeFactory.createBinanceFee(volume, true, true, false);
        this.bnbTakerFee = feeFactory.createBinanceFee(volume, false, true, false);
        
        // Create specialized fees for non-BNB pairs
        this.nonBnbMakerFee = feeFactory.createBinanceFee(volume, true, false, hasBnbDiscount);
        this.nonBnbTakerFee = feeFactory.createBinanceFee(volume, false, false, hasBnbDiscount);
        
        logInfo("Initialized BNB and non-BNB specific fees");
    }
    
    /**
     * Get the appropriate maker fee based on the trading pair.
     * 
     * @param tradingPair The trading pair
     * @return The appropriate maker fee
     */
    public Fee getMakerFee(String tradingPair) {
        boolean isBnbPair = tradingPair != null && 
                          (tradingPair.startsWith("BNB") || tradingPair.endsWith("BNB"));
                          
        return isBnbPair ? bnbMakerFee : nonBnbMakerFee;
    }
    
    /**
     * Get the appropriate taker fee based on the trading pair.
     * 
     * @param tradingPair The trading pair
     * @return The appropriate taker fee
     */
    public Fee getTakerFee(String tradingPair) {
        boolean isBnbPair = tradingPair != null && 
                          (tradingPair.startsWith("BNB") || tradingPair.endsWith("BNB"));
                          
        return isBnbPair ? bnbTakerFee : nonBnbTakerFee;
    }
    
    /**
     * Calculate and track a fee for a transaction.
     * This overrides the base method to handle BNB pairs specially.
     * 
     * @param tradingPair The trading pair
     * @param amount The transaction amount
     * @param isMaker Whether this is a maker order
     * @return The calculated fee amount
     */
    @Override
    public double calculateAndTrackFee(String tradingPair, double amount, boolean isMaker) {
        // Choose the appropriate fee based on pair and order type
        Fee fee = isMaker ? getMakerFee(tradingPair) : getTakerFee(tradingPair);
        
        // Calculate the fee
        double feeAmount = fee.calculateFee(amount);
        
        // Get the effective fee percentage
        double feePercentage;
        try {
            if (fee instanceof org.example.data.fee.PercentageFee) {
                feePercentage = ((org.example.data.fee.PercentageFee) fee).getPercentage();
            } else {
                // For other fee types, calculate percentage from amount
                feePercentage = amount > 0 ? feeAmount / amount : 0;
            }
        } catch (Exception e) {
            // Fallback if there's any casting error
            feePercentage = amount > 0 ? feeAmount / amount : 0;
            logWarning("Error getting fee percentage: " + e.getMessage());
        }
        
        // Create and track the fee
        TransactionFee transactionFee = new TransactionFee(
                "tx-" + System.currentTimeMillis(),
                getExchangeName(),
                tradingPair,
                feeAmount,
                fee.getType(),
                null,
                fee.getDescription(),
                feePercentage,
                hasBnbPaymentDiscount(tradingPair) ? 0.25 : 0.0,
                isMaker
        );
        
        getFeeTracker().trackFee(transactionFee);
        
        return feeAmount;
    }
    
    /**
     * Check if BNB payment discount applies to this trading pair.
     * 
     * @param tradingPair The trading pair
     * @return true if BNB payment discount applies
     */
    private boolean hasBnbPaymentDiscount(String tradingPair) {
        // BNB discount applies only when:
        // 1. BNB payment is enabled, and
        // 2. The trading pair itself is not a BNB pair (as BNB pairs already have reduced fees)
        boolean isBnbPair = tradingPair != null && 
                          (tradingPair.startsWith("BNB") || tradingPair.endsWith("BNB"));
                          
        return !isBnbPair && super.hasBnbDiscount;
    }
    
    /**
     * Update fee tiers based on trading volume.
     * This overrides the base method to update both BNB and non-BNB fees.
     * 
     * @param thirtyDayVolume The 30-day trading volume
     */
    @Override
    public void updateFeesTiers(double thirtyDayVolume) {
        super.updateFeesTiers(thirtyDayVolume);
        
        // Also update specialized fees
        ExchangeFeeFactory feeFactory = ExchangeFeeFactory.getInstance();
        
        // Update BNB pair fees
        this.bnbMakerFee = feeFactory.createBinanceFee(thirtyDayVolume, true, true, false);
        this.bnbTakerFee = feeFactory.createBinanceFee(thirtyDayVolume, false, true, false);
        
        // Update non-BNB pair fees
        this.nonBnbMakerFee = feeFactory.createBinanceFee(thirtyDayVolume, true, false, super.hasBnbDiscount);
        this.nonBnbTakerFee = feeFactory.createBinanceFee(thirtyDayVolume, false, false, super.hasBnbDiscount);
        
        logInfo("Updated BNB and non-BNB fees for volume: $" + thirtyDayVolume);
    }
    
    @Override
    public List<TradingPair> fetchTradingPairs() {
        List<TradingPair> pairs = new ArrayList<>();
        
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/v3/exchangeInfo"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
                JSONArray symbols = json.getJSONArray("symbols");
                
                for (int i = 0; i < symbols.length(); i++) {
                    JSONObject symbol = symbols.getJSONObject(i);
                    if ("TRADING".equals(symbol.getString("status"))) {
                        String baseAsset = symbol.getString("baseAsset");
                        String quoteAsset = symbol.getString("quoteAsset");
                        pairs.add(new TradingPair(baseAsset, quoteAsset));
                    }
                }
                
                setTradingPairs(pairs);
                logInfo("Fetched " + pairs.size() + " trading pairs from Binance");
            } else {
                logError("Failed to fetch trading pairs: " + response.statusCode(), null);
            }
        } catch (IOException | InterruptedException e) {
            logError("Error fetching trading pairs", e);
        } catch (JSONException e) {
            logError("Error parsing trading pairs JSON", e);
        }
        
        return pairs;
    }
    
    @Override
    protected Ticker fetchTickerDataREST(String symbol) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/v3/ticker/bookTicker?symbol=" + symbol))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());
            double bidPrice = json.getDouble("bidPrice");
            double askPrice = json.getDouble("askPrice");
                
                // Get 24h volume from a separate endpoint
                HttpRequest volumeRequest = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/api/v3/ticker/24hr?symbol=" + symbol))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .GET()
                        .build();
                
                HttpResponse<String> volumeResponse = client.send(volumeRequest, HttpResponse.BodyHandlers.ofString());
                
                double volume = 0;
                double lastPrice = 0;
                if (volumeResponse.statusCode() == 200) {
                    JSONObject volumeJson = new JSONObject(volumeResponse.body());
                    volume = volumeJson.getDouble("volume");
                    lastPrice = volumeJson.getDouble("lastPrice");
                }
                
                return new Ticker(bidPrice, askPrice, lastPrice, volume, new Date());
            } else {
                logWarning("Failed to fetch ticker for " + symbol + ": " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            logError("Error fetching ticker data for " + symbol, e);
        } catch (JSONException e) {
            logError("Error parsing ticker JSON for " + symbol, e);
        }
        
        return null;
    }
    
    @Override
    protected OrderBook fetchOrderBookREST(String symbol) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/v3/depth?symbol=" + symbol + "&limit=20"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JSONObject json = new JSONObject(response.body());

            List<OrderBookEntry> bids = new ArrayList<>();
                JSONArray bidsArray = json.getJSONArray("bids");
            for (int i = 0; i < bidsArray.length(); i++) {
                    JSONArray bid = bidsArray.getJSONArray(i);
                    double price = bid.getDouble(0);
                    double amount = bid.getDouble(1);
                    bids.add(new OrderBookEntry(price, amount));
            }

            List<OrderBookEntry> asks = new ArrayList<>();
                JSONArray asksArray = json.getJSONArray("asks");
            for (int i = 0; i < asksArray.length(); i++) {
                    JSONArray ask = asksArray.getJSONArray(i);
                    double price = ask.getDouble(0);
                    double amount = ask.getDouble(1);
                    asks.add(new OrderBookEntry(price, amount));
                }
                
                OrderBook orderBook = new OrderBook(symbol, bids, asks, new Date());
                logDebug("Fetched order book for " + symbol + " with " + bids.size() + " bids and " + asks.size() + " asks");
                return orderBook;
            } else {
                logWarning("Failed to fetch order book for " + symbol + ": " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            logError("Error fetching order book for " + symbol, e);
        } catch (JSONException e) {
            logError("Error parsing order book JSON for " + symbol, e);
        }
        
        return null;
    }
    
    @Override
    public boolean initializeWebSocket(List<String> symbols) {
        if (webSocket != null && websocketConnected) {
            logInfo("WebSocket already connected");
            return true;
        }
        
        try {
            StringBuilder streams = new StringBuilder();
            for (int i = 0; i < symbols.size(); i++) {
                String symbol = symbols.get(i).toLowerCase();
                streams.append(symbol).append("@bookTicker");
                if (i < symbols.size() - 1) {
                    streams.append("/");
                }
            }
            
            String wsUrl = WS_BASE_URL + "/" + streams.toString();
            logDebug("Connecting to WebSocket: " + wsUrl);
            
            webSocketListener = new BinanceWebSocketListener();
            webSocket = wsClient.newWebSocketBuilder()
                    .buildAsync(URI.create(wsUrl), webSocketListener)
                    .get();
            
            websocketConnected = true;
            logInfo("WebSocket connection established for " + symbols.size() + " symbols");
            return true;
        } catch (InterruptedException | ExecutionException e) {
            logError("Error initializing WebSocket", e);
            websocketConnected = false;
            return false;
        }
    }

    @Override
    public void closeWebSocket() {
        if (webSocket != null) {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Closing connection");
                websocketConnected = false;
            logInfo("WebSocket connection closed");
        }
    }

    /**
     * WebSocket listener for Binance.
     */
    private class BinanceWebSocketListener implements WebSocket.Listener {
        private StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            logDebug("WebSocket connection opened");
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                processMessage(buffer.toString());
                buffer.setLength(0);
                webSocket.request(1);
            }
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            websocketConnected = false;
            logInfo("WebSocket closed: " + statusCode + " " + reason);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            websocketConnected = false;
            logError("WebSocket error", error);
        }

        /**
         * Process WebSocket message.
         *
         * @param message The message received
         */
        private void processMessage(String message) {
            try {
                JSONObject json = new JSONObject(message);

                if (json.has("s") && json.has("b") && json.has("a")) {
                    String symbol = json.getString("s");
                        double bidPrice = json.getDouble("b");
                        double askPrice = json.getDouble("a");
                    double bidQty = json.getDouble("B");
                    double askQty = json.getDouble("A");
                    
                    // Update ticker cache
                    Ticker existingTicker = tickerCache.get(symbol);
                    double lastPrice = existingTicker != null ? existingTicker.getLastPrice() : 0;
                    double volume = existingTicker != null ? existingTicker.getVolume() : 0;
                    
                    Ticker updatedTicker = new Ticker(bidPrice, askPrice, lastPrice, volume, new Date());
                    tickerCache.put(symbol, updatedTicker);
                    
                    // Create a simple order book with just the best bid and ask
                        List<OrderBookEntry> bids = new ArrayList<>();
                    bids.add(new OrderBookEntry(bidPrice, bidQty));

                        List<OrderBookEntry> asks = new ArrayList<>();
                    asks.add(new OrderBookEntry(askPrice, askQty));
                    
                    OrderBook updatedOrderBook = new OrderBook(symbol, bids, asks, new Date());
                    orderBookCache.put(symbol, updatedOrderBook);
                    
                    logDebug("Updated ticker and order book for " + symbol);
                }
            } catch (JSONException e) {
                logError("Error parsing WebSocket message", e);
            }
        }
    }
    
    /**
     * Get the current 30-day trading volume.
     * This method could be enhanced to fetch real volume from the API.
     * 
     * @return The 30-day trading volume for fee tier calculations
     */
    private double getThirtyDayTradingVolume() {
        // For now, use the volume from the parent class
        return super.thirtyDayTradingVolume;
    }
}