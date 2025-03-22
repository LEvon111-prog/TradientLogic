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
import java.util.concurrent.CompletionStage;

/**
 * BybitV5ExchangeService provides concrete implementations for fetching market data
 * from Bybit V5 APIs for spot markets.
 *
 * Endpoints used:
 * - Trading Pairs: GET https://api.bybit.com/v5/market/instruments-info?category=spot
 * - Ticker Data:  GET https://api.bybit.com/v5/market/tickers?category=spot
 * - Order Book:   GET https://api.bybit.com/v5/market/orderbook?category=spot&symbol={symbol}&limit=5
 * - WebSocket:    wss://stream.bybit.com/v5/public/spot
 *
 * Important messages are now accumulated in a log, retrievable via getLogMessages().
 */
public class BybitV5ExchangeService extends ExchangeService {

    private static final String BASE_URL = "https://api.bybit.com";
    private static final String WS_BASE_URL = "wss://stream.bybit.com/v5/public/spot";

    // WebSocket client and connection.
    private HttpClient wsClient;
    private WebSocket webSocket;
    private BybitWebSocketListener webSocketListener;

    // Accumulates important log messages.
    private StringBuilder logBuilder = new StringBuilder();

    /**
     * Constructs a BybitV5ExchangeService instance.
     *
     * @param fees The trading fee as a percentage (e.g., 0.001 for 0.1%).
     */
    public BybitV5ExchangeService(double fees) {
        super("Bybit", fees);
        this.wsClient = HttpClient.newHttpClient();
        this.webSocketListener = new BybitWebSocketListener();
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
     * Fetches and caches the list of trading pairs (instruments) available on Bybit V5.
     * Endpoint: GET https://api.bybit.com/v5/market/instruments-info?category=spot
     *
     * @return A list of TradingPair objects.
     */
    @Override
    public List<TradingPair> fetchTradingPairs() {
        List<TradingPair> tradingPairs = new ArrayList<>();
        try {
            String urlStr = BASE_URL + "/v5/market/instruments-info?category=spot";
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
            int retCode = json.optInt("retCode", -1);
            if (retCode == 0) {
                JSONObject result = json.getJSONObject("result");
                JSONArray list = result.getJSONArray("list");
                for (int i = 0; i < list.length(); i++) {
                    JSONObject instrument = list.getJSONObject(i);
                    String symbol = instrument.getString("symbol");
                    TradingPair pair = new TradingPair(symbol);
                    tradingPairs.add(pair);
                }
                setTradingPairs(tradingPairs);
            } else {
                logBuilder.append("Error fetching instruments: ").append(json.optString("retMsg")).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tradingPairs;
    }

    /**
     * Retrieves the latest ticker data for the specified symbol using REST API.
     * This is used as a fallback when WebSocket data is not available.
     *
     * Endpoint: GET https://api.bybit.com/v5/market/tickers?category=spot
     *
     * @param symbol The trading pair symbol (e.g., "BTCUSDT").
     * @return A Ticker object containing bid, ask, last price, volume, and timestamp.
     */
    @Override
    protected Ticker fetchTickerDataREST(String symbol) {
        Ticker ticker = null;
        try {
            String urlStr = BASE_URL + "/v5/market/tickers?category=spot";
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
            int retCode = json.optInt("retCode", -1);
            if (retCode == 0) {
                JSONObject result = json.getJSONObject("result");
                JSONArray list = result.getJSONArray("list");
                for (int i = 0; i < list.length(); i++) {
                    JSONObject tickerObj = list.getJSONObject(i);
                    if (tickerObj.getString("symbol").equalsIgnoreCase(symbol)) {
                        double lastPrice = tickerObj.getDouble("lastPrice");
                        double bid = lastPrice;
                        double ask = lastPrice;
                        double volume = tickerObj.getDouble("volume24h");
                        Date timestamp = new Date();
                        ticker = new Ticker(bid, ask, lastPrice, volume, timestamp);
                        break;
                    }
                }
            } else {
                logBuilder.append("Error fetching tickers: ").append(json.optString("retMsg")).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ticker;
    }

    /**
     * Retrieves the current order book for the specified trading pair using REST API.
     * This is used as a fallback when WebSocket data is not available.
     *
     * Endpoint: GET https://api.bybit.com/v5/market/orderbook?category=spot&symbol={symbol}&limit=5
     *
     * @param symbol The trading pair symbol (e.g., "BTCUSDT").
     * @return An OrderBook object with bids and asks.
     */
    @Override
    protected OrderBook fetchOrderBookREST(String symbol) {
        OrderBook orderBook = null;
        try {
            String urlStr = BASE_URL + "/v5/market/orderbook?category=spot&symbol=" + symbol + "&limit=5";
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
            int retCode = json.optInt("retCode", -1);
            if (retCode == 0) {
                JSONObject result = json.getJSONObject("result");
                JSONArray bidsArray = result.getJSONArray("b");
                JSONArray asksArray = result.getJSONArray("a");

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

                Date timestamp = new Date();
                orderBook = new OrderBook(symbol, bids, asks, timestamp);
            } else {
                logBuilder.append("Error fetching order book: ").append(json.optString("retMsg")).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return orderBook;
    }

    /**
     * Initializes WebSocket connections for market data streaming from Bybit.
     *
     * @param symbols List of symbols to subscribe to.
     * @return true if successfully connected, false otherwise.
     */
    @Override
    public boolean initializeWebSocket(List<String> symbols) {
        try {
            if (webSocket != null) {
                closeWebSocket();
            }

            webSocket = wsClient.newWebSocketBuilder()
                    .buildAsync(URI.create(WS_BASE_URL), webSocketListener)
                    .join();

            for (String symbol : symbols) {
                String tickerSubRequest = String.format(
                        "{\"op\":\"subscribe\",\"args\":[\"tickers.%s\"]}",
                        symbol
                );
                webSocket.sendText(tickerSubRequest, true);

                String orderbookSubRequest = String.format(
                        "{\"op\":\"subscribe\",\"args\":[\"orderbook.20.%s\"]}",
                        symbol
                );
                webSocket.sendText(orderbookSubRequest, true);
            }

            websocketConnected = true;
            logBuilder.append("Bybit WebSocket connection and subscriptions initialized.\n");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            websocketConnected = false;
            logBuilder.append("Error initializing Bybit WebSocket: ").append(e.getMessage()).append("\n");
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
            logBuilder.append("Bybit WebSocket connection closed.\n");
        }
    }

    /**
     * WebSocket listener for Bybit data.
     */
    private class BybitWebSocketListener implements WebSocket.Listener {
        private StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            logBuilder.append("Bybit WebSocket connection opened.\n");
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
            logBuilder.append("Bybit WebSocket closed: ").append(statusCode)
                    .append(", reason: ").append(reason).append("\n");
            websocketConnected = false;
            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            logBuilder.append("Bybit WebSocket error: ").append(error.getMessage()).append("\n");
            error.printStackTrace();
            websocketConnected = false;
            WebSocket.Listener.super.onError(webSocket, error);
        }

        /**
         * Process the WebSocket message and update the cache.
         */
        private void processMessage(String message) {
            try {
                JSONObject json = new JSONObject(message);

                // Handle subscription confirmation.
                if (json.has("op") && json.getString("op").equals("subscribe")) {
                    logBuilder.append("Bybit subscription success: ").append(json.toString()).append("\n");
                    return;
                }

                // Handle data messages.
                if (json.has("topic") && json.has("data")) {
                    String topic = json.getString("topic");
                    JSONObject data = json.getJSONObject("data");

                    if (topic.startsWith("tickers.")) {
                        String symbol = topic.substring("tickers.".length());
                        double lastPrice = data.getDouble("lastPrice");
                        double bidPrice = data.optDouble("bid1Price", lastPrice);
                        double askPrice = data.optDouble("ask1Price", lastPrice);
                        double volume = data.getDouble("volume24h");
                        Ticker ticker = new Ticker(bidPrice, askPrice, lastPrice, volume, new Date());
                        tickerCache.put(symbol, ticker);
                    }
                    else if (topic.startsWith("orderbook.")) {
                        String[] parts = topic.split("\\.");
                        String symbol = parts[2];
                        JSONArray bidsArray = data.optJSONArray("b");
                        JSONArray asksArray = data.optJSONArray("a");

                        if (bidsArray != null && asksArray != null) {
                            List<OrderBookEntry> bids = new ArrayList<>();
                            for (int i = 0; i < bidsArray.length(); i++) {
                                JSONArray entry = bidsArray.getJSONArray(i);
                                double price = entry.getDouble(0);
                                double volume = entry.getDouble(1);
                                bids.add(new OrderBookEntry(price, volume));
                            }

                            List<OrderBookEntry> asks = new ArrayList<>();
                            for (int i = 0; i < asksArray.length(); i++) {
                                JSONArray entry = asksArray.getJSONArray(i);
                                double price = entry.getDouble(0);
                                double volume = entry.getDouble(1);
                                asks.add(new OrderBookEntry(price, volume));
                            }

                            OrderBook orderBook = new OrderBook(symbol, bids, asks, new Date());
                            orderBookCache.put(symbol, orderBook);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}