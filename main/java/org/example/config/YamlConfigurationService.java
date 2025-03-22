package org.example.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * Configuration service implementation that loads configuration from YAML files.
 * Supports hierarchical configuration, environment-specific overrides, and runtime reloading.
 */
public class YamlConfigurationService implements ConfigurationService {
    
    private static final String CONFIG_DIR = "config";
    private static final String DEFAULT_ENV = "default";
    private static final String[] CONFIG_FILES = {
            "application.yaml", 
            "exchanges.yaml", 
            "risk-parameters.yaml", 
            "trading-limits.yaml"
    };
    
    private static YamlConfigurationService INSTANCE;
    
    private final ObjectMapper yamlMapper;
    private final Map<String, JsonNode> configurationMap;
    private final ReadWriteLock lock;
    private String environment;
    
    /**
     * Private constructor for singleton pattern
     */
    private YamlConfigurationService() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.configurationMap = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
        
        // Determine environment from system property or environment variable
        this.environment = System.getProperty("app.environment");
        if (this.environment == null) {
            this.environment = System.getenv("APP_ENVIRONMENT");
        }
        if (this.environment == null) {
            this.environment = DEFAULT_ENV;
        }
        
        loadConfiguration();
    }
    
    /**
     * Get the singleton instance
     * 
     * @return The configuration service instance
     */
    public static synchronized YamlConfigurationService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new YamlConfigurationService();
        }
        return INSTANCE;
    }
    
    /**
     * Load configuration from YAML files
     */
    private void loadConfiguration() {
        lock.writeLock().lock();
        try {
            configurationMap.clear();
            
            // Load default configuration first
            loadConfigurationFromDirectory(DEFAULT_ENV);
            
            // Then load environment-specific configuration (which will override defaults)
            if (!DEFAULT_ENV.equals(environment)) {
                loadConfigurationFromDirectory(environment);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Load configuration files from a specific directory
     * 
     * @param directory The directory name under the config directory
     */
    private void loadConfigurationFromDirectory(String directory) {
        Path configDir = Paths.get(CONFIG_DIR, directory);
        if (!Files.exists(configDir)) {
            System.err.println("Configuration directory not found: " + configDir);
            return;
        }
        
        for (String fileName : CONFIG_FILES) {
            Path configFile = configDir.resolve(fileName);
            if (Files.exists(configFile)) {
                try {
                    JsonNode rootNode = yamlMapper.readTree(configFile.toFile());
                    mergeConfiguration(rootNode);
                } catch (IOException e) {
                    System.err.println("Error loading configuration file: " + configFile + ": " + e.getMessage());
                }
            }
        }
        
        // Also load any additional YAML files in the directory
        try (Stream<Path> paths = Files.list(configDir)) {
            paths.filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                 .filter(p -> {
                     String name = p.getFileName().toString();
                     for (String configFile : CONFIG_FILES) {
                         if (name.equals(configFile)) {
                             return false; // Skip files we already processed
                         }
                     }
                     return true;
                 })
                 .forEach(p -> {
                     try {
                         JsonNode rootNode = yamlMapper.readTree(p.toFile());
                         mergeConfiguration(rootNode);
                     } catch (IOException e) {
                         System.err.println("Error loading additional configuration file: " + p + ": " + e.getMessage());
                     }
                 });
        } catch (IOException e) {
            System.err.println("Error listing configuration directory: " + configDir + ": " + e.getMessage());
        }
    }
    
    /**
     * Merge a configuration node into the main configuration map
     * 
     * @param rootNode The root node of the configuration
     */
    private void mergeConfiguration(JsonNode rootNode) {
        rootNode.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            configurationMap.put(key, value);
        });
    }
    
    @Override
    public Optional<String> getString(String key) {
        JsonNode node = getNode(key);
        if (node != null && node.isTextual()) {
            return Optional.of(node.asText());
        }
        return Optional.empty();
    }
    
    @Override
    public String getString(String key, String defaultValue) {
        return getString(key).orElse(defaultValue);
    }
    
    @Override
    public Optional<Integer> getInteger(String key) {
        JsonNode node = getNode(key);
        if (node != null && node.isInt()) {
            return Optional.of(node.asInt());
        }
        return Optional.empty();
    }
    
    @Override
    public int getInteger(String key, int defaultValue) {
        return getInteger(key).orElse(defaultValue);
    }
    
    @Override
    public Optional<Double> getDouble(String key) {
        JsonNode node = getNode(key);
        if (node != null && node.isNumber()) {
            return Optional.of(node.asDouble());
        }
        return Optional.empty();
    }
    
    @Override
    public double getDouble(String key, double defaultValue) {
        return getDouble(key).orElse(defaultValue);
    }
    
    @Override
    public Optional<Boolean> getBoolean(String key) {
        JsonNode node = getNode(key);
        if (node != null && node.isBoolean()) {
            return Optional.of(node.asBoolean());
        }
        return Optional.empty();
    }
    
    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return getBoolean(key).orElse(defaultValue);
    }
    
    @Override
    public <T> Optional<T> getObject(String key, Class<T> clazz) {
        JsonNode node = getNode(key);
        if (node == null) {
            return Optional.empty();
        }
        
        try {
            return Optional.of(yamlMapper.treeToValue(node, clazz));
        } catch (Exception e) {
            System.err.println("Error converting configuration to object: " + key + ": " + e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public <T> T getObject(String key, Class<T> clazz, T defaultValue) {
        return getObject(key, clazz).orElse(defaultValue);
    }
    
    @Override
    public boolean reload() {
        try {
            loadConfiguration();
            return true;
        } catch (Exception e) {
            System.err.println("Error reloading configuration: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get a configuration node by key
     * Supports dot notation for nested properties (e.g., "exchanges.binance.fee")
     * 
     * @param key The configuration key
     * @return The JSON node for the key, or null if not found
     */
    private JsonNode getNode(String key) {
        lock.readLock().lock();
        try {
            if (key == null || key.isEmpty()) {
                return null;
            }
            
            String[] parts = key.split("\\.");
            if (parts.length == 1) {
                return configurationMap.get(key);
            }
            
            // Handle nested properties with dot notation
            JsonNode current = configurationMap.get(parts[0]);
            for (int i = 1; i < parts.length && current != null; i++) {
                current = current.get(parts[i]);
            }
            return current;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Set the environment for configuration
     * 
     * @param environment The environment name
     * @return True if the environment was changed and configuration reloaded
     */
    public boolean setEnvironment(String environment) {
        if (environment == null || environment.isEmpty() || environment.equals(this.environment)) {
            return false;
        }
        
        this.environment = environment;
        return reload();
    }
} 