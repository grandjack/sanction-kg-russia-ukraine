# CLAUDE.md — AI Assistant Guide for sanction-kg-russia-ukraine

## Project Overview

**sanction-kg-russia-ukraine** is a knowledge graph project focused on modeling, collecting, and analyzing sanctions data related to the Russia-Ukraine conflict. The goal is to build a structured, queryable dataset of sanctioned entities (individuals, organizations, vessels, etc.), their relationships, and the regulatory frameworks under which they are sanctioned.

## Repository Status

This repository is in its **initial setup phase**. The foundational structure and tooling are being established.

## Intended Architecture

```
sanction-kg-russia-ukraine/
├── CLAUDE.md                # This file — AI assistant guide
├── README.md                # Project documentation and setup instructions
├── data/
│   ├── raw/                 # Raw sanctions lists and source data (CSV, JSON, XML)
│   ├── processed/           # Cleaned and normalized data
│   └── kg/                  # Knowledge graph exports (RDF, JSON-LD, or similar)
├── src/                     # Source code
│   ├── ingestion/           # Data collection and parsing scripts
│   ├── normalization/       # Entity resolution and data cleaning
│   ├── graph/               # Knowledge graph construction and queries
│   └── analysis/            # Analytics and reporting
├── notebooks/               # Jupyter notebooks for exploration and analysis
├── tests/                   # Unit and integration tests
├── config/                  # Configuration files
├── scripts/                 # Utility and automation scripts
└── docs/                    # Extended documentation
```

## Development Conventions

### Language and Stack

- **Primary language:** Python 3.10+
- **Data formats:** JSON, CSV, RDF/Turtle for knowledge graph triples
- **Graph tooling:** NetworkX, rdflib, or Neo4j (to be determined)
- **Data processing:** pandas, requests

### Code Style

- Follow PEP 8 for Python code
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
- Document data provenance: include source URL, download date, and license info
- Entity identifiers: use canonical IDs from official sanctions lists where available
- Date formats: ISO 8601 (`YYYY-MM-DD`)

### Testing

- Tests go in the `tests/` directory, mirroring `src/` structure
- Use `pytest` as the test runner
- Name test files `test_<module>.py`
- Run tests: `pytest tests/`

## Key Domain Concepts

- **Sanctioned Entity:** An individual, organization, vessel, or aircraft subject to sanctions
- **Sanctions Program:** The regulatory framework (e.g., EU, US OFAC, UK OFSI)
- **Designation:** The act of adding an entity to a sanctions list
- **SDN:** Specially Designated Nationals (US OFAC terminology)
- **Knowledge Graph Triple:** (Subject, Predicate, Object) — the fundamental unit of the KG

## Data Sources (Typical)

- US OFAC SDN List
- EU Consolidated Sanctions List
- UK OFSI Consolidated List
- UN Security Council Sanctions
- OpenSanctions aggregated dataset

## Commands Reference

```bash
# Install dependencies (once pyproject.toml or requirements.txt is set up)
pip install -r requirements.txt

# Run tests
pytest tests/

# Lint code
ruff check src/

# Format code
ruff format src/
```

## Important Notes for AI Assistants

1. **Data sensitivity:** Sanctions data is publicly available but handle entity information carefully and accurately. Errors in entity matching can have real consequences.
2. **No fabrication:** Never invent or hallucinate sanctions data. Only use verified sources.
3. **Provenance tracking:** Always track where data came from and when it was retrieved.
4. **Idempotent pipelines:** Data ingestion and processing scripts should be safe to re-run.
5. **Incremental development:** This project is being built incrementally. Check existing files before creating new ones.
6. **Keep CLAUDE.md updated:** As the project evolves, update this file to reflect the current state of the codebase.
