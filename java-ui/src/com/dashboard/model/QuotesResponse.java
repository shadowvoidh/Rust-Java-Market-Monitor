package com.dashboard.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Envelope da resposta de GET /api/quotes, espelhando a struct
 * QuotesResponse do backend Rust.
 */
public class QuotesResponse {

    private List<Quote> quotes;

    @SerializedName("generated_at")
    private String generatedAt;

    private boolean cached;

    public QuotesResponse() {
        // Construtor vazio exigido pelo Gson
    }

    public List<Quote> getQuotes() {
        return quotes;
    }

    public void setQuotes(List<Quote> quotes) {
        this.quotes = quotes;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }
}