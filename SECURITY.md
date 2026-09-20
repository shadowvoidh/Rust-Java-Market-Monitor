**[PT-BR 🇧🇷 ]** |   **[[ENG 🇺🇸 ]](SECURITY.en.md)**

# 🔒 Política de Segurança — RustQuote FX

Este documento descreve o modelo de segurança do **RustQuote FX**, como reportar
vulnerabilidades e boas práticas recomendadas para quem for rodar ou estender
o projeto.

---

## 📦 Versões Suportadas

O projeto é distribuído como código-fonte (Rust + Java), não há versões
publicadas em pacote/binário oficial no momento. Recomenda-se sempre usar a
versão mais recente da branch `main`.

| Componente     | Versão de referência | Suportado |
| -------------- | --------------------- | :-------: |
| Rust Engine    | Edition 2021           | ✅        |
| Java UI        | JDK 17+                | ✅        |
| Java UI        | JDK < 17               | ❌        |

---

## 🧭 Modelo de Ameaça

O **RustQuote FX** foi projetado para uso **local e pessoal**, não para ser
exposto na internet. Pontos importantes do modelo de segurança atual:

* **Engine Rust vinculada a `127.0.0.1:8080`** — a API REST só aceita
  conexões da própria máquina. Ela **não** deve ser exposta em `0.0.0.0` ou
  atrás de um proxy público sem adicionar autenticação e TLS.
* **Sem autenticação/autorização** — qualquer processo local com acesso à
  porta 8080 pode consumir `/api/quotes` e `/api/health`. Isso é aceitável
  para uso desktop pessoal, mas **não é adequado** para ambientes
  multiusuário ou servidores compartilhados.
* **CORS liberado (`Any`)** — configurado assim porque o cliente é uma
  aplicação desktop local, não uma página web. Se a engine vier a ser
  acessada por um navegador ou reexposta em rede, restrinja
  `allow_origin`/`allow_methods` no `tower_http::cors::CorsLayer`.
* **Chamadas de saída para APIs de terceiros** — a engine faz requisições
  HTTPS para AwesomeAPI e CoinGecko. Nenhuma chave de API é necessária nem
  armazenada para os endpoints públicos usados atualmente.
* **Sem persistência em disco** — o cache é apenas em memória
  (`Arc<RwLock<...>>`) e é descartado ao encerrar o processo; nenhum dado
  financeiro ou pessoal é gravado em arquivo.
* **Sem coleta de dados do usuário** — a aplicação não coleta, armazena ou
  transmite dados pessoais do usuário para terceiros.

---

## 🚨 Riscos Conhecidos e Mitigações

| Risco                                                            | Mitigação atual / recomendada                                                                 |
| ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| Exposição acidental da porta 8080 em rede                       | Manter bind em `127.0.0.1`; nunca alterar para `0.0.0.0` sem adicionar autenticação e TLS.       |
| Indisponibilidade das APIs externas (AwesomeAPI/CoinGecko)       | Cache com fallback degradado; a UI exibe status "Offline" quando a engine não responde.          |
| Dependências desatualizadas (crates.io / Maven)                 | Execute `cargo audit` e mantenha o Gson/JDK atualizados periodicamente (veja seção abaixo).       |
| Ataques de negação de serviço via chamadas repetidas ao endpoint | O cache de 25s reduz efeito de flooding sobre as APIs externas, mas não há rate limiting interno. |
| Man-in-the-middle nas chamadas externas                          | `reqwest` usa TLS (`rustls-tls`) por padrão nas chamadas HTTPS às APIs externas.                  |

---

## 🛡️ Boas Práticas para Quem for Estender o Projeto

1. **Não** exponha a engine Rust diretamente na internet sem adicionar:
   - autenticação (ex: token de API, mTLS);
   - TLS/HTTPS na própria engine (hoje ela roda em HTTP simples, pois é
     tráfego local `localhost → localhost`);
   - rate limiting (ex: `tower::limit`).
2. Rode `cargo audit` regularmente para checar vulnerabilidades conhecidas
   nas dependências Rust:
   ```bash
   cargo install cargo-audit
   cargo audit
   ```
3. Mantenha o `gson-2.10.1.jar` atualizado para a versão estável mais
   recente do Gson, e o JDK em uma versão com suporte ativo (17 LTS ou
   superior).
4. Se for adicionar novas fontes de dados externas que exijam chave de API,
   **nunca** faça commit da chave no repositório — use variáveis de ambiente
   ou um arquivo `.env` incluído no `.gitignore`.
5. Valide sempre a resposta de APIs externas antes de repassá-la à UI (o
   código atual já trata falhas de parsing/timeout de forma isolada por
   fonte, evitando que uma API instável derrube a resposta inteira).

---

## 📣 Como Reportar uma Vulnerabilidade

Se você encontrar uma vulnerabilidade de segurança neste projeto, **não abra
uma issue pública**. Em vez disso:

1. Envie um e-mail para **shadow.voidh@gmail.com** com:
   - descrição da vulnerabilidade;
   - passos para reproduzir (se possível);
   - impacto potencial;
   - sugestão de correção (opcional).
2. Você pode esperar uma confirmação de recebimento em até **5 dias úteis**.
3. Pedimos, por gentileza, um prazo razoável para correção antes de qualquer
   divulgação pública (*responsible disclosure*).

Relatos feitos de boa-fé, sem exploração maliciosa dos dados de terceiros
nem interrupção de serviços de produção, são muito bem-vindos e
apreciados.

---

## 📬 Contato

* *GitHub:* [@shadowvoidh](https://github.com/shadowvoidh)
* *E-mail:* shadow.voidh@gmail.com
* *LinkedIn:* [Pedro Carnio](https://linkedin.com/in/pedrocarnio)
* *Discord:* shadow_voidh
