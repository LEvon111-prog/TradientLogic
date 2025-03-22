package org.example.data.model;

/**
 * Represents a single entry in an order book.
 * <p>
 * Each entry contains:
 * - A price level at which orders exist.
 * - The cumulative volume available at that price level.
 */
public class OrderBookEntry {

    // The price level for the order entry.
    private double price;

    // The available volume at the given price level.
    private double volume;

    /**
     * Constructs an OrderBookEntry with the specified price and volume.
     *
     * @param price  The price level of the order.
     * @param volume The available volume at this price level.
     */
    public OrderBookEntry(double price, double volume) {
        this.price = price;
        this.volume = volume;
    }

    /**
     * Returns the price level for this order entry.
     *
     * @return The price level.
     */
    public double getPrice() {
        return price;
    }

    /**
     * Returns the volume available at this price level.
     *
     * @return The volume.
     */
    public double getVolume() {
        return volume;
    }

    /**
     * Optionally, override toString() for easier logging and debugging.
     */
    @Override
    public String toString() {
        return "OrderBookEntry{" +
                "price=" + price +
                ", volume=" + volume +
                '}';
    }
}
