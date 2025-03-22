package org.example.data.model;

import java.util.Date;
import java.util.List;

/**
 * Represents the order book for a given trading pair.
 * <p>
 * This class stores the list of bid and ask orders along with a timestamp.
 * Additional helper methods provide quick access to key values such as:
 * - The best bid and best ask orders.
 * - The volume available at the best bid or ask.
 * - The spread between the best ask and best bid prices.
 */
public class OrderBook {

    // The trading pair symbol (e.g., "BTC/USD")
    private String symbol;

    // Lists of order book entries for bids and asks.
    private List<OrderBookEntry> bids;
    private List<OrderBookEntry> asks;

    // The timestamp when the order book was last updated.
    private Date timestamp;

    /**
     * Constructor to initialize the OrderBook.
     *
     * @param symbol    The trading pair symbol.
     * @param bids      The list of bid entries (typically sorted descending by price).
     * @param asks      The list of ask entries (typically sorted ascending by price).
     * @param timestamp The timestamp of the order book snapshot.
     */
    public OrderBook(String symbol, List<OrderBookEntry> bids, List<OrderBookEntry> asks, Date timestamp) {
        this.symbol = symbol;
        this.bids = bids;
        this.asks = asks;
        this.timestamp = timestamp;
    }

    /**
     * Returns the trading pair symbol associated with this order book.
     *
     * @return The symbol (e.g., "BTC/USD").
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Returns the list of bid entries.
     *
     * @return A list of OrderBookEntry objects for bids.
     */
    public List<OrderBookEntry> getBids() {
        return bids;
    }

    /**
     * Returns the list of ask entries.
     *
     * @return A list of OrderBookEntry objects for asks.
     */
    public List<OrderBookEntry> getAsks() {
        return asks;
    }

    /**
     * Returns the timestamp of this order book snapshot.
     *
     * @return The timestamp.
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Retrieves the best bid entry.
     * <p>
     * Assumes that the bids list is sorted in descending order by price.
     *
     * @return The highest bid entry, or null if there are no bids.
     */
    public OrderBookEntry getBestBid() {
        return (bids != null && !bids.isEmpty()) ? bids.get(0) : null;
    }

    /**
     * Retrieves the best ask entry.
     * <p>
     * Assumes that the asks list is sorted in ascending order by price.
     *
     * @return The lowest ask entry, or null if there are no asks.
     */
    public OrderBookEntry getBestAsk() {
        return (asks != null && !asks.isEmpty()) ? asks.get(0) : null;
    }

    /**
     * Retrieves the volume available at the best bid price.
     *
     * @return The volume of the best bid, or 0 if no bids are available.
     */
    public double getBestBidVolume() {
        OrderBookEntry bestBid = getBestBid();
        return bestBid != null ? bestBid.getVolume() : 0;
    }

    /**
     * Retrieves the volume available at the best ask price.
     *
     * @return The volume of the best ask, or 0 if no asks are available.
     */
    public double getBestAskVolume() {
        OrderBookEntry bestAsk = getBestAsk();
        return bestAsk != null ? bestAsk.getVolume() : 0;
    }

    /**
     * Calculates the spread between the best ask and the best bid prices.
     *
     * @return The spread (best ask price minus best bid price), or 0 if either side is missing.
     */
    public double getSpread() {
        OrderBookEntry bestBid = getBestBid();
        OrderBookEntry bestAsk = getBestAsk();
        if (bestBid != null && bestAsk != null) {
            return bestAsk.getPrice() - bestBid.getPrice();
        }
        return 0;
    }


}
