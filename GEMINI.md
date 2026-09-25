# AI Operational Rules & Behavioral Directives

This project enforces strict workspace boundaries, frozen technology stacks, and deterministic data patterns.

👉 **MANDATORY RULES DOCUMENT:** See [AGENTS.md](./AGENTS.md) for full binding constraints.

## Critical Summaries for All Agents:

1. **Workspace Boundary:** Only interact with files inside `c:\Users\THINKPAD\Desktop\Football-Analytics-Platform`. Never access or modify external directories (e.g. `Desktop\Trend_of_Tiktok`, parent directories, or system paths).
2. **Tool & Stack Freeze:** Never replace or introduce alternative core technologies (FastAPI, ClickHouse, PostgreSQL, Kafka/Redpanda, Redis, MinIO, Kestra, Next.js, LangGraph) without explicit user permission.
3. **Mandatory Reference:** Consult [project.docs](./project.docs) for architectural specifications, data schemas, API envelopes, and sprint plans before building any component.
4. **Data Integrity:** Analytical data in ClickHouse is the single source of truth. No LLM-generated arbitrary SQL, no statistical hallucination.
