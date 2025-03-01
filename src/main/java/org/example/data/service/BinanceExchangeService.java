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
 * BinanceExchangeService provides concrete implementations of the abstract
 * methods in ExchangeService using Binance API endpoints.
 */
public class BinanceExchangeService extends ExchangeService {

    // Updated WebSocket URL (without port specification that might cause issues)
    private static final String BASE_URL = "https://api.binance.com";
    private static final String WS_BASE_URL = "wss://stream.binance.com/ws";
    
    // WebSocket client and connection
    private HttpClient wsClient;
    private WebSocket webSocket;
    private BinanceWebSocketListener webSocketListener;

    /**
     * Constructs a BinanceExchangeService instance.
     *
     * @param fees The trading fee as a percentage (e.g., 0.001 for 0.1%)
     */
    public BinanceExchangeService(double fees) {
        // "Binance" is the exchange name
        super("Binance", fees);
        this.wsClient = HttpClient.newHttpClient();
        this.webSocketListener = new BinanceWebSocketListener();
    }

    /**
     * Fetches and caches the list of trading pairs available on Binance.
     *
     * Endpoint: GET /api/v3/exchangeInfo
     *
     * @return A list of TradingPair objects.
     */
    @Override
    public List<TradingPair> fetchTradingPairs() {
        List<TradingPair> tradingPairs = new ArrayList<>();
        try {
            String urlStr = BASE_URL + "/api/v3/exchangeInfo";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Read the response
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder responseStr = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                responseStr.append(inputLine);
            }
            in.close();

            // Parse JSON response
            JSONObject json = new JSONObject(responseStr.toString());
            JSONArray symbols = json.getJSONArray("symbols");
            for (int i = 0; i < symbols.length(); i++) {
                JSONObject symbolObj = symbols.getJSONObject(i);
                // Only include symbols that are actively trading.
                if (symbolObj.getString("status").equalsIgnoreCase("TRADING")) {
                    // With a simplified TradingPair model, we only store the symbol.
                    String symbol = symbolObj.getString("symbol");
                    TradingPair pair = new TradingPair(symbol);
                    tradingPairs.add(pair);
                }
            }
            // Update internal cache in the abstract ExchangeService
            setTradingPairs(tradingPairs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tradingPairs;
    }

    /**
     * Retrieves the latest ticker data for the specified symbol using REST API.
     * This is used as a fallback when WebSocket data is not available.
     *
     * Endpoint: GET /api/v3/ticker/24hr?symbol={symbol}
     *
     * @param symbol The trading symbol (e.g., "BTCUSDT").
     * @return A Ticker object containing bid, ask, last prices, etc.
     */
    @Override
    protected Ticker fetchTickerDataREST(String symbol) {
        Ticker ticker = null;
        try {
            String urlStr = BASE_URL + "/api/v3/ticker/24hr?symbol=" + symbol;
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
            double bidPrice = json.getDouble("bidPrice");
            double askPrice = json.getDouble("askPrice");
            double lastPrice = json.getDouble("lastPrice");
            double volume = json.getDouble("volume");
            Date timestamp = new Date(); // Alternatively, parse a timestamp if provided in the JSON

            ticker = new Ticker(bidPrice, askPrice, lastPrice, volume, timestamp);
        } catch (Exception e) {
            System.err.println("Error fetching ticker data for " + symbol + ": " + e.getMessage());
        }
        return ticker;
    }

    /**
     * Retrieves the current order book for the specified trading pair using REST API.
     * This is used as a fallback when WebSocket data is not available.
     *
     * Endpoint: GET /api/v3/depth?symbol={symbol}&limit=5
     *
     * @param symbol The trading symbol (e.g., "BTCUSDT").
     * @return An OrderBook object with bids and asks.
     */
    @Override
    protected OrderBook fetchOrderBookREST(String symbol) {
        OrderBook orderBook = null;
        try {
            String urlStr = BASE_URL + "/api/v3/depth?symbol=" + symbol;
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
                bids.add(new OrderBookEntry(price, volume));
            }

            List<OrderBookEntry> asks = new ArrayList<>();
            for (int i = 0; i < asksArray.length(); i++) {
                JSONArray entry = asksArray.getJSONArray(i);
                double price = Double.parseDouble(entry.getString(0));
                double volume = Double.parseDouble(entry.getString(1));
                asks.add(new OrderBookEntry(price, volume));
            }

            Date timestamp = new Date(); // Using current time for the snapshot
            orderBook = new OrderBook(symbol, bids, asks, timestamp);
        } catch (Exception e) {
            System.err.println("Error fetching order book for " + symbol + ": " + e.getMessage());
        }
        return orderBook;
    }
    
    /**
     * Initializes WebSocket connections for market data streaming from Binance
     *
     * @param symbols List of symbols to subscribe to
     * @return true if successfully connected, false otherwise
     */
    @Override
    public boolean initializeWebSocket(List<String> symbols) {
        try {
            if (symbols == null || symbols.isEmpty()) {
                return false;
            }
            
            // Close any existing WebSocket connection
            closeWebSocket();
            
            // Create a new WebSocket client and connect
            wsClient = HttpClient.newHttpClient();
            
            // Log the WebSocket URL and symbols for debugging
            System.out.println("Connecting to Binance WebSocket at: " + WS_BASE_URL);
            System.out.println("Subscribing to symbols: " + symbols);
            
            // Build proper WebSocket subscription message for Binance
            // This is the Binance-specific format for subscribing to trade and depth (order book) streams
            List<String> streams = new ArrayList<>();
            for (String symbol : symbols) {
                // Convert symbol to lowercase for Binance
                String formattedSymbol = symbol.toLowerCase();
                streams.add(formattedSymbol + "@trade");    // For trade data
                streams.add(formattedSymbol + "@depth20");  // For order book data
            }
            
            // Create subscription message in the correct Binance format
            JSONObject subscriptionMsg = new JSONObject();
            subscriptionMsg.put("method", "SUBSCRIBE");
            subscriptionMsg.put("params", streams);
            subscriptionMsg.put("id", 1);
            String subscriptionString = subscriptionMsg.toString();
            
            // Build WebSocket
            CompletableFuture<WebSocket> webSocketFuture = wsClient.newWebSocketBuilder()
                    .buildAsync(URI.create(WS_BASE_URL), new BinanceWebSocketListener());
            
            // Wait for connection to complete
            webSocket = webSocketFuture.get(10, TimeUnit.SECONDS);
            
            // Send subscription message
            System.out.println("Sending subscription message: " + subscriptionString);
            webSocket.sendText(subscriptionString, true);
            
            System.out.println("Binance WebSocket connection and subscription successful");
            return true;
        } catch (Exception e) {
            System.err.println("Error initializing Binance WebSocket: " + e.getMessage());
            e.printStackTrace();
            closeWebSocket();
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
                System.out.println("Binance WebSocket connection closed");
            } catch (Exception e) {
                System.err.println("Error closing Binance WebSocket: " + e.getMessage());
            }
        }
    }
    
    /**
     * WebSocket listener for Binance data
     */
    private class BinanceWebSocketListener implements WebSocket.Listener {
        private StringBuilder buffer = new StringBuilder();
        
        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("Binance WebSocket connection opened");
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
                    System.err.println("Error processing Binance WebSocket message: " + e.getMessage());
                }
            }
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }
        
        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            System.out.println("Binance WebSocket closed: " + statusCode + ", reason: " + reason);
            websocketConnected = false;
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }
        
        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.err.println("Binance WebSocket error: " + error.getMessage());
            error.printStackTrace();
            websocketConnected = false;
            WebSocket.Listener.super.onError(webSocket, error);
        }
        
        /**
         * Process the WebSocket message and update the cache
         */
        private void processMessage(String message) {
            try {
                // Check if we're dealing with a simple error message
                if (message.contains("error")) {
                    System.err.println("Binance WebSocket error message: " + message);
                    return;
                }
                
                JSONObject json = new JSONObject(message);
                
                // Single stream format
                if (json.has("e") && json.has("s")) {
                    String eventType = json.getString("e");
                    String symbol = json.getString("s");
                    
                    if ("24hrTicker".equals(eventType)) {
                        double bidPrice = json.getDouble("b");
                        double askPrice = json.getDouble("a");
                        double lastPrice = json.getDouble("c");
                        double volume = json.getDouble("v");
                        
                        Ticker ticker = new Ticker(bidPrice, askPrice, lastPrice, volume, new Date());
                        tickerCache.put(symbol, ticker);
                    }
                    // Handle other single stream formats if needed
                }
                // Combined streams format
                else if (json.has("data") && json.has("stream")) {
                    String stream = json.getString("stream");
                    JSONObject data = json.getJSONObject("data");
                    
                    if (stream.contains("@ticker")) {
                        // Parse ticker data
                        String symbol = data.getString("s"); // Symbol
                        double bidPrice = data.getDouble("b"); // Best bid price
                        double askPrice = data.getDouble("a"); // Best ask price
                        double lastPrice = data.getDouble("c"); // Close price
                        double volume = data.getDouble("v"); // Total trading volume
                        
                        Ticker ticker = new Ticker(bidPrice, askPrice, lastPrice, volume, new Date());
                        tickerCache.put(symbol, ticker);
                    } 
                    else if (stream.contains("@depth")) {
                        // Parse order book data
                        String symbol = data.getString("s");
                        JSONArray bidsArray = data.getJSONArray("bids");
                        JSONArray asksArray = data.getJSONArray("asks");
                        
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
                } else {
                    System.out.println("Unrecognized Binance WebSocket message format: " + message);
                }
            } catch (Exception e) {
                System.err.println("Error parsing Binance WebSocket message: " + message);
                e.printStackTrace();
            }
        }
    }
}
