package org.example.config;

import org.example.data.model.ArbitrageConfiguration;
import org.example.data.model.ExchangeConfiguration;
import org.example.data.model.RiskConfiguration;

/**
 * Factory for accessing configuration objects.
 * Provides convenient access to typed configuration objects from the configuration service.
 */
public class ConfigurationFactory {
    
    private static final ConfigurationService configService = YamlConfigurationService.getInstance();
    
    // Constants for configuration paths
    private static final String ARBITRAGE_CONFIG_PATH = "arbitrage";
    private static final String EXCHANGES_CONFIG_PATH = "exchanges";
    private static final String RISK_CONFIG_PATH = "risk";
    private static final String TRADING_CONFIG_PATH = "trading";
    private static final String SYSTEM_CONFIG_PATH = "system";
    
    /**
     * Private constructor to prevent instantiation
     */
    private ConfigurationFactory() {
        // Static factory, no instantiation needed
    }
    
    /**
     * Get the arbitrage configuration
     * 
     * @return The arbitrage configuration
     */
    public static ArbitrageConfiguration getArbitrageConfig() {
        return configService.getObject(ARBITRAGE_CONFIG_PATH, ArbitrageConfiguration.class, new ArbitrageConfiguration());
    }
    
    /**
     * Get the exchange configuration
     * 
     * @return The exchange configuration
     */
    public static ExchangeConfiguration getExchangeConfig() {
        return configService.getObject(EXCHANGES_CONFIG_PATH, ExchangeConfiguration.class, new ExchangeConfiguration());
    }
    
    /**
     * Get the risk configuration
     * 
     * @return The risk configuration
     */
    public static RiskConfiguration getRiskConfig() {
        return configService.getObject(RISK_CONFIG_PATH, RiskConfiguration.class, new RiskConfiguration());
    }
    
    /**
     * Get a double value from the configuration
     * 
     * @param key The configuration key
     * @param defaultValue The default value if the key is not found
     * @return The configuration value or the default
     */
    public static double getDouble(String key, double defaultValue) {
        return configService.getDouble(key, defaultValue);
    }
    
    /**
     * Get an integer value from the configuration
     * 
     * @param key The configuration key
     * @param defaultValue The default value if the key is not found
     * @return The configuration value or the default
     */
    public static int getInteger(String key, int defaultValue) {
        return configService.getInteger(key, defaultValue);
    }
    
    /**
     * Get a boolean value from the configuration
     * 
     * @param key The configuration key
     * @param defaultValue The default value if the key is not found
     * @return The configuration value or the default
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        return configService.getBoolean(key, defaultValue);
    }
    
    /**
     * Get a string value from the configuration
     * 
     * @param key The configuration key
     * @param defaultValue The default value if the key is not found
     * @return The configuration value or the default
     */
    public static String getString(String key, String defaultValue) {
        return configService.getString(key, defaultValue);
    }
    
    /**
     * Reload the configuration
     * 
     * @return true if the configuration was reloaded successfully
     */
    public static boolean reloadConfiguration() {
        return configService.reload();
    }
} 