package org.example.data.service;

import org.example.data.model.OrderBook;
import org.example.data.model.OrderBookEntry;
import org.example.data.model.Ticker;
import org.example.data.model.TradingPair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * CoinbaseExchangeService provides implementations for fetching data from Coinbase's API.
 * <p>
 * Endpoints used:
 * - Trading Pairs: GET https://api.exchange.coinbase.com/products
 * - Ticker Data:  GET https://api.exchange.coinbase.com/products/{symbol}/ticker
 * - Order Book:   GET https://api.exchange.coinbase.com/products/{symbol}/book?level=2
 * - WebSocket:    wss://ws-feed.exchange.coinbase.com
 */
public class CoinbaseExchangeService extends ExchangeService {

    // Base URL for Coinbase endpoints
    private static final String BASE_URL = "https://api.exchange.coinbase.com";
    private static final String WS_BASE_URL = "wss://ws-feed.exchange.coinbase.com";
    
    // WebSocket client and connection
    private HttpClient wsClient;
    private WebSocket webSocket;
    private CoinbaseWebSocketListener webSocketListener;

    /**
     * Constructs a CoinbaseExchangeService instance.
     *
     * @param fees The trading fee as a percentage (e.g., 0.001 for 0.1%).
     */
    public CoinbaseExchangeService(double fees) {
        // "Coinbase" is the exchange name
        super("Coinbase", fees);
        this.wsClient = HttpClient.newHttpClient();
        this.webSocketListener = new CoinbaseWebSocketListener();
    }

    /**
     * Fetches and caches the list of trading pairs (products) available on Coinbase.
     * Endpoint: GET https://api.exchange.coinbase.com/products
     *
     * @return A list of TradingPair objects.
     */
    @Override
    public List<TradingPair> fetchTradingPairs() {
        List<TradingPair> tradingPairs = new ArrayList<>();
        try {
            String urlStr = BASE_URL + "/products";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Read the response
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseStr = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                responseStr.append(inputLine);
            }
            in.close();

            // Parse JSON response (an array of product objects)
            JSONArray products = new JSONArray(responseStr.toString());
            for (int i = 0; i < products.length(); i++) {
                JSONObject productObj = products.getJSONObject(i);
                // Optionally, filter by status if desired (e.g., "online")
                String status = productObj.optString("status", "online");
                if ("online".equalsIgnoreCase(status)) {
                    // Use the product "id" as the trading pair symbol (e.g., "ARB-USD")
                    String symbol = productObj.getString("id");
                    TradingPair pair = new TradingPair(symbol);
                    tradingPairs.add(pair);
                }
            }
            // Update the internal cache in ExchangeService
            setTradingPairs(tradingPairs);
        } catch (Exception e) {
            System.err.println("Error fetching trading pairs from Coinbase: " + e.getMessage());
            e.printStackTrace();
        }
        return tradingPairs;
    }

    /**
     * Retrieves the latest ticker data for the specified symbol using REST API.
     * This is used as a fallback when WebSocket data is not available.
     * 
     * Endpoint: GET https://api.exchange.coinbase.com/products/{symbol}/ticker
     *
     * @param symbol The trading pair symbol (e.g., "ARB-USD").
     * @return A Ticker object containing bid, ask, price, volume, and timestamp.
     */
    @Override
    protected Ticker fetchTickerDataREST(String symbol) {
        Ticker ticker = null;
        try {
            String urlStr = BASE_URL + "/products/" + symbol + "/ticker";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseStr = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                responseStr.append(inputLine);
            }
            in.close();

            JSONObject json = new JSONObject(responseStr.toString());
            double bid = json.getDouble("bid");
            double ask = json.getDouble("ask");
            double price = json.getDouble("price");
            double volume = json.getDouble("volume");
            // Parse the "time" field if needed; here we use the current time as a placeholder.
            Date timestamp = new Date();

            ticker = new Ticker(bid, ask, price, volume, timestamp);
        } catch (Exception e) {
            System.err.println("Error fetching ticker data from Coinbase for " + symbol + ": " + e.getMessage());
        }
        return ticker;
    }

    /**
     * Retrieves the current order book for the specified trading pair using REST API.
     * This is used as a fallback when WebSocket data is not available.
     * 
     * Endpoint: GET https://api.exchange.coinbase.com/products/{symbol}/book?level=2
     *
     * @param symbol The trading pair symbol (e.g., "ARB-USD").
     * @return An OrderBook object with bids and asks.
     */
    @Override
    protected OrderBook fetchOrderBookREST(String symbol) {
        OrderBook orderBook = null;
        try {
            String urlStr = BASE_URL + "/products/" + symbol + "/book?level=2";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseStr = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                responseStr.append(inputLine);
            }
            in.close();

            JSONObject json = new JSONObject(responseStr.toString());
            JSONArray bidsArray = json.getJSONArray("bids");
            JSONArray asksArray = json.getJSONArray("asks");

            List<OrderBookEntry> bids = new ArrayList<>();
            for (int i = 0; i < bidsArray.length(); i++) {
                JSONArray entry = bidsArray.getJSONArray(i);
                double price = Double.parseDouble(entry.getString(0));
                double volume = Double.parseDouble(entry.getString(1));
                // The third element (number of orders) can be ignored if not needed.
                bids.add(new OrderBookEntry(price, volume));
            }

            List<OrderBookEntry> asks = new ArrayList<>();
            for (int i = 0; i < asksArray.length(); i++) {
                JSONArray entry = asksArray.getJSONArray(i);
                double price = Double.parseDouble(entry.getString(0));
                double volume = Double.parseDouble(entry.getString(1));
                asks.add(new OrderBookEntry(price, volume));
            }

            Date timestamp = new Date();
            orderBook = new OrderBook(symbol, bids, asks, timestamp);
        } catch (Exception e) {
            System.err.println("Error fetching order book from Coinbase for " + symbol + ": " + e.getMessage());
        }
        return orderBook;
    }
    
    /**
     * Initializes WebSocket connections for market data streaming from Coinbase
     *
     * @param symbols List of symbols to subscribe to
     * @return true if successfully connected, false otherwise
     */
    @Override
    public boolean initializeWebSocket(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            System.err.println("No symbols provided for Coinbase WebSocket initialization");
            return false;
        }
        
        try {
            // Close existing connection if any
            if (webSocket != null) {
                closeWebSocket();
            }
            
            System.out.println("Connecting to Coinbase WebSocket...");
            
            // Connect to Coinbase WebSocket stream with timeout
            CompletableFuture<WebSocket> futureWs = wsClient.newWebSocketBuilder()
                    .buildAsync(URI.create(WS_BASE_URL), webSocketListener);
            
            webSocket = futureWs.get(10, TimeUnit.SECONDS);
            
            // Create subscription message for ticker and level2 (order book) channels
            JSONObject subscribeMsg = new JSONObject();
            subscribeMsg.put("type", "subscribe");
            
            // Print the symbols we're subscribing to
            System.out.println("Subscribing to Coinbase products: " + symbols);
            
            // Add product_ids (symbols)
            JSONArray productIds = new JSONArray();
            for (String symbol : symbols) {
                productIds.put(symbol);
            }
            subscribeMsg.put("product_ids", productIds);
            
            // Add channels
            JSONArray channels = new JSONArray();
            channels.put("ticker");
            channels.put("level2");
            subscribeMsg.put("channels", channels);
            
            // Send subscription message
            String subMessage = subscribeMsg.toString();
            System.out.println("Sending Coinbase subscription message: " + subMessage);
            webSocket.sendText(subMessage, true);
            
            websocketConnected = true;
            System.out.println("Coinbase WebSocket connected for symbols: " + symbols);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to connect to Coinbase WebSocket: " + e.getMessage());
            e.printStackTrace();
            websocketConnected = false;
            return false;
        }
    }
    
    /**
     * Closes the WebSocket connection
     */
    @Override
    public void closeWebSocket() {
        if (webSocket != null) {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Closing connection");
                websocketConnected = false;
                System.out.println("Coinbase WebSocket connection closed");
            } catch (Exception e) {
                System.err.println("Error closing Coinbase WebSocket: " + e.getMessage());
            }
        }
    }
    
    /**
     * WebSocket listener for Coinbase data
     */
    private class CoinbaseWebSocketListener implements WebSocket.Listener {
        private StringBuilder buffer = new StringBuilder();
        
        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("Coinbase WebSocket connection opened");
            WebSocket.Listener.super.onOpen(webSocket);
        }
        
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String message = buffer.toString();
                buffer = new StringBuilder();
                try {
                    processMessage(message);
                } catch (Exception e) {
                    System.err.println("Error processing Coinbase WebSocket message: " + e.getMessage());
                }
            }
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }
        
        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            System.out.println("Coinbase WebSocket closed: " + statusCode + ", reason: " + reason);
            websocketConnected = false;
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }
        
        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.err.println("Coinbase WebSocket error: " + error.getMessage());
            error.printStackTrace();
            websocketConnected = false;
            WebSocket.Listener.super.onError(webSocket, error);
        }
        
        /**
         * Process the WebSocket message and update the cache
         */
        private void processMessage(String message) {
            try {
                if (message.contains("error")) {
                    System.err.println("Coinbase WebSocket error message: " + message);
                    return;
                }
                
                JSONObject json = new JSONObject(message);
                String type = json.optString("type", "");
                
                if (type.equals("subscriptions")) {
                    System.out.println("Coinbase subscription confirmed: " + json.toString());
                }
                else if (type.equals("ticker")) {
                    // Process ticker update
                    String symbol = json.getString("product_id");
                    double price = json.getDouble("price");
                    double bid = json.getDouble("best_bid");
                    double ask = json.getDouble("best_ask");
                    double volume = json.getDouble("volume_24h");
                    
                    Ticker ticker = new Ticker(bid, ask, price, volume, new Date());
                    tickerCache.put(symbol, ticker);
                }
                else if (type.equals("snapshot")) {
                    // Initial order book snapshot
                    String symbol = json.getString("product_id");
                    JSONArray bidsArray = json.getJSONArray("bids");
                    JSONArray asksArray = json.getJSONArray("asks");
                    
                    List<OrderBookEntry> bids = new ArrayList<>();
                    for (int i = 0; i < bidsArray.length(); i++) {
                        JSONArray entry = bidsArray.getJSONArray(i);
                        double price = Double.parseDouble(entry.getString(0));
                        double volume = Double.parseDouble(entry.getString(1));
                        bids.add(new OrderBookEntry(price, volume));
                    }
                    
                    List<OrderBookEntry> asks = new ArrayList<>();
                    for (int i = 0; i < asksArray.length(); i++) {
                        JSONArray entry = asksArray.getJSONArray(i);
                        double price = Double.parseDouble(entry.getString(0));
                        double volume = Double.parseDouble(entry.getString(1));
                        asks.add(new OrderBookEntry(price, volume));
                    }
                    
                    OrderBook orderBook = new OrderBook(symbol, bids, asks, new Date());
                    orderBookCache.put(symbol, orderBook);
                }
                else if (type.equals("l2update")) {
                    // Order book update - we need to update existing order book
                    String symbol = json.getString("product_id");
                    JSONArray changes = json.getJSONArray("changes");
                    
                    // Get current order book from cache or create a new one
                    OrderBook currentBook = orderBookCache.get(symbol);
                    if (currentBook == null) {
                        // If there's no existing order book, we need to fetch it first
                        currentBook = fetchOrderBookREST(symbol);
                        if (currentBook == null) {
                            return; // Can't process update without a base order book
                        }
                    }
                    
                    List<OrderBookEntry> bids = new ArrayList<>(currentBook.getBids());
                    List<OrderBookEntry> asks = new ArrayList<>(currentBook.getAsks());
                    
                    // Process each change
                    for (int i = 0; i < changes.length(); i++) {
                        JSONArray change = changes.getJSONArray(i);
                        String side = change.getString(0);
                        double price = Double.parseDouble(change.getString(1));
                        double size = Double.parseDouble(change.getString(2));
                        
                        if (side.equals("buy")) {
                            updateOrderBookSide(bids, price, size);
                        } else if (side.equals("sell")) {
                            updateOrderBookSide(asks, price, size);
                        }
                    }
                    
                    // Create updated order book and put in cache
                    OrderBook updatedBook = new OrderBook(symbol, bids, asks, new Date());
                    orderBookCache.put(symbol, updatedBook);
                }
                else {
                    // For debugging unexpected message types
                    System.out.println("Received unexpected Coinbase message type: " + type);
                }
            } catch (Exception e) {
                System.err.println("Error parsing Coinbase WebSocket message: " + message);
                e.printStackTrace();
            }
        }
        
        /**
         * Helper method to update a side (bids or asks) of an order book
         */
        private void updateOrderBookSide(List<OrderBookEntry> entries, double price, double size) {
            // Find if there's an existing entry with this price
            boolean found = false;
            for (int i = 0; i < entries.size(); i++) {
                OrderBookEntry entry = entries.get(i);
                if (entry.getPrice() == price) {
                    if (size > 0) {
                        // Update the size
                        entries.set(i, new OrderBookEntry(price, size));
                    } else {
                        // Remove the entry if size is 0
                        entries.remove(i);
                    }
                    found = true;
                    break;
                }
            }
            
            // If not found and size > 0, add new entry
            if (!found && size > 0) {
                entries.add(new OrderBookEntry(price, size));
            }
            
            // Sort bids in descending order by price
            entries.sort((e1, e2) -> Double.compare(e2.getPrice(), e1.getPrice()));
        }
    }
}
