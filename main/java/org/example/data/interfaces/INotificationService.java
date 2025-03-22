package org.example.data.interfaces;

/**
 * Interface for notification services following the Interface Segregation Principle.
 * This provides a clean interface for logging and notifications without
 * implementing direct console output in business logic.
 */
public interface INotificationService {
    /**
     * Log an informational message.
     *
     * @param message The message to log
     */
    void logInfo(String message);

    /**
     * Log a warning message.
     *
     * @param message The message to log
     */
    void logWarning(String message);

    /**
     * Log an error message with an associated exception.
     *
     * @param message The error message
     * @param throwable The exception that caused the error
     */
    void logError(String message, Throwable throwable);

    /**
     * Log a debug message.
     *
     * @param message The message to log
     */
    void logDebug(String message);

    /**
     * Display a notification to the user.
     *
     * @param title The notification title
     * @param message The notification message
     * @param type The notification type (e.g., "info", "warning", "error")
     */
    void notify(String title, String message, String type);

    /**
     * Notify about an arbitrage opportunity.
     *
     * @param opportunity The arbitrage opportunity to notify about
     */
    void notifyArbitrageOpportunity(ArbitrageResult opportunity);

    /**
     * Notify about an error in arbitrage processing.
     *
     * @param error The error that occurred
     */
    void notifyArbitrageError(Throwable error);
} 