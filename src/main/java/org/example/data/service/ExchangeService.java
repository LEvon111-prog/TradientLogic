package org.example.data.service;

import org.example.data.model.OrderBook;
import org.example.data.model.Ticker;
import org.example.data.model.TradingPair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ExchangeService {

    // Unique exchange identifier
    private String exchangeName;

    //------------------->Dont forget add here logo property <---------------------

    // Property to cache trading pairs
    private List<TradingPair> tradingPairs;

    // Cache for real-time ticker data
    protected ConcurrentHashMap<String, Ticker> tickerCache;
    
    // Cache for real-time order book data
    protected ConcurrentHashMap<String, OrderBook> orderBookCache;
    
    // Flag to track if WebSocket connection is active
    protected volatile boolean websocketConnected = false;

    private double fees;

    // Constructor to initialize the exchange name and the trading pairs cache
    public ExchangeService(String exchangeName, double fees) {
        this.exchangeName = exchangeName;
        this.tradingPairs = new ArrayList<>();
        this.tickerCache = new ConcurrentHashMap<>();
        this.orderBookCache = new ConcurrentHashMap<>();
        this.fees = fees;
    }

    /**
     * Returns the cached list of trading pairs.
     */
    public List<TradingPair> getTradingPairs() {
        return tradingPairs;
    }

    /**
     * Updates the trading pairs cache with a new list.
     */
    protected void setTradingPairs(List<TradingPair> pairs) {
        this.tradingPairs = pairs;
    }

    /**
     * Fetches the list of trading pairs from the exchange API,
     * updates the internal cache, and returns the updated list.
     */
    public abstract List<TradingPair> fetchTradingPairs();

    /**
     * Retrieves the latest ticker data for the specified symbol.
     * Will try to use cached WebSocket data if available, otherwise falls back to REST.
     */
    public Ticker getTickerData(String symbol) {
        // Try to get from WebSocket cache first
        Ticker cachedTicker = tickerCache.get(symbol);
        if (cachedTicker != null) {
            return cachedTicker;
        }
        
        // Fall back to REST API if WebSocket data is not available
        return fetchTickerDataREST(symbol);
    }

    /**
     * Retrieves the latest ticker data using REST API (fallback method)
     */
    protected abstract Ticker fetchTickerDataREST(String symbol);

    /**
     * Retrieves the current order book for the specified trading pair.
     * Will try to use cached WebSocket data if available, otherwise falls back to REST.
     */
    public OrderBook getOrderBook(String symbol) {
        // Try to get from WebSocket cache first
        OrderBook cachedOrderBook = orderBookCache.get(symbol);
        if (cachedOrderBook != null) {
            return cachedOrderBook;
        }
        
        // Fall back to REST API if WebSocket data is not available
        return fetchOrderBookREST(symbol);
    }

    /**
     * Retrieves the current order book using REST API (fallback method)
     */
    protected abstract OrderBook fetchOrderBookREST(String symbol);

    /**
     * Initializes WebSocket connections for market data streaming
     * 
     * @param symbols List of symbols to subscribe to
     * @return true if successfully connected, false otherwise
     */
    public abstract boolean initializeWebSocket(List<String> symbols);

    /**
     * Closes the WebSocket connections
     */
    public abstract void closeWebSocket();

    /**
     * Returns the exchange's unique name.
     */
    public String getExchangeName() {
        return exchangeName;
    }

    public double getFees() {
        return fees;
    }
    
    /**
     * Checks if the WebSocket connection is active
     */
    public boolean isWebSocketConnected() {
        return websocketConnected;
    }

    /**
     * Gets the trading fee for this exchange.
     * 
     * @return The trading fee as a decimal (e.g., 0.001 for 0.1%)
     */
    public double getTradingFees() {
        return fees;
    }
}
