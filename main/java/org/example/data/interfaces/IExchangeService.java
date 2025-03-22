package org.example.data.interfaces;

import org.example.data.model.OrderBook;
import org.example.data.model.Ticker;
import org.example.data.model.TradingPair;
import org.example.data.fee.Fee;
import org.example.data.fee.FeeTracker;

import java.util.List;

/**
 * Interface for exchange services following Interface Segregation Principle.
 * This interface defines the contract for all exchange service implementations.
 */
public interface IExchangeService {
    /**
     * Gets the name of the exchange.
     *
     * @return The exchange name
     */
    String getExchangeName();
    
    /**
     * Get all available trading pairs from the exchange.
     *
     * @return List of trading pairs
     */
    List<TradingPair> getTradingPairs();
    
    /**
     * Fetch all trading pairs from the exchange API.
     *
     * @return List of trading pairs
     */
    List<TradingPair> fetchTradingPairs();
    
    /**
     * Get ticker data for a specific trading pair.
     *
     * @param symbol The trading pair symbol
     * @return Ticker data or null if not available
     */
    Ticker getTickerData(String symbol);
    
    /**
     * Get order book for a specific trading pair.
     *
     * @param symbol The trading pair symbol
     * @return Order book or null if not available
     */
    OrderBook getOrderBook(String symbol);
    
    /**
     * Initialize WebSocket connection for real-time data.
     *
     * @param symbols List of symbols to subscribe to
     * @return true if successful, false otherwise
     */
    boolean initializeWebSocket(List<String> symbols);
    
    /**
     * Close WebSocket connection.
     */
    void closeWebSocket();
    
    /**
     * Get the maker fee for this exchange.
     *
     * @return The maker fee
     */
    Fee getMakerFee();
    
    /**
     * Get the taker fee for this exchange.
     *
     * @return The taker fee
     */
    Fee getTakerFee();
    
    /**
     * Update fee tiers based on trading volume.
     *
     * @param thirtyDayVolume The 30-day trading volume
     */
    void updateFeesTiers(double thirtyDayVolume);
    
    /**
     * Calculate and track a fee for a transaction.
     *
     * @param tradingPair The trading pair
     * @param amount The transaction amount
     * @param isMaker Whether this is a maker order
     * @return The calculated fee amount
     */
    double calculateAndTrackFee(String tradingPair, double amount, boolean isMaker);
    
    /**
     * Get the fee tracker for this exchange.
     *
     * @return The fee tracker
     */
    FeeTracker getFeeTracker();
    
    /**
     * Check if WebSocket is connected.
     *
     * @return true if connected, false otherwise
     */
    boolean isWebSocketConnected();
} 