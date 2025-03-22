package org.example.data.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration model for exchange-specific parameters.
 * Contains settings for APIs, fees, rate limits, and exchange-specific behaviors.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangeConfiguration {
    
    /**
     * Base fee rates for each exchange (default is maker rate)
     */
    private Map<String, Double> baseFees = new HashMap<>();
    
    /**
     * API base URLs for each exchange
     */
    private Map<String, String> apiUrls = new HashMap<>();
    
    /**
     * WebSocket URLs for each exchange
     */
    private Map<String, String> wsUrls = new HashMap<>();
    
    /**
     * API rate limits for each exchange (requests per minute)
     */
    private Map<String, Integer> rateLimits = new HashMap<>();
    
    /**
     * Connection timeouts for each exchange (in milliseconds)
     */
    private Map<String, Integer> connectionTimeouts = new HashMap<>();
    
    /**
     * API key IDs (stored in external secure storage)
     */
    private Map<String, String> apiKeyReferences = new HashMap<>();
    
    /**
     * Enable specific exchange integrations
     */
    private Map<String, Boolean> enabled = new HashMap<>();
    
    /**
     * Constructor
     */
    public ExchangeConfiguration() {
        // Initialize with default values
        baseFees.put("binance", 0.001);  // 0.1%
        baseFees.put("coinbase", 0.006); // 0.6% 
        baseFees.put("kraken", 0.0026);  // 0.26%
        baseFees.put("bybit", 0.001);    // 0.1%
        
        apiUrls.put("binance", "https://api.binance.com");
        apiUrls.put("coinbase", "https://api.coinbase.com");
        apiUrls.put("kraken", "https://api.kraken.com");
        apiUrls.put("bybit", "https://api.bybit.com");
        
        wsUrls.put("binance", "wss://stream.binance.com:9443/ws");
        wsUrls.put("coinbase", "wss://ws-feed.exchange.coinbase.com");
        wsUrls.put("kraken", "wss://ws.kraken.com");
        wsUrls.put("bybit", "wss://stream.bybit.com/realtime");
        
        rateLimits.put("binance", 1200);
        rateLimits.put("coinbase", 300);
        rateLimits.put("kraken", 60);
        rateLimits.put("bybit", 600);
        
        connectionTimeouts.put("binance", 30000);
        connectionTimeouts.put("coinbase", 30000);
        connectionTimeouts.put("kraken", 30000);
        connectionTimeouts.put("bybit", 30000);
        
        enabled.put("binance", true);
        enabled.put("coinbase", true);
        enabled.put("kraken", true);
        enabled.put("bybit", true);
    }

    public Map<String, Double> getBaseFees() {
        return baseFees;
    }

    public void setBaseFees(Map<String, Double> baseFees) {
        this.baseFees = baseFees;
    }

    public Map<String, String> getApiUrls() {
        return apiUrls;
    }

    public void setApiUrls(Map<String, String> apiUrls) {
        this.apiUrls = apiUrls;
    }

    public Map<String, String> getWsUrls() {
        return wsUrls;
    }

    public void setWsUrls(Map<String, String> wsUrls) {
        this.wsUrls = wsUrls;
    }

    public Map<String, Integer> getRateLimits() {
        return rateLimits;
    }

    public void setRateLimits(Map<String, Integer> rateLimits) {
        this.rateLimits = rateLimits;
    }

    public Map<String, Integer> getConnectionTimeouts() {
        return connectionTimeouts;
    }

    public void setConnectionTimeouts(Map<String, Integer> connectionTimeouts) {
        this.connectionTimeouts = connectionTimeouts;
    }

    public Map<String, String> getApiKeyReferences() {
        return apiKeyReferences;
    }

    public void setApiKeyReferences(Map<String, String> apiKeyReferences) {
        this.apiKeyReferences = apiKeyReferences;
    }

    public Map<String, Boolean> getEnabled() {
        return enabled;
    }

    public void setEnabled(Map<String, Boolean> enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Get the base fee for a specific exchange
     * 
     * @param exchangeName The exchange name
     * @return The base fee or default if not configured
     */
    public double getExchangeFee(String exchangeName) {
        return baseFees.getOrDefault(exchangeName.toLowerCase(), 0.001);
    }
    
    /**
     * Get the API URL for a specific exchange
     * 
     * @param exchangeName The exchange name
     * @return The API URL or null if not configured
     */
    public String getApiUrl(String exchangeName) {
        return apiUrls.get(exchangeName.toLowerCase());
    }
    
    /**
     * Get the WebSocket URL for a specific exchange
     * 
     * @param exchangeName The exchange name
     * @return The WebSocket URL or null if not configured
     */
    public String getWsUrl(String exchangeName) {
        return wsUrls.get(exchangeName.toLowerCase());
    }
    
    /**
     * Check if an exchange is enabled
     * 
     * @param exchangeName The exchange name
     * @return true if the exchange is enabled
     */
    public boolean isExchangeEnabled(String exchangeName) {
        return enabled.getOrDefault(exchangeName.toLowerCase(), false);
    }
} 