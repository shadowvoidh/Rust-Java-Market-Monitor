//! ============================================================================
//! Rust Engine - Dashboard de Cotações Financeiras
//! ----------------------------------------------------------------------------
//! Este serviço local (daemon) sobe um servidor HTTP em 127.0.0.1:8080 e expõe:
//!
//!   GET /api/quotes  -> JSON consolidado com USD, EUR e BTC (com cache)
//!   GET /api/health  -> healthcheck simples, usado pelo cliente Java para
//!                       exibir o indicador Online/Offline
//!
//! Fontes de dados:
//!   - AwesomeAPI (https://economia.awesomeapi.com.br) para USD-BRL e EUR-BRL
//!   - CoinGecko  (https://api.coingecko.com)          para BTC-BRL
//!
//! O cache em memória evita bater nas APIs externas a cada requisição do
//! cliente Java (que faz polling a cada 30s). Enquanto o cache estiver
//! "fresco" (< CACHE_TTL_SECONDS), a resposta é servida sem nova chamada
//! externa. Se a busca externa falhar, o serviço tenta devolver o último
//! valor válido em cache (modo degradado) em vez de quebrar a resposta.
//! ============================================================================

use axum::{extract::State, routing::get, Json, Router};
use chrono::Local;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::Arc;
use std::time::{Duration, Instant};
use tokio::sync::RwLock;
use tower_http::cors::{Any, CorsLayer};

/// Tempo de vida do cache. Definido um pouco abaixo do intervalo de polling
/// da UI Java (30s) para que a UI quase sempre receba dados frescos, mas sem
/// gerar uma chamada externa a cada requisição recebida.
const CACHE_TTL_SECONDS: u64 = 25;

// =============================================================================
// Modelos de saída (o que o cliente Java consome)
// =============================================================================

#[derive(Debug, Clone, Serialize)]
struct Quote {
    symbol: String,
    name: String,
    price: f64,
    change_percent: f64,
    last_update: String,
}

#[derive(Debug, Clone, Serialize)]
struct QuotesResponse {
    quotes: Vec<Quote>,
    generated_at: String,
    cached: bool,
}

// =============================================================================
// Modelos de entrada (formato bruto retornado pelas APIs externas)
// =============================================================================

/// Formato de cada item retornado por
/// https://economia.awesomeapi.com.br/json/last/USD-BRL,EUR-BRL
#[derive(Debug, Deserialize)]
struct AwesomeApiQuote {
    #[allow(dead_code)]
    code: String,
    name: String,
    bid: String,
    #[serde(rename = "pctChange")]
    pct_change: String,
}

/// Formato de resposta da CoinGecko para
/// /api/v3/simple/price?ids=bitcoin&vs_currencies=brl&include_24hr_change=true
#[derive(Debug, Deserialize)]
struct CoinGeckoResponse {
    bitcoin: CoinGeckoBitcoin,
}

#[derive(Debug, Deserialize)]
struct CoinGeckoBitcoin {
    brl: f64,
    #[serde(rename = "brl_24h_change")]
    brl_24h_change: f64,
}

// =============================================================================
// Estado compartilhado / Cache
// =============================================================================

struct CacheEntry {
    data: QuotesResponse,
    fetched_at: Instant,
}

type Cache = Arc<RwLock<Option<CacheEntry>>>;

#[derive(Clone)]
struct AppState {
    http_client: reqwest::Client,
    cache: Cache,
}

// =============================================================================
// Busca de dados externos
// =============================================================================

async fn fetch_awesome_api(
    client: &reqwest::Client,
) -> Result<HashMap<String, AwesomeApiQuote>, reqwest::Error> {
    let url = "https://economia.awesomeapi.com.br/json/last/USD-BRL,EUR-BRL";
    let resp = client.get(url).send().await?;
    let parsed = resp.json::<HashMap<String, AwesomeApiQuote>>().await?;
    Ok(parsed)
}

async fn fetch_coingecko(client: &reqwest::Client) -> Result<CoinGeckoResponse, reqwest::Error> {
    let url = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=brl&include_24hr_change=true";
    let resp = client.get(url).send().await?;
    let parsed = resp.json::<CoinGeckoResponse>().await?;
    Ok(parsed)
}

/// Busca USD, EUR e BTC em paralelo (join) e consolida no formato de saída.
async fn fetch_all_quotes(client: &reqwest::Client) -> Result<QuotesResponse, String> {
    let (fx_result, crypto_result) =
        tokio::join!(fetch_awesome_api(client), fetch_coingecko(client));

    let mut quotes: Vec<Quote> = Vec::new();
    let now = Local::now();
    let now_str = now.format("%H:%M:%S").to_string();

    match fx_result {
        Ok(fx_map) => {
            if let Some(usd) = fx_map.get("USDBRL") {
                quotes.push(build_fx_quote("USD", usd, &now_str));
            }
            if let Some(eur) = fx_map.get("EURBRL") {
                quotes.push(build_fx_quote("EUR", eur, &now_str));
            }
        }
        Err(e) => {
            eprintln!("[WARN] Falha ao buscar câmbio (AwesomeAPI): {e}");
        }
    }

    match crypto_result {
        Ok(coingecko) => {
            quotes.push(Quote {
                symbol: "BTC".to_string(),
                name: "Bitcoin".to_string(),
                price: coingecko.bitcoin.brl,
                change_percent: coingecko.bitcoin.brl_24h_change,
                last_update: now_str.clone(),
            });
        }
        Err(e) => {
            eprintln!("[WARN] Falha ao buscar cripto (CoinGecko): {e}");
        }
    }

    if quotes.is_empty() {
        return Err("Não foi possível obter nenhuma cotação das APIs externas".to_string());
    }

    Ok(QuotesResponse {
        quotes,
        generated_at: now.format("%Y-%m-%d %H:%M:%S").to_string(),
        cached: false,
    })
}

fn build_fx_quote(symbol: &str, raw: &AwesomeApiQuote, now_str: &str) -> Quote {
    Quote {
        symbol: symbol.to_string(),
        name: raw.name.clone(),
        price: raw.bid.parse::<f64>().unwrap_or(0.0),
        change_percent: raw.pct_change.parse::<f64>().unwrap_or(0.0),
        last_update: now_str.to_string(),
    }
}

// =============================================================================
// Handlers HTTP
// =============================================================================

async fn get_quotes(State(state): State<AppState>) -> Json<QuotesResponse> {
    {
        let cache_guard = state.cache.read().await;
        if let Some(entry) = cache_guard.as_ref() {
            if entry.fetched_at.elapsed() < Duration::from_secs(CACHE_TTL_SECONDS) {
                let mut response = entry.data.clone();
                response.cached = true;
                return Json(response);
            }
        }
    }

    match fetch_all_quotes(&state.http_client).await {
        Ok(fresh_data) => {
            let mut cache_guard = state.cache.write().await;
            *cache_guard = Some(CacheEntry {
                data: fresh_data.clone(),
                fetched_at: Instant::now(),
            });
            Json(fresh_data)
        }
        Err(err) => {
            eprintln!("[ERROR] {err}");
            let cache_guard = state.cache.read().await;
            if let Some(entry) = cache_guard.as_ref() {
                let mut stale = entry.data.clone();
                stale.cached = true;
                return Json(stale);
            }
            Json(QuotesResponse {
                quotes: vec![],
                generated_at: Local::now().format("%Y-%m-%d %H:%M:%S").to_string(),
                cached: false,
            })
        }
    }
}

async fn health() -> &'static str {
    "OK"
}

// =============================================================================
// main
// =============================================================================

#[tokio::main]
async fn main() {
    let http_client = reqwest::Client::builder()
        .timeout(Duration::from_secs(8))
        .user_agent("rust-engine-cotacoes/0.1")
        .build()
        .expect("Falha ao construir o cliente HTTP");

    let state = AppState {
        http_client,
        cache: Arc::new(RwLock::new(None)),
    };

    // CORS liberado (o cliente é local/desktop, então não há origem web a
    let cors = CorsLayer::new()
        .allow_origin(Any)
        .allow_methods(Any)
        .allow_headers(Any);

    let app = Router::new()
        .route("/api/quotes", get(get_quotes))
        .route("/api/health", get(health))
        .layer(cors)
        .with_state(state);

    let listener = tokio::net::TcpListener::bind("127.0.0.1:8080")
        .await
        .expect("Não foi possível abrir a porta 8080. Ela já está em uso?");

    println!(" Rust Engine no ar em http://127.0.0.1:8080");
    println!("   -> GET /api/quotes  (cotações consolidadas, cache de {CACHE_TTL_SECONDS}s)");
    println!("   -> GET /api/health  (healthcheck)");

    axum::serve(listener, app)
        .await
        .expect("Erro ao rodar o servidor Axum");
}
