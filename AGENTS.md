# 🚨 AI AGENT OPERATIONAL DIRECTIVES & MANDATORY CONSTRAINTS

> **CRITICAL PROTOCOL FOR ALL AI AGENTS:**  
> All AI agents (regardless of model, persona, or subagent role) working on or resuming this project **MUST read, acknowledge, and strictly adhere to this document BEFORE executing any task, planning changes, or running commands.**  
> Any violation of these directives will cause immediate task rejection and workflow disruption.

---

## 1. 🛡️ Absolute Workspace Boundary Enforcement (Sandbox Rule)

1. **Strictly Confined to Project Scope:**  
   - All file operations (`view_file`, `write_to_file`, `replace_file_content`, `multi_replace_file_content`, `list_dir`, `grep_search`) and shell commands (`run_command`) **MUST ONLY** target paths inside the current project root:  
     `c:\Users\THINKPAD\Desktop\Football-Analytics-Platform` (and its subdirectories).
2. **Zero Access to External Directories:**  
   - **DO NOT** read, view, edit, search, list, or delete any files or directories outside this project.
   - Specifically, **NEVER touch**:
     - `c:\Users\THINKPAD\Desktop\Trend_of_Tiktok` or other projects on Desktop.
     - `c:\Users\THINKPAD\Desktop\` directly.
     - User home, AppData, system directories, or temporary OS folders (except the designated sandbox artifact scratch directory if needed).
3. **No Traversing Upwards (`..`):**  
   - Relative paths traversing upwards outside the workspace (e.g., `cd ..`, `../../`) are strictly prohibited. The working directory (`Cwd`) for any command execution must always be within `c:\Users\THINKPAD\Desktop\Football-Analytics-Platform`.

---

## 2. 🔒 Fixed Tooling & Technology Stack (No Arbitrary Changes)

1. **Strict Stack Freeze:**  
   - The technology stack, libraries, databases, and architectural tools are **explicitly defined** in [`project.docs`](./project.docs).
   - **NO AI agent is permitted to swap, replace, remove, or introduce alternative core technologies without EXPLICIT, WRITTEN PERMISSION from the USER.**
2. **Authorized Core Stack (As mandated in `project.docs`):**
   - **Frontend:** Next.js (App Router, TypeScript), Tailwind CSS, shadcn/ui, Recharts, SVG/D3.
   - **Backend API:** Python 3.11+, FastAPI (Modular Monolith), Pydantic, SQLAlchemy.
   - **Analytical Database:** ClickHouse (ReplacingMergeTree / Star Schema).
   - **Transactional Database:** PostgreSQL.
   - **Cache & Key-Value:** Redis.
   - **Streaming & Messaging:** Apache Kafka / Redpanda.
   - **Object Lakehouse:** MinIO (Local) / AWS S3.
   - **Transformations & Modeling:** Python + dbt-core.
   - **Pipeline Orchestrator:** Kestra.
   - **AI Analyst Layer:** LangGraph, typed tools, strict guardrails.
3. **Prohibited Unilateral Decisions:**
   - ❌ Replacing FastAPI with Django/Flask/Node.js.
   - ❌ Replacing ClickHouse with DuckDB/MongoDB/Elasticsearch.
   - ❌ Replacing Kafka with RabbitMQ/Celery.
   - ❌ Replacing Tailwind/shadcn with Bootstrap/MUI/Chakra.
   - ❌ Switching package managers or restructuring the monorepo layout without permission.
   - If a new library or tool is proposed, you **MUST ask the user first** with clear rationale and wait for confirmation.

---

## 3. 🎯 Adherence to Architecture & Blueprint (`project.docs`)

1. **Source of Architecture:**  
   - All architectural decisions, data models, Kafka topic definitions, API response envelopes, and sprint plans are documented in [`project.docs`](./project.docs).
   - Agents must consult [`project.docs`](./project.docs) before creating models, endpoints, topics, or components to ensure strict structural consistency.
2. **Modular Monolith First:**  
   - Keep backend services in a modular monolith under `apps/api`. Do not split into microservices prematurely.
3. **End-to-End DoD (Definition of Done):**  
   - A feature is never "done" just because UI code is written. It must respect the full data journey: Source ➔ Kafka ➔ Raw Storage ➔ ClickHouse ➔ FastAPI ➔ UI/AI.

---

## 4. 🧠 AI Analyst Guardrails & Data Truth (Zero Hallucination)

1. **Data Is the Single Source of Truth:**  
   - Statistics, metrics, radar charts, and AI conversational responses must strictly originate from ClickHouse data returned by deterministic API tool calls.
2. **No Arbitrary LLM SQL Generation:**  
   - Agents must never implement patterns where an LLM generates dynamic, unchecked SQL strings directly executed against ClickHouse. All queries must be parameterized and pre-defined in repository methods.
3. **Graceful Uncertainty:**  
   - If data for a match, player, or season is missing or null, the system and the AI must state that data is unavailable—never estimate or interpolate without explicit user labeling.

---

## 5. 🛡️ Git & System Safety Protocols

1. **Non-Destructive Actions:**  
   - Never run destructive git commands like `git reset --hard`, `git clean -fd`, `git push --force`, or branch deletions without explicit instruction.
2. **Secret Hygiene:**  
   - Never hardcode API keys, database credentials, or tokens in source code. Always use `.env` patterns.
3. **Incremental Commits & Verification:**  
   - Keep changes modular, testable, and verified before concluding steps.

---

## 📋 Mandatory Agent Pre-Flight Checklist

Before an AI agent executes any prompt or task in this repository, it must internally verify:
- [x] Have I read `AGENTS.md` and [`project.docs`](./project.docs)?
- [x] Is my current working directory strictly within `c:\Users\THINKPAD\Desktop\Football-Analytics-Platform`?
- [x] Am I modifying only files inside this repository?
- [x] Am I using the exact tech stack agreed upon without unauthorized substitutions?
- [x] Have I received user confirmation if proposing any new tool or architectural divergence?
