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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * KrakenExchangeService provides concrete implementations for fetching market data
 * from Kraken's API.
 *
 * Endpoints used:
 * - Trading Pairs: GET https://api.kraken.com/0/public/AssetPairs
 * - Ticker Data:  GET https://api.kraken.com/0/public/Ticker?pair={symbol}
 * - Order Book:   GET https://api.kraken.com/0/public/Depth?pair={symbol}&count=10
 * - WebSocket:    wss://ws.kraken.com
 *
 * Important log messages are now accumulated in a log which can be retrieved
 * via the getLogMessages() method.
 */
public class KrakenExchangeService extends ExchangeService {

    private static final String BASE_URL = "https://api.kraken.com/0/public";
    private static final String WS_BASE_URL = "wss://ws.kraken.com";

    // Map to translate between Kraken's format and standard format
    private Map<String, String> krakenSymbolMap = new HashMap<>();

    // WebSocket client and connection
    private HttpClient wsClient;
    private WebSocket webSocket;
    private KrakenWebSocketListener webSocketListener;

    // Accumulates important log messages.
    private StringBuilder logBuilder = new StringBuilder();

    /**
     * Constructs a KrakenExchangeService instance.
     *
     * @param fees The trading fee as a percentage (e.g., 0.002 for 0.2%).
     */
    public KrakenExchangeService(double fees) {
        super("Kraken", fees);
        this.wsClient = HttpClient.newHttpClient();
        this.webSocketListener = new KrakenWebSocketListener();
    }

    /**
     * Returns the accumulated log messages as a String.
     *
     * @return A String containing log messages.
     */
    public String getLogMessages() {
        return logBuilder.toString();
    }

    /**
     * Fetches and caches the list of trading pairs available on Kraken.
     *
     * Endpoint: GET https://api.kraken.com/0/public/AssetPairs
     *
     * @return A list of TradingPair objects.
     */
    @Override
    public List<TradingPair> fetchTradingPairs() {
        List<TradingPair> tradingPairs = new ArrayList<>();
        try {
            String urlStr = BASE_URL + "/AssetPairs";
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
            if (json.has("error") && json.getJSONArray("error").length() > 0) {
                logBuilder.append("Error fetching Kraken trading pairs: ")
                        .append(json.getJSONArray("error").toString()).append("\n");
                return tradingPairs;
            }

            JSONObject result = json.getJSONObject("result");
            for (String key : result.keySet()) {
                JSONObject pairData = result.getJSONObject(key);
                String symbol = key;

                // Kraken may use different naming; track both forms.
                krakenSymbolMap.put(symbol, symbol);

                TradingPair pair = new TradingPair(symbol);
                tradingPairs.add(pair);
            }

            setTradingPairs(tradingPairs);

        } catch (Exception e) {
            logBuilder.append("Exception in fetchTradingPairs: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }
        return tradingPairs;
    }

    /**
     * Retrieves the latest ticker data for the specified symbol using REST API.
     * This is used as a fallback when WebSocket data is not available.
     *
     * Endpoint: GET https://api.kraken.com/0/public/Ticker?pair={symbol}
     *
     * @param symbol The trading symbol.
     * @return A Ticker object containing bid, ask, last prices, etc.
     */
    @Override
    protected Ticker fetchTickerDataREST(String symbol) {
        Ticker ticker = null;

        // Convert to Kraken format if needed
        String krakenSymbol = krakenSymbolMap.getOrDefault(symbol, symbol);

        try {
            String urlStr = BASE_URL + "/Ticker?pair=" + krakenSymbol;
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
            if (json.has("error") && json.getJSONArray("error").length() > 0) {
                logBuilder.append("Error fetching Kraken ticker: ")
                        .append(json.getJSONArray("error").toString()).append("\n");
                return null;
            }

            JSONObject result = json.getJSONObject("result");
            JSONObject tickerData = result.getJSONObject(krakenSymbol);

            JSONArray bidData = tickerData.getJSONArray("b");
            double bidPrice = bidData.getDouble(0);

            JSONArray askData = tickerData.getJSONArray("a");
            double askPrice = askData.getDouble(0);

            JSONArray lastData = tickerData.getJSONArray("c");
            double lastPrice = lastData.getDouble(0);

            JSONArray volumeData = tickerData.getJSONArray("v");
            double volume = volumeData.getDouble(1); // Using the 24h volume

            ticker = new Ticker(bidPrice, askPrice, lastPrice, volume, new Date());

        } catch (Exception e) {
            logBuilder.append("Exception in fetchTickerDataREST for symbol ").append(symbol)
                    .append(": ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }
        return ticker;
    }

    /**
     * Retrieves the current order book for the specified trading pair using REST API.
     * This is used as a fallback when WebSocket data is not available.
     *
     * Endpoint: GET https://api.kraken.com/0/public/Depth?pair={symbol}&count=10
     *
     * @param symbol The trading symbol.
     * @return An OrderBook object with bids and asks.
     */
    @Override
    protected OrderBook fetchOrderBookREST(String symbol) {
        OrderBook orderBook = null;

        // Convert to Kraken format if needed
        String krakenSymbol = krakenSymbolMap.getOrDefault(symbol, symbol);

        try {
            String urlStr = BASE_URL + "/Depth?pair=" + krakenSymbol + "&count=10";
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
            if (json.has("error") && json.getJSONArray("error").length() > 0) {
                logBuilder.append("Error fetching Kraken order book: ")
                        .append(json.getJSONArray("error").toString()).append("\n");
                return null;
            }

            JSONObject result = json.getJSONObject("result");
            JSONObject bookData = result.getJSONObject(krakenSymbol);

            JSONArray bidsArray = bookData.getJSONArray("bids");
            JSONArray asksArray = bookData.getJSONArray("asks");

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

            orderBook = new OrderBook(symbol, bids, asks, new Date());

        } catch (Exception e) {
            logBuilder.append("Exception in fetchOrderBookREST for symbol ").append(symbol)
                    .append(": ").append(e.getMessage()).append("\n");
            e.printStackTrace();
        }
        return orderBook;
    }

    /**
     * Initializes WebSocket connections for market data streaming from Kraken.
     *
     * @param symbols List of symbols to subscribe to.
     * @return true if successfully connected, false otherwise.
     */
    @Override
    public boolean initializeWebSocket(List<String> symbols) {
        try {
            // Close existing connection if any.
            if (webSocket != null) {
                closeWebSocket();
            }

            webSocket = wsClient.newWebSocketBuilder()
                    .buildAsync(URI.create(WS_BASE_URL), webSocketListener)
                    .join();

            // Subscribe to ticker and book channels for each symbol.
            JSONObject subscribeMsg = new JSONObject();
            subscribeMsg.put("name", "subscribe");

            JSONArray pairs = new JSONArray();
            for (String symbol : symbols) {
                // Convert to Kraken format if needed.
                String krakenSymbol = krakenSymbolMap.getOrDefault(symbol, symbol);
                pairs.put(krakenSymbol);
            }
            subscribeMsg.put("pair", pairs);

            // Subscribe to ticker channel.
            JSONObject subscription = new JSONObject();
            subscription.put("name", "ticker");
            subscribeMsg.put("subscription", subscription);
            webSocket.sendText(subscribeMsg.toString(), true);

            // Subscribe to book channel.
            subscription.put("name", "book");
            subscription.put("depth", 10); // Depth of 10 entries.
            subscribeMsg.put("subscription", subscription);
            webSocket.sendText(subscribeMsg.toString(), true);

            websocketConnected = true;
            logBuilder.append("Kraken WebSocket connected for symbols: ").append(symbols).append("\n");
            return true;
        } catch (Exception e) {
            logBuilder.append("Exception initializing Kraken WebSocket: ").append(e.getMessage()).append("\n");
            e.printStackTrace();
            websocketConnected = false;
            return false;
        }
    }

    /**
     * Closes the WebSocket connection.
     */
    @Override
    public void closeWebSocket() {
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Closing connection");
            websocketConnected = false;
            logBuilder.append("Kraken WebSocket connection closed\n");
        }
    }

    /**
     * WebSocket listener for Kraken data.
     */
    private class KrakenWebSocketListener implements WebSocket.Listener {
        private StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            logBuilder.append("Kraken WebSocket connection opened\n");
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                String message = buffer.toString();
                buffer = new StringBuilder();
                processMessage(message);
            }
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            logBuilder.append("Kraken WebSocket closed: ").append(statusCode)
                    .append(", reason: ").append(reason).append("\n");
            websocketConnected = false;
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            logBuilder.append("Kraken WebSocket error: ").append(error.getMessage()).append("\n");
            error.printStackTrace();
            websocketConnected = false;
            WebSocket.Listener.super.onError(webSocket, error);
        }

        /**
         * Processes the WebSocket message and updates the cache.
         */
        private void processMessage(String message) {
            try {
                // Kraken's WebSocket messages can be arrays or objects.
                if (message.startsWith("[")) {
                    JSONArray data = new JSONArray(message);

                    // Check if this is a data message.
                    if (data.length() >= 3 && !data.isNull(1)) {
                        String channelName = data.optString(1);
                        String pair = data.optString(3); // The pair name.

                        if ("ticker".equals(channelName)) {
                            JSONObject tickerData = data.getJSONObject(1);

                            JSONArray bidData = tickerData.getJSONArray("b");
                            double bidPrice = bidData.getDouble(0);

                            JSONArray askData = tickerData.getJSONArray("a");
                            double askPrice = askData.getDouble(0);

                            JSONArray lastData = tickerData.getJSONArray("c");
                            double lastPrice = lastData.getDouble(0);

                            JSONArray volumeData = tickerData.getJSONArray("v");
                            double volume = volumeData.getDouble(1); // 24h volume.

                            Ticker ticker = new Ticker(bidPrice, askPrice, lastPrice, volume, new Date());
                            tickerCache.put(pair, ticker);
                        }
                        else if ("book".equals(channelName)) {
                            JSONObject bookData = data.getJSONObject(1);

                            // Get current order book or create new one.
                            OrderBook currentBook = orderBookCache.get(pair);
                            List<OrderBookEntry> bids = new ArrayList<>();
                            List<OrderBookEntry> asks = new ArrayList<>();

                            if (currentBook != null) {
                                bids = new ArrayList<>(currentBook.getBids());
                                asks = new ArrayList<>(currentBook.getAsks());
                            }

                            // Process bids if present.
                            if (bookData.has("bs")) {
                                JSONArray newBids = bookData.getJSONArray("bs");
                                for (int i = 0; i < newBids.length(); i++) {
                                    JSONArray entry = newBids.getJSONArray(i);
                                    double price = Double.parseDouble(entry.getString(0));
                                    double volume = Double.parseDouble(entry.getString(1));

                                    boolean found = false;
                                    for (int j = 0; j < bids.size(); j++) {
                                        if (bids.get(j).getPrice() == price) {
                                            if (volume > 0) {
                                                bids.set(j, new OrderBookEntry(price, volume));
                                            } else {
                                                bids.remove(j);
                                            }
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (!found && volume > 0) {
                                        bids.add(new OrderBookEntry(price, volume));
                                    }
                                }
                            }

                            // Process asks if present.
                            if (bookData.has("as")) {
                                JSONArray newAsks = bookData.getJSONArray("as");
                                for (int i = 0; i < newAsks.length(); i++) {
                                    JSONArray entry = newAsks.getJSONArray(i);
                                    double price = Double.parseDouble(entry.getString(0));
                                    double volume = Double.parseDouble(entry.getString(1));

                                    boolean found = false;
                                    for (int j = 0; j < asks.size(); j++) {
                                        if (asks.get(j).getPrice() == price) {
                                            if (volume > 0) {
                                                asks.set(j, new OrderBookEntry(price, volume));
                                            } else {
                                                asks.remove(j);
                                            }
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (!found && volume > 0) {
                                        asks.add(new OrderBookEntry(price, volume));
                                    }
                                }
                            }

                            // Sort bids descending and asks ascending.
                            bids.sort((e1, e2) -> Double.compare(e2.getPrice(), e1.getPrice()));
                            asks.sort((e1, e2) -> Double.compare(e1.getPrice(), e2.getPrice()));

                            OrderBook newBook = new OrderBook(pair, bids, asks, new Date());
                            orderBookCache.put(pair, newBook);
                        }
                    }
                }
                else if (message.startsWith("{")) {
                    JSONObject json = new JSONObject(message);
                    if (json.has("status")) {
                        String status = json.getString("status");
                        if ("subscribed".equals(status)) {
                            logBuilder.append("Kraken subscription confirmed: ").append(json.toString()).append("\n");
                        } else if ("error".equals(status)) {
                            logBuilder.append("Kraken subscription error: ").append(json.toString()).append("\n");
                        }
                    }
                }
            } catch (Exception e) {
                logBuilder.append("Exception in processing Kraken WebSocket message: ")
                        .append(e.getMessage()).append("\n");
                e.printStackTrace();
            }
        }
    }
}