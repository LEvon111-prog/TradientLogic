package org.example.data.model;

public class TradingPair {
    private String baseCurrency;
    private String quoteCurrency;
    private String symbol;

    public TradingPair(String baseCurrency, String quoteCurrency) {
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.symbol = baseCurrency + quoteCurrency;
    }
    public TradingPair(String symbol) {
        this.symbol = symbol;
    }


    public String getBaseCurrency() {
        return baseCurrency;
    }

    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    public String getSymbol() {
        return symbol;
    }
}
