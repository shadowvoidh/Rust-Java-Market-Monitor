**[PT-BR 🇧🇷 ]([README.md](README.md))** |   **[ENG ]**

# 💹 RustQuote FX — Real-Time Market Dashboard

**RustQuote FX** is a high-performance hybrid desktop application built for real-time monitoring of major foreign currencies (**USD**, **EUR**) and cryptocurrency (**BTC**) converted to BRL.

The project uses a **local client-server hybrid architecture**: a **Rust** engine responsible for fetching, concurrency, and caching the data, and a modern **Java Swing** GUI for non-blocking rendering on screen.

---

<div align="center">

  <h2>💻 Cross-Platform Desktop Compatibility</h2>

 <br>

  <h3>Windows 🪟</h3>
  <img src="./assets/win-screenshot.png" alt="Windows Preview" width="85%">

  <br><br>

  <h3>MacOS 🍎 / Linux 🐧</h3>
  <h3>MacOS</h3>
  <img src="./assets/mac-screenshot.png" alt="macOS Preview" width="85%">
  <br>
  <h3>Void Linux</h3>
   <img src="./assets/linux-screenshot.png" alt="Linux Preview" width="85%">

</div>

---

## 🏗️ System Architecture
```
┌─────────────────────────┐         GET /api/quotes         ┌─────────────────────────┐
│     Java Swing UI       │ ──────────────────────────────► │    Rust Engine (Axum)   │
│   (RustEngineClient)    │ ◄────────────────────────────── │   25s Cache + Tokio     │
│ Thread-Safe (SwingWorker)│       Consolidated JSON         │ (AwesomeAPI / CoinGecko)│
└─────────────────────────┘                                 └─────────────────────────┘

```

---

## 🔑 Key Features

* **Hybrid Architecture (Rust + Java):** Clear separation between the high-performance logic (local backend) and the user interface (desktop frontend).
* **Parallel Processing (Rust):** Simultaneous fetching from AwesomeAPI and CoinGecko using `tokio::join!`.
* **Smart Caching System (Rust):** 25-second in-memory cache with degraded fallback (if the external API fails, the engine returns the last valid quote).
* **Asynchronous Interface (Java):** UI updates executed via `SwingWorker`, keeping the interface 100% responsive.
* **Real-Time Connection Monitor:** Checked every 5s, showing the engine status (`Engine Online` / `Engine Offline`).
* **Configurable Auto-Refresh:** Automatic quote updates every 30 seconds, with a countdown and a manual refresh button.

---

## 🛠️ Technologies Used

### **Backend (Rust Engine)**
* **Rust** (2021 Edition)
* **Axum** — Asynchronous web framework for the local REST API.
* **Tokio** — Asynchronous runtime for parallel requests.
* **Serde / Serde JSON** — Ultra-fast JSON serialization and deserialization.
* **Reqwest** — HTTP client for consuming external APIs.

### **Frontend (Java UI)**
* **Java 17+**
* **Java Swing** — Custom dark-themed GUI.
* **Java HTTP Client (`java.net.http`)** — Native communication with the Rust backend.
* **Gson (Google)** — Parsing of the JSON payload returned by the engine.

---

## 🗂️ Repository Structure
```
projeto-cotacoes/
├── README.md
├── README.en.md
├── SECURITY.md
├── SECURITY.en.md
├── .gitignore
├── rust-engine/
│   ├── Cargo.toml
│   └── src/
│       └── main.rs
└── java-ui/
├── lib/
│   ├── LEIA-ME.txt          # Instructions to download Gson
│   └── gson-2.10.1.jar
└── src/
└── com/
└── dashboard/
├── Main.java
├── model/
│   ├── Quote.java
│   └── QuotesResponse.java
├── service/
│   └── RustEngineClient.java
└── view/
└── DashboardFrame.java
```

---

## 🚀 How to Run the Project

### **1. Prerequisites**
* **Rust** and **Cargo** installed ([rustup.rs](https://rustup.rs/))
* **JDK 17 or higher** installed
* `gson-2.10.1.jar` file placed in the `java-ui/lib/` folder

---

### **2. Running the Rust Engine**

Open a terminal in the project's root folder:

```bash
cd rust-engine

cargo run

```

On the first run, Cargo will download and compile the dependencies. When it finishes, you should see:
```
🚀 Rust Engine running at http://127.0.0.1:8080
```

Testing the REST API (optional):

```Bash
curl http://localhost:8080/api/quotes

```

3. Running the Java UI
Open another terminal in the project folder:

```Windows (PowerShell):
PowerShell
cd java-ui

# Compile
javac -cp "lib\gson-2.10.1.jar" -d out (Get-ChildItem -Recurse -Filter *.java -Path src | ForEach-Object { $_.FullName })

# Run
java -cp "out;lib\gson-2.10.1.jar" com.dashboard.Main

```
Linux / macOS (Terminal):
```Bash
cd java-ui

# Compile
find src -name "*.java" > sources.txt
javac -cp "lib/gson-2.10.1.jar" -d out @sources.txt

# Run
java -cp "out:lib/gson-2.10.1.jar" com.dashboard.Main

```
📌 Recommended Execution Order
Start the Rust backend first (cargo run).

Then start the Java application (com.dashboard.Main).

The status indicator at the top of the window will show Engine Online in green. If the engine is stopped, the indicator will automatically switch to Engine Offline in red.


---

## 🌘 Author
* *Shadow_Voidh* - (https://github.com/shadowvoidh)

## 📬 Contact
* *GitHub:* [@shadowvoidh](https://github.com/shadowvoidh)
* *Instagram:* [@shadow_voidh](https://www.instagram.com/shadow_voidh/)
* *LinkedIn:* [Pedro Carnio](https://linkedin.com/in/pedrocarnio)
* *Discord:* shadow_voidh
* *E-mail:* shadow.voidh@gmail.com
