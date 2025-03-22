package org.example.data.model;

import java.util.Date;

public class Ticker {
    private double bidPrice;
    private double askPrice;
    private double lastPrice;
    private double volume;
    private Date timestamp;

    public Ticker(double bidPrice, double askPrice, double lastPrice, double volume, Date timestamp) {
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
        this.lastPrice = lastPrice;
        this.volume = volume;
        this.timestamp = timestamp;
    }

    public double getBidPrice() {
        return bidPrice;
    }

    public double getAskPrice() {
        return askPrice;
    }

    public double getLastPrice() {
        return lastPrice;
    }

    public double getVolume() {
        return volume;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}
