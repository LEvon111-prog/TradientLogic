package org.example.config;

import java.util.Optional;

/**
 * Core service for accessing application configuration.
 * Provides methods to retrieve various types of configuration values
 * with support for default values and optional returns.
 */
public interface ConfigurationService {
    
    /**
     * Get a string configuration value
     * 
     * @param key The configuration key
     * @return Optional containing the value if present
     */
    Optional<String> getString(String key);
    
    /**
     * Get a string with a default value
     * 
     * @param key The configuration key
     * @param defaultValue The default value if key not found
     * @return The configuration value or the default
     */
    String getString(String key, String defaultValue);
    
    /**
     * Get an integer configuration value
     * 
     * @param key The configuration key
     * @return Optional containing the value if present
     */
    Optional<Integer> getInteger(String key);
    
    /**
     * Get an integer with a default value
     * 
     * @param key The configuration key
     * @param defaultValue The default value if key not found
     * @return The configuration value or the default
     */
    int getInteger(String key, int defaultValue);
    
    /**
     * Get a double configuration value
     * 
     * @param key The configuration key
     * @return Optional containing the value if present
     */
    Optional<Double> getDouble(String key);
    
    /**
     * Get a double with a default value
     * 
     * @param key The configuration key
     * @param defaultValue The default value if key not found
     * @return The configuration value or the default
     */
    double getDouble(String key, double defaultValue);
    
    /**
     * Get a boolean configuration value
     * 
     * @param key The configuration key
     * @return Optional containing the value if present
     */
    Optional<Boolean> getBoolean(String key);
    
    /**
     * Get a boolean with a default value
     * 
     * @param key The configuration key
     * @param defaultValue The default value if key not found
     * @return The configuration value or the default
     */
    boolean getBoolean(String key, boolean defaultValue);
    
    /**
     * Get a typed configuration object
     * 
     * @param key The configuration key
     * @param clazz The class of the configuration object
     * @param <T> The type of the configuration object
     * @return Optional containing the configuration object if present
     */
    <T> Optional<T> getObject(String key, Class<T> clazz);
    
    /**
     * Get a typed configuration object with a default
     * 
     * @param key The configuration key
     * @param clazz The class of the configuration object
     * @param defaultValue The default value if key not found
     * @param <T> The type of the configuration object
     * @return The configuration object or the default
     */
    <T> T getObject(String key, Class<T> clazz, T defaultValue);
    
    /**
     * Reload configuration from source
     * 
     * @return true if reloaded successfully
     */
    boolean reload();
} 