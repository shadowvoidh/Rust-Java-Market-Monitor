package com.dashboard.service;

import com.dashboard.model.QuotesResponse;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente HTTP responsável por conversar com a engine Rust local,
 * usando exclusivamente o java.net.http.HttpClient (JDK 11+, sem
 * dependências externas para a camada de transporte).
 */
public class RustEngineClient {

    private static final String BASE_URL = "http://localhost:8080";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(2);

    private final HttpClient httpClient;
    private final Gson gson;

    public RustEngineClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.gson = new Gson();
    }

    /**
     * Busca as cotações consolidadas em GET /api/quotes.
     *
     * @throws IOException          se houver falha de rede ou status HTTP inesperado
     * @throws InterruptedException se a thread for interrompida durante a chamada
     */
    public QuotesResponse fetchQuotes() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/quotes"))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Status HTTP inesperado da engine Rust: " + response.statusCode());
        }

        return gson.fromJson(response.body(), QuotesResponse.class);
    }

    /**
     * Verifica se a engine Rust está no ar, chamando GET /api/health.
     * Nunca lança exceção: qualquer falha (conexão recusada, timeout, etc.)
     * é interpretada como "offline".
     */
    public boolean isEngineOnline() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/health"))
                    .timeout(HEALTH_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
