package org.example.infrastructure.notification;

import org.example.data.interfaces.ArbitrageResult;
import org.example.data.interfaces.INotificationService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Android-compatible implementation of the notification service.
 * This service implements the INotificationService interface and provides
 * logging functionality compatible with Android's logging system.
 * 
 * The service supports different log levels (INFO, WARNING, ERROR, DEBUG)
 * and can be configured to enable/disable debug logging.
 * 
 * It also provides notification methods for arbitrage opportunities and errors.
 */
public class ConsoleNotificationService implements INotificationService {
    
    private static final String TAG = "TradientArbitrage";
    private static final DateTimeFormatter timeFormatter = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private boolean debugEnabled;
    
    /**
     * Constructor with debug mode option.
     *
     * @param debugEnabled Whether to enable debug logging
     */
    public ConsoleNotificationService(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }
    
    /**
     * Default constructor with debug mode disabled.
     */
    public ConsoleNotificationService() {
        this(false);
    }
    
    @Override
    public void logInfo(String message) {
        logFormattedMessage("INFO", message);
    }
    
    @Override
    public void logWarning(String message) {
        logFormattedMessage("WARNING", message);
    }
    
    @Override
    public void logError(String message, Throwable throwable) {
        logFormattedMessage("ERROR", message);
        // Android logging would handle the throwable differently, but for now we'll keep it silent
    }
    
    @Override
    public void logDebug(String message) {
        if (debugEnabled) {
            logFormattedMessage("DEBUG", message);
        }
    }
    
    @Override
    public void notify(String title, String message, String type) {
        // On Android, this would create a notification
        // For now, we're silencing all output
    }
    
    @Override
    public void notifyArbitrageOpportunity(ArbitrageResult opportunity) {
        if (opportunity != null && opportunity.hasOpportunities()) {
            // On Android, this would create a notification for the arbitrage opportunity
            // For now, we're silencing all output
        }
    }
    
    @Override
    public void notifyArbitrageError(Throwable error) {
        // On Android, this would create an error notification
        // For now, we're silencing all output
    }
    
    /**
     * Helper method to format and log a message.
     *
     * @param level The log level
     * @param message The message to log
     */
    private void logFormattedMessage(String level, String message) {
        // On Android, this would use Android's Log class
        // For now, we're silencing all output
    }
} 