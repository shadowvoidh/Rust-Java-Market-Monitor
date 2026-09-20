**[PT-BR 🇧🇷 ](SECURITY.md)** |   **[ENG 🇺🇸 ]**

# 🔒 Security Policy — RustQuote FX

This document describes the security model of **RustQuote FX**, how to
report vulnerabilities, and recommended best practices for anyone running
or extending the project.

---

## 📦 Supported Versions

The project is distributed as source code (Rust + Java); there is currently
no published official binary/package release. Always use the latest version
of the `main` branch.

| Component      | Reference version | Supported |
| --------------- | ------------------- | :-------: |
| Rust Engine     | Edition 2021         | ✅        |
| Java UI         | JDK 17+              | ✅        |
| Java UI         | JDK < 17             | ❌        |

---

## 🧭 Threat Model

**RustQuote FX** was designed for **local, personal use**, not to be
exposed to the internet. Key points of the current security model:

* **Rust engine bound to `127.0.0.1:8080`** — the REST API only accepts
  connections from the local machine. It should **not** be exposed on
  `0.0.0.0` or behind a public proxy without adding authentication and TLS.
* **No authentication/authorization** — any local process with access to
  port 8080 can consume `/api/quotes` and `/api/health`. This is acceptable
  for personal desktop use but **not suitable** for multi-user or shared
  server environments.
* **CORS fully open (`Any`)** — configured this way because the client is a
  local desktop application, not a web page. If the engine is ever accessed
  from a browser or re-exposed on a network, restrict
  `allow_origin`/`allow_methods` in the `tower_http::cors::CorsLayer`.
* **Outbound calls to third-party APIs** — the engine makes HTTPS requests
  to AwesomeAPI and CoinGecko. No API key is required or stored for the
  public endpoints currently used.
* **No disk persistence** — the cache is in-memory only
  (`Arc<RwLock<...>>`) and is discarded when the process exits; no
  financial or personal data is written to disk.
* **No user data collection** — the application does not collect, store, or
  transmit any personal user data to third parties.

---

## 🚨 Known Risks and Mitigations

| Risk                                                          | Current / recommended mitigation                                                                |
| ---------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------- |
| Accidental exposure of port 8080 on a network                  | Keep the bind on `127.0.0.1`; never change it to `0.0.0.0` without adding authentication and TLS.    |
| External API unavailability (AwesomeAPI/CoinGecko)              | Cache with degraded fallback; the UI shows "Offline" status when the engine doesn't respond.         |
| Outdated dependencies (crates.io / Maven)                       | Run `cargo audit` and keep Gson/JDK updated periodically (see section below).                        |
| Denial-of-service via repeated calls to the endpoint             | The 25s cache reduces flooding effects on external APIs, but there is no built-in rate limiting.     |
| Man-in-the-middle on external calls                              | `reqwest` uses TLS (`rustls-tls`) by default for HTTPS calls to external APIs.                        |

---

## 🛡️ Best Practices for Extending the Project

1. **Do not** expose the Rust engine directly to the internet without
   adding:
   - authentication (e.g., API token, mTLS);
   - TLS/HTTPS on the engine itself (it currently runs over plain HTTP,
     since traffic is local `localhost → localhost`);
   - rate limiting (e.g., `tower::limit`).
2. Run `cargo audit` regularly to check for known vulnerabilities in Rust
   dependencies:
   ```bash
   cargo install cargo-audit
   cargo audit
   ```
3. Keep `gson-2.10.1.jar` updated to the latest stable Gson release, and
   keep the JDK on an actively supported version (17 LTS or higher).
4. If you add new external data sources that require an API key, **never**
   commit the key to the repository — use environment variables or a
   `.env` file included in `.gitignore`.
5. Always validate responses from external APIs before passing them to the
   UI (the current code already handles parsing/timeout failures per
   source in isolation, preventing one unstable API from breaking the
   entire response).

---

## 📣 How to Report a Vulnerability

If you find a security vulnerability in this project, **do not open a
public issue**. Instead:

1. Send an e-mail to **shadow.voidh@gmail.com** with:
   - a description of the vulnerability;
   - steps to reproduce it (if possible);
   - potential impact;
   - a suggested fix (optional).
2. You can expect an acknowledgment within **5 business days**.
3. We kindly ask for a reasonable amount of time to fix the issue before
   any public disclosure (*responsible disclosure*).

Good-faith reports, made without malicious exploitation of third-party data
or disruption of production services, are very welcome and appreciated.

---

## 📬 Contact

* *GitHub:* [@shadowvoidh](https://github.com/shadowvoidh)
* *E-mail:* shadow.voidh@gmail.com
* *LinkedIn:* [Pedro Carnio](https://linkedin.com/in/pedrocarnio)
* *Discord:* shadow_voidh
