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
 *
 * Important messages that were previously output to the console are now accumulated
 * in a log, which can be retrieved via getLogMessages().
 */
public class BinanceExchangeService extends ExchangeService {

    private static final String BASE_URL = "https://api.binance.com";
    private static final String WS_BASE_URL = "wss://stream.binance.com/ws";

    // WebSocket client and connection
    private HttpClient wsClient;
    private WebSocket webSocket;
    private BinanceWebSocketListener webSocketListener;

    // Accumulates important log messages.
    private StringBuilder logBuilder = new StringBuilder();

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
     * Returns the accumulated log messages as a String.
     *
     * @return a String containing log messages.
     */
    public String getLogMessages() {
        return logBuilder.toString();
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
            logBuilder.append("Error fetching ticker data for ").append(symbol).append(": ")
                    .append(e.getMessage()).append("\n");
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
            logBuilder.append("Error fetching order book for ").append(symbol).append(": ")
                    .append(e.getMessage()).append("\n");
            System.err.println("Error fetching order book for " + symbol + ": " + e.getMessage());
        }
        return orderBook;
    }

    /**
     * Initializes WebSocket connections for market data streaming from Binance.
     *
     * @param symbols List of symbols to subscribe to.
     * @return true if successfully connected, false otherwise.
     */
    @Override
    public boolean initializeWebSocket(List<String> symbols) {
        try {
            if (symbols == null || symbols.isEmpty()) {
                return false;
            }

            // Close any existing WebSocket connection.
            closeWebSocket();

            // Create a new WebSocket client and connect.
            wsClient = HttpClient.newHttpClient();

            // Build proper WebSocket subscription message for Binance.
            List<String> streams = new ArrayList<>();
            for (String symbol : symbols) {
                String formattedSymbol = symbol.toLowerCase(); // Binance requires lowercase for streams.
                streams.add(formattedSymbol + "@trade");    // For trade data.
                streams.add(formattedSymbol + "@depth20");    // For order book data.
            }

            JSONObject subscriptionMsg = new JSONObject();
            subscriptionMsg.put("method", "SUBSCRIBE");
            subscriptionMsg.put("params", streams);
            subscriptionMsg.put("id", 1);
            String subscriptionString = subscriptionMsg.toString();

            // Build WebSocket.
            CompletableFuture<WebSocket> webSocketFuture = wsClient.newWebSocketBuilder()
                    .buildAsync(URI.create(WS_BASE_URL), new BinanceWebSocketListener());

            // Wait for connection to complete.
            webSocket = webSocketFuture.get(10, TimeUnit.SECONDS);

            // Send subscription message.
            webSocket.sendText(subscriptionString, true);
            logBuilder.append("Binance WebSocket connection and subscription successful.\n");
            return true;
        } catch (Exception e) {
            logBuilder.append("Error initializing Binance WebSocket: ").append(e.getMessage()).append("\n");
            System.err.println("Error initializing Binance WebSocket: " + e.getMessage());
            e.printStackTrace();
            closeWebSocket();
            return false;
        }
    }

    /**
     * Closes the WebSocket connection.
     */
    @Override
    public void closeWebSocket() {
        if (webSocket != null) {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Closing connection");
                websocketConnected = false;
                logBuilder.append("Binance WebSocket connection closed.\n");
            } catch (Exception e) {
                logBuilder.append("Error closing Binance WebSocket: ").append(e.getMessage()).append("\n");
                System.err.println("Error closing Binance WebSocket: " + e.getMessage());
            }
        }
    }

    /**
     * WebSocket listener for Binance data.
     */
    private class BinanceWebSocketListener implements WebSocket.Listener {
        private StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            logBuilder.append("Binance WebSocket connection opened.\n");
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
                    logBuilder.append("Error processing Binance WebSocket message: ")
                            .append(e.getMessage()).append("\n");
                    System.err.println("Error processing Binance WebSocket message: " + e.getMessage());
                }
            }
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            logBuilder.append("Binance WebSocket closed: ").append(statusCode)
                    .append(", reason: ").append(reason).append("\n");
            websocketConnected = false;
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            logBuilder.append("Binance WebSocket error: ").append(error.getMessage()).append("\n");
            System.err.println("Binance WebSocket error: " + error.getMessage());
            error.printStackTrace();
            websocketConnected = false;
            WebSocket.Listener.super.onError(webSocket, error);
        }

        /**
         * Process the WebSocket message and update the cache.
         */
        private void processMessage(String message) {
            try {
                // Check for error messages.
                if (message.contains("error")) {
                    logBuilder.append("Binance WebSocket error message: ").append(message).append("\n");
                    return;
                }

                JSONObject json = new JSONObject(message);

                // Single stream format.
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
                }
                // Combined streams format.
                else if (json.has("data") && json.has("stream")) {
                    String stream = json.getString("stream");
                    JSONObject data = json.getJSONObject("data");

                    if (stream.contains("@ticker")) {
                        String symbol = data.getString("s");
                        double bidPrice = data.getDouble("b");
                        double askPrice = data.getDouble("a");
                        double lastPrice = data.getDouble("c");
                        double volume = data.getDouble("v");

                        Ticker ticker = new Ticker(bidPrice, askPrice, lastPrice, volume, new Date());
                        tickerCache.put(symbol, ticker);
                    }
                    else if (stream.contains("@depth")) {
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
                    logBuilder.append("Unrecognized Binance WebSocket message format: ").append(message).append("\n");
                }
            } catch (Exception e) {
                logBuilder.append("Error parsing Binance WebSocket message: ").append(message)
                        .append(" | Exception: ").append(e.getMessage()).append("\n");
                e.printStackTrace();
            }
        }
    }
}