# 🤖 Agent IA Contrats d'Assurance

Agent conversationnel intelligent pour la gestion de contrats d'assurance. Au lieu de naviguer dans des écrans classiques, l'utilisateur pose ses questions en langage naturel et l'agent récupère les données pertinentes.

## 🏗️ Architecture

```
┌──────────────────┐     HTTP POST     ┌──────────────────────────────────────┐
│                  │   /api/agent      │            Spring Boot 3             │
│   Angular 22     │ ──────────────►   │                                     │
│                  │                   │  AgentController                     │
│  • ChatComponent │                   │       │                              │
│  • Provider      │   ◄────────────   │       ▼                              │
│    Selector      │    JSON response  │  LlmProviderRegistry                │
│                  │                   │       │                              │
└──────────────────┘                   │       │
                                       │       ├── ClaudeProvider (Anthropic) │
                                       │       ├── OpenAiProvider (GPT-4o)    │
                                       │       └── GroqProvider (Llama 70B)   │
                                       │                                     │
                                       │  Base H2 (dev) → PostgreSQL (prod)  │
                                       └──────────────────────────────────────┘
```

## ✨ Fonctionnalités

- **Provider Pattern** — switcher entre Claude, OpenAI et Groq depuis l'interface Angular sans redémarrage
- **Chat en temps réel** — interface conversationnelle avec bulles user/agent
- **Multi-LLM** — même code métier, providers interchangeables
- **Sélecteur de provider** — dropdown intégré dans la zone de saisie

## 🛠️ Stack technique

| Couche | Technologie |
|--------|------------|
| Frontend | Angular 22, Signals, Angular Material |
| Backend | Java 25, Spring Boot 3, Spring Web, JPA |
| LLM | Claude API, OpenAI API, Groq API |
| Base de données | H2 (dev) → PostgreSQL (prod) |
| Build | Maven (backend), Angular CLI (frontend) |

## 📋 Prérequis

- **Java 25** (JDK) — [Télécharger](https://www.oracle.com/java/technologies/downloads/)
- **Node.js 20+** — [Télécharger](https://nodejs.org/)
- **Maven 3.9+** — [Télécharger](https://maven.apache.org/)
- **Angular CLI 22** — `npm install -g @angular/cli`

## 🚀 Lancement

### Backend (Spring Boot)

```bash
cd backend/

# Configurer les clés API dans application.properties
# llm.groq.api-key=gsk_xxxxx
# llm.claude.api-key=sk-ant-xxxxx
# llm.openai.api-key=sk-xxxxx

mvn clean compile spring-boot:run
```

Le serveur démarre sur **http://localhost:8090**

### Frontend (Angular)

```bash
cd frontend/

npm install
ng serve --proxy-config proxy.conf.json
```

L'application est accessible sur **http://localhost:4200**

## 📁 Structure du projet

```
├── backend/
│   └── src/main/java/com/example/Stage/
│       ├── Controller/
│       │   └── AgentController.java         # POST /api/agent + GET /api/providers
│       ├── Config/
│       │   └── SecurityConfig.java          # CORS + désactivation CSRF (dev)
│       ├── Llm/
│       │   ├── LlmService.java              # Interface — ask() et stream()
│       │   ├── LlmProviderRegistry.java     # Registre des providers
│       │   ├── Model/
│       │   │   ├── LlmMessage.java          # Record (role, content)
│       │   │   └── LlmConfig.java           # Record (model, temperature, maxTokens)
│       │   └── Provider/
│       │       ├── ClaudeProvider.java       # Anthropic API
│       │       ├── OpenAiProvider.java       # OpenAI API
│       │       └── GroqProvider.java         # Groq API (Llama 70B)
│       ├── Model/
│       ├── Repository/
│       ├── Service/
│       └── StageApplication.java
│
├── frontend/
│   └── src/app/
│       ├── agent/
│       │   ├── chat.component.ts            # Logique chat + Signals
│       │   ├── chat.component.html          # Template avec provider selector
│       │   ├── chat.component.scss          # Styles
│       │   ├── chat.service.ts              # HttpClient → /api/agent
│       │   └── chat.model.ts               # Interface Message
│       ├── contrats/
│       ├── auth/
│       ├── shared/
│       ├── app.component.ts
│       └── app.config.ts
│
└── README.md
```

## 🔌 API Endpoints

### `POST /api/agent`

Envoie un message à l'agent.

**Request :**
```json
{
  "message": "contrats en vigueur du client Dupont",
  "provider": "groq"
}
```

**Response :**
```json
{
  "response": "Voici les contrats en vigueur de Dupont Marie...",
  "provider": "groq"
}
```

### `GET /api/providers`

Liste les providers disponibles.

**Response :**
```json
[ "claude", "openai", "groq"]
```

## ⚙️ Configuration des providers

Dans `application.properties` :

```properties
# Clés API — mettre "none" si pas disponible
llm.claude.api-key=none
llm.openai.api-key=none
llm.groq.api-key=gsk_xxxxx

# Serveur
server.port=8090
spring.threads.virtual.enabled=true
```

Le provider est sélectionné **côté Angular** à chaque message. Pas besoin de redémarrer pour switcher.

## 📅 Sprints

| Sprint | Contenu | Semaines |
|--------|---------|----------|
| Sprint 1 | Socle technique — Provider Pattern, Hello LLM, Chat Angular | S1-S2 |
| Sprint 2 | Consultation — recherche contrats/clients/tiers via function calling | S3-S4 |
| Sprint 3 | Saisie guidée — souscription et avenants via conversation | S5-S6 |
| Sprint 4 | Sécurité JWT, qualité, documentation, démo | S7-S8 |

## 🧪 Test rapide

```bash
# Vérifier que Spring Boot répond
curl -X POST http://localhost:8090/api/agent \
  -H "Content-Type: application/json" \
  -d '{"message": "bonjour", "provider":}'

# Lister les providers
curl http://localhost:8090/api/providers
```

## 👤 Auteur

Stage — Agent IA Contrats d'Assurance
Java 25 · Angular 22 · Spring Boot 3 · Provider Pattern LLM
