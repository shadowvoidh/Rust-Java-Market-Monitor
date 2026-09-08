package com.dashboard.model;

import com.google.gson.annotations.SerializedName;

/**
 * Representa uma única cotação (USD, EUR ou BTC) retornada pela engine Rust.
 */
public class Quote {

    private String symbol;
    private String name;
    private double price;

    @SerializedName("change_percent")
    private double changePercent;

    @SerializedName("last_update")
    private String lastUpdate;

    public Quote() {
        // Construtor vazio exigido pelo Gson
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getChangePercent() {
        return changePercent;
    }

    public void setChangePercent(double changePercent) {
        this.changePercent = changePercent;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public boolean isPositive() {
        return changePercent >= 0;
    }

    @Override
    public String toString() {
        return "Quote{" +
                "symbol='" + symbol + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", changePercent=" + changePercent +
                ", lastUpdate='" + lastUpdate + '\'' +
                '}';
    }
}