# CLAUDE.md — AI Assistant Guide for sanction-kg-russia-ukraine

## Project Overview

**Sanction-KG Risk System** (面向经济制裁合规风险的知识图谱构建与风险评估验证系统) is a knowledge graph project focused on modeling, collecting, and analyzing sanctions data related to the Russia-Ukraine conflict. It serves as a **thesis verification prototype system** for the following core algorithms:

- **GSR-ER** (Graph-guided Self-Refine for Entity/Relation/Event Extraction): sanctions-domain text-to-graph extraction, fusion, and versioning
- **HGT-RAM** (Hierarchical Graph Temporal Risk Assessment Model): static (weighted PPR + community risk) and dynamic (Hawkes) fused risk assessment
- **GraphRAG**: graph-retrieval-augmented generation for compliance Q&A and traceable reports

The system addresses three key business problems:
1. Rapid sanctions list updates and diff comparison
2. Compliance risk propagation identification (indirect risk via entity relationships)
3. Evidence-based retrieval and report generation with full traceability

## Repository Status

This repository is in the **design and prototyping phase**. The SRS, backend API design, and frontend prototype have been completed. Backend and data pipeline implementation is next.

## Current Repository Structure

```
sanction-kg-russia-ukraine/
├── CLAUDE.md                       # This file — AI assistant guide
├── docs/
│   ├── SRS.md                      # Software Requirements Specification (Chinese)
│   └── backend-api-design.md       # Backend API design document (Chinese)
└── frontend/
    └── index.html                  # Frontend prototype (single-page HTML with embedded CSS/JS)
```

## Target Architecture

```
sanction-kg-russia-ukraine/
├── CLAUDE.md
├── README.md
├── docs/
│   ├── SRS.md                      # Software Requirements Specification
│   └── backend-api-design.md       # Backend API design
├── frontend/
│   └── index.html                  # Frontend prototype
├── backend/                        # Java Spring Boot backend (planned)
│   └── sanction-kg-backend/
│       ├── src/main/java/com/sanction/kg/
│       │   ├── controller/         # REST API controllers
│       │   ├── service/            # Business logic
│       │   ├── repository/         # Data access (MySQL + Neo4j)
│       │   ├── entity/             # JPA & Neo4j entity models
│       │   ├── dto/                # Request/Response DTOs
│       │   └── common/             # Shared utilities, enums, exception handling
│       └── src/main/resources/
│           └── application.yml     # Spring Boot config
├── data/
│   ├── raw/                        # Raw sanctions lists and source data
│   ├── processed/                  # Cleaned and normalized data
│   └── kg/                         # Knowledge graph exports
├── src/                            # Python data pipeline scripts
│   ├── ingestion/                  # Data collection and parsing
│   ├── normalization/              # Entity resolution and cleaning
│   ├── graph/                      # KG construction and queries
│   └── analysis/                   # Analytics and reporting
├── notebooks/                      # Jupyter notebooks
├── tests/                          # Unit and integration tests
├── config/                         # Configuration files
└── scripts/                        # Utility and automation scripts
```

## Technology Stack

### Backend (API Server)
- **Language:** Java 11+
- **Framework:** Spring Boot 2.7+
- **ORM:** Spring Data JPA (MySQL) + Spring Data Neo4j
- **Databases:** MySQL 8.0+ (relational data) + Neo4j 4.4+ (graph data)
- **Cache:** Redis (optional)
- **Task scheduling:** Spring @Async / Quartz
- **API docs:** Swagger/OpenAPI 3.0
- **Build tool:** Maven

### Frontend (Prototype)
- **Type:** Single-page HTML application
- **Styling:** Custom CSS with CSS variables (dark theme)
- **Fonts:** IBM Plex Mono, Libre Baskerville, DM Sans (Google Fonts)
- **Graph visualization:** Planned — D3.js or Cytoscape.js
- **Pages:** Smart Query (GraphRAG Q&A), Risk Dashboard, Entity Search, Data Management

### Data Pipeline
- **Language:** Python 3.10+
- **Data formats:** JSON, CSV, RDF/Turtle
- **Graph tooling:** NetworkX, rdflib, Neo4j GDS
- **Data processing:** pandas, requests
- **LLM integration:** Anthropic Claude API (for GSR-ER extraction)

## Key API Endpoints

| Endpoint | Method | Description | Priority |
|----------|--------|-------------|----------|
| `/api/qa/ask` | POST | GraphRAG evidence-based Q&A | P0 |
| `/api/entities/search` | GET | Entity search (name/alias/ID) | P0 |
| `/api/entities/{id}` | GET | Entity detail | P0 |
| `/api/risk/calculate` | POST | Trigger risk calculation | P0 |
| `/api/risk/{entityId}` | GET | Get risk assessment result | P0 |
| `/api/risk/paths/{entityId}` | GET | Get risk propagation paths | P0 |
| `/api/graph/subgraph` | GET | Get k-hop subgraph | P1 |
| `/api/evidence/{id}` | GET | Get evidence detail | P1 |
| `/api/ingestion/snapshots` | GET/POST | Snapshot management | P1 |
| `/api/extraction/tasks` | POST | Create extraction task | P1 |
| `/api/system/stats` | GET | System statistics | P1 |

### API Response Format
```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": "2026-02-06T14:23:45Z"
}
```

## Core Data Models

### MySQL Tables
- **snapshots** — Data import snapshot records (source, hash, record count)
- **evidences** — Evidence store (text excerpts, confidence, extractor version)
- **risk_results** — Risk assessment results (PPR/community/Hawkes scores, fusion params)
- **risk_paths** — Risk propagation paths (ranked paths with contribution weights)
- **extraction_tasks** — GSR-ER extraction task tracking

### Neo4j Graph Schema
- **Entity nodes** — entity_id, entity_type, name, aliases, identifiers, status, version
- **RELATED_TO relationships** — predicate, weight, first_seen_at, last_seen_at, evidence_ids
- Constraints: entity_id unique; indexes on name, entity_type, status

## Core Domain Concepts

- **Sanctioned Entity:** Individual, organization, vessel, or aircraft subject to sanctions
- **Sanctions Program:** Regulatory framework (OFAC, EU, UK OFSI, UN)
- **SDN:** Specially Designated Nationals (US OFAC)
- **Evidence:** Traceable record (source, text span, timestamp, confidence, extractor version)
- **Versioning:** Append-only change records (ADD/UPDATE/DEPRECATE) for entities/relations/events
- **KG Triple:** (Subject, Predicate, Object) — fundamental unit of the knowledge graph
- **PPR:** Personalized PageRank — used for static risk propagation scoring
- **Hawkes Process:** Point process model for dynamic/temporal risk scoring
- **GraphRAG:** Graph-retrieval augmented generation — evidence-based Q&A with citations

## Development Conventions

### Code Style — Java (Backend)
- Follow standard Java conventions
- Use Lombok for boilerplate reduction
- Package structure: `com.sanction.kg.{controller,service,repository,entity,dto,common}`
- Use `@RestController` + `@RequestMapping` for API layers
- Async tasks via `@Async` annotation

### Code Style — Python (Data Pipeline)
- Follow PEP 8
- Use type hints for function signatures
- Docstrings: Google style
- Maximum line length: 120 characters
- Use `snake_case` for functions/variables, `PascalCase` for classes

### Git Workflow
- Branch naming: `claude/<description>-<session-id>` for AI-assisted work
- Commit messages: imperative mood, concise subject line (< 72 chars)
- Keep commits atomic — one logical change per commit
- Never commit secrets, API keys, or credentials
- Add large data files to `.gitignore` (or use Git LFS)

### Data Handling
- Raw source data should never be modified in place — always write to `data/processed/`
- Document data provenance: source URL, download date, license info
- Entity identifiers: use canonical IDs from official sanctions lists where available
- Date formats: ISO 8601 (`YYYY-MM-DD`)
- Every data import must generate a snapshot record with file hash (SHA-256)
- All extracted facts must link to at least one evidence_id

### Testing
- Tests go in the `tests/` directory, mirroring `src/` structure
- Python: Use `pytest` as the test runner; name test files `test_<module>.py`
- Java: Use JUnit 5 + Spring Boot Test; name test files `*Test.java`

## Data Sources

- **US OFAC SDN List** — Structured sanctions list
- **EU Consolidated Sanctions List** — Structured sanctions list
- **UK OFSI Consolidated List** — Structured sanctions list
- **UN Security Council Sanctions** — Structured sanctions list
- **GDELT** — Global news event data (text corpus)
- **Reuters / news agencies** — News text corpus
- **OpenSanctions** — Aggregated dataset

## Commands Reference

```bash
# --- Backend (Java/Spring Boot) ---
# Start MySQL (Docker)
docker run -d --name mysql -e MYSQL_ROOT_PASSWORD=root123 -e MYSQL_DATABASE=sanction_kg -p 3306:3306 mysql:8.0

# Start Neo4j (Docker)
docker run -d --name neo4j -e NEO4J_AUTH=neo4j/neo4j123 -p 7474:7474 -p 7687:7687 neo4j:4.4

# Run Spring Boot app
./mvnw spring-boot:run

# --- Data Pipeline (Python) ---
pip install -r requirements.txt
pytest tests/

# --- Code Quality ---
ruff check src/
ruff format src/
```

## Important Notes for AI Assistants

1. **Thesis context:** This is a thesis verification prototype (Chapter 5). Focus on demonstrating algorithm correctness and traceability, not production-grade features.
2. **Data sensitivity:** Sanctions data is publicly available but handle entity information carefully and accurately. Errors in entity matching can have real consequences.
3. **No fabrication:** Never invent or hallucinate sanctions data. Only use verified sources.
4. **Provenance tracking:** Always track where data came from and when it was retrieved. Every fact must have evidence.
5. **Bilingual codebase:** Documentation is primarily in Chinese; code and API interfaces use English identifiers.
6. **Idempotent pipelines:** Data ingestion and processing scripts should be safe to re-run.
7. **Incremental development:** This project is being built incrementally. Check existing files before creating new ones.
8. **Dual database architecture:** MySQL for relational/tabular data (snapshots, evidence, risk results); Neo4j for graph data (entities, relations). Keep them in sync.
9. **Evidence-first design:** Every risk conclusion, extraction result, and generated answer must reference traceable evidence (evidence_ids).
10. **Keep CLAUDE.md updated:** As the project evolves, update this file to reflect the current state of the codebase.
