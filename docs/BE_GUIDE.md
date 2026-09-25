# ⚙️ Backend Engineer (BE) — Hướng Dẫn Toàn Diện

> **Vai trò:** Backend Engineer  
> **Nhánh Git:** `BE/*`  
> **Thư mục phụ trách:** `apps/api/`, `apps/web/` (API routes), `ai/`  
> **Điểm đầu vào dữ liệu:** ClickHouse `football_analytics` database (do DE bàn giao)

---

## 1. 🎯 Mục tiêu chiến lược của BE

```
[ClickHouse] ──► [BE sở hữu toàn bộ đoạn này] ──► [Frontend / User]

ClickHouse ──► FastAPI (Repositories + Services) ──► Redis Cache ──► Next.js App
                            │
                         LangGraph AI Agent ──► LLM Provider ──► User
```

- BE là **lớp phục vụ dữ liệu**: Lấy dữ liệu từ ClickHouse ra, đóng gói, cache và phục vụ API.
- BE **không tính toán số liệu phức tạp** — mọi aggregation đã được DE tính sẵn trong dbt marts.
- BE đảm bảo **AI Agent không bao giờ bịa số liệu** — mọi con số phải đến từ tool call.

---

## 2. 🧰 Bộ công cụ (Toolset) của BE

### 2.1. Ngôn ngữ & Framework Backend

| Công cụ | Phiên bản | Mục đích |
| :--- | :--- | :--- |
| **Python** | 3.11+ | Ngôn ngữ chính cho API và AI service |
| **FastAPI** | 0.111+ | Framework xây dựng REST API hiệu năng cao, async native |
| **Pydantic v2** | 2.x | Validate request/response body, type-safe schema |
| **SQLAlchemy** | 2.x (async) | ORM cho PostgreSQL (bảng user, auth, metadata) |
| **uvicorn** | Latest | ASGI server chạy FastAPI |
| **uv** hoặc **pip** | Latest | Quản lý dependencies |

### 2.2. Databases & Connections

| Công cụ | Vai trò | Ghi chú |
| :--- | :--- | :--- |
| **clickhouse-connect** | Kết nối ClickHouse từ Python | Python client chính thức, hỗ trợ async |
| **asyncpg** | Kết nối PostgreSQL async | Dùng với SQLAlchemy async engine |
| **redis-py** | Kết nối Redis | Dùng `aioredis` hoặc `redis.asyncio` cho async context |
| **PostgreSQL** | OLTP: lưu users, bookmarks, alerts | Port `5432` — chạy qua Docker Compose |
| **Redis** | Cache API responses, rate limiting | Port `6379` — chạy qua Docker Compose |
| **ClickHouse** | OLAP: nguồn dữ liệu phân tích | Port `8123` / `9000` — do DE setup và maintain |

### 2.3. Authentication & Security

| Công cụ | Vai trò | Ghi chú |
| :--- | :--- | :--- |
| **python-jose** | Encode/Decode JWT tokens | `pip install python-jose[cryptography]` |
| **passlib** | Hash password | `pip install passlib[bcrypt]` |
| **slowapi** | Rate limiting middleware cho FastAPI | Dùng Redis backend |

### 2.4. AI Analyst Stack

| Công cụ | Vai trò | Ghi chú |
| :--- | :--- | :--- |
| **LangGraph** | Xây dựng stateful AI agent workflow | `pip install langgraph` |
| **langchain-core** | Base interfaces cho tools, messages | Dependency của LangGraph |
| **langchain-openai** | Tích hợp OpenAI API (GPT-4o, GPT-4o-mini) | `pip install langchain-openai` |
| **OpenAI API** | LLM provider chính | Có thể đổi sang Anthropic Claude bằng `langchain-anthropic` |

### 2.5. Testing

| Công cụ | Vai trò |
| :--- | :--- |
| **pytest** | Test runner chính |
| **pytest-asyncio** | Test async FastAPI endpoints |
| **httpx** | HTTP client để test API (dùng với FastAPI `TestClient`) |
| **factory-boy** | Tạo test fixtures cho database |
| **testcontainers** | Spin up ClickHouse/PostgreSQL thật trong tests (integration tests) |

### 2.6. Frontend (API Routes / BFF Layer)

| Công cụ | Vai trò |
| :--- | :--- |
| **Next.js 15** | Framework web chính (App Router) |
| **TypeScript** | Type-safe frontend code |
| **Tailwind CSS + shadcn/ui** | UI components |
| **Recharts** | Biểu đồ thống kê, trend charts |
| **D3.js / SVG** | Pitch visualization (shot map, heatmap) |

---

## 3. 📋 Danh sách công việc theo từng Sprint (Checklist)

### ✅ Sprint 1 — Khung sườn FastAPI & Đọc Seed Data từ ClickHouse

> **Mục tiêu:** Có API endpoint đầu tiên trả dữ liệu thật từ ClickHouse (dùng Seed Data của DE).

**Cấu trúc thư mục `apps/api/app/`:**
```
api/
├── api/
│   └── v1/
│       ├── __init__.py
│       ├── competitions.py
│       ├── matches.py
│       ├── players.py
│       └── teams.py
├── core/
│   ├── config.py       # Pydantic Settings từ .env
│   ├── exceptions.py   # Global exception handlers
│   └── deps.py         # Dependency injection (DB connections)
├── domain/
│   └── models.py       # Domain entities (Player, Match, Team)
├── repositories/
│   ├── clickhouse.py   # Queries trực tiếp vào ClickHouse
│   └── postgres.py     # CRUD cho PostgreSQL
├── services/
│   └── player_service.py  # Business logic + cache
├── schemas/
│   ├── player.py       # Pydantic request/response models
│   └── common.py       # Envelope response models
└── main.py
```

**Checklist:**
- [ ] **Khởi tạo FastAPI app** với lifespan context (mở/đóng connection pool khi startup/shutdown).
- [ ] **Cấu hình `core/config.py`** dùng Pydantic `BaseSettings` đọc từ `.env`.
- [ ] **ClickHouse Repository** — viết hàm `get_player_season_stats(player_id, season_id)` lấy từ mart.
- [ ] **Endpoints đầu tiên** với dữ liệu thật từ ClickHouse:
  - `GET /api/v1/players/{id}` → `dim_player`
  - `GET /api/v1/competitions` → `dim_competition`
  - `GET /api/v1/matches?season_id=...` → `dim_match`
- [ ] **Chuẩn hóa Response Envelope** cho tất cả endpoints:
  ```json
  {
    "data": { ... },
    "meta": { "execution_time_ms": 12.4, "cached": false, "version": "v1" }
  }
  ```

### ✅ Sprint 2 — Redis Cache & Rate Limiting

> **Mục tiêu:** API chịu được tải cao, endpoint cache trả về < 100ms.

- [ ] **Tích hợp Redis Cache** vào `services/player_service.py`:
  ```python
  async def get_player_overview(player_id: int) -> PlayerOverview:
      cache_key = f"player:{player_id}:overview"
      cached = await redis.get(cache_key)
      if cached:
          return PlayerOverview.parse_raw(cached)
      
      data = await clickhouse_repo.get_player(player_id)
      await redis.setex(cache_key, 86400, data.json())  # TTL 24h
      return data
  ```
- [ ] **Cache theo tầng** (tham chiếu [Data Contract](./data_contract.md)):
  - `player:*:overview` → TTL 24h
  - `player:*:{season_id}:stats` → TTL 6h
  - `match:*:summary` → TTL 1h
  - `ai:cache:{query_hash}` → TTL 30 phút
- [ ] **Rate Limiting với slowapi**:
  - General endpoints: `100/minute` per IP.
  - AI endpoint `/api/v1/ai/analyze`: `20/minute` per IP.
- [ ] **Global Exception Handler** chuẩn hóa lỗi:
  ```json
  { "error": { "code": "PLAYER_NOT_FOUND", "message": "Player 99999 not found." } }
  ```

### ✅ Sprint 3 — Statistics, Comparison & Shot Map Endpoints

> **Mục tiêu:** Cung cấp đủ dữ liệu cho frontend render Player Profile, Team Profile, Comparison.

- [ ] **Player Statistics Endpoint:**
  - `GET /api/v1/players/{id}/stats?season_id=2024` → Query `mart_player_season_stats`.
- [ ] **Player Shot Map Endpoint:**
  - `GET /api/v1/players/{id}/shots?season_id=2024`:
    ```json
    {
      "data": {
        "shots": [
          { "x": 108.5, "y": 42.0, "xg": 0.45, "outcome": "GOAL", "minute": 67 }
        ]
      }
    }
    ```
  - Query `fact_event WHERE event_type = 'SHOT' AND player_id = ... AND season_id = ...`
- [ ] **Player Comparison Endpoint:**
  - `GET /api/v1/players/compare?ids=1024,1032&season_id=2024` → Query mart cho cả 2 player cùng lúc.
- [ ] **Team Stats Endpoint:**
  - `GET /api/v1/teams/{id}/stats?season_id=2024` → Query `mart_team_season_stats`.

### ✅ Sprint 4 — AI Football Analyst (LangGraph)

> **Mục tiêu:** AI trả lời câu hỏi phân tích bóng đá dựa 100% trên số liệu thật, không bịa.

**Luồng xử lý bắt buộc:**
```
User Input
    │
    ▼
LangGraph Supervisor Node
    │ (Xác định loại câu hỏi: player / team / comparison / historical)
    ▼
Tool Selection Node
    │ (Chọn tool phù hợp dựa vào intent)
    ▼
Tool Execution ──► ClickHouse (qua repository có sẵn)
    │ (Nhận structured JSON data)
    ▼
Synthesis Node (LLM)
    │ (Chỉ được phép diễn giải data có sẵn, không được tự tạo số)
    ▼
Response trả về user
```

**Typed Tools cần triển khai** (`ai/tools/`):
```python
from langchain_core.tools import tool

@tool
def get_player_season_stats(player_id: int, season_id: int) -> dict:
    """
    Lấy thống kê của 1 cầu thủ trong 1 mùa giải.
    Trả về: goals, assists, xg, xa, minutes, goals_per_90, xg_per_90, xa_per_90.
    """
    return clickhouse_repo.get_player_season_stats(player_id, season_id)

@tool
def compare_players(player_ids: list[int], season_id: int) -> dict:
    """So sánh nhiều cầu thủ trên các chỉ số chuẩn hóa trên 90 phút."""
    ...

@tool
def get_shot_data(player_id: int, season_id: int) -> dict:
    """Lấy toàn bộ dữ liệu cú sút (tọa độ + xG) để phân tích xu hướng sút."""
    ...

@tool
def get_team_season_stats(team_id: int, season_id: int) -> dict:
    """Lấy thống kê tập thể của đội bóng trong mùa giải."""
    ...
```

**Guardrails bắt buộc — ghi vào System Prompt:**
```text
You are a football analytics assistant. Rules you MUST follow:
1. NEVER invent or estimate a statistic. Only cite numbers from tool results.
2. If a tool returns no data, say "Data is not available for this query."
3. All calculations (per-90 rates, averages) are already computed in tool results. Do not recalculate.
4. You may only call tools listed in your tool registry. No raw SQL queries.
```

- [ ] **Triển khai LangGraph StateGraph** (`ai/graphs/football_analyst.py`).
- [ ] **Viết tất cả typed tools** với docstring rõ ràng để LLM hiểu khi nào dùng tool nào.
- [ ] **Endpoint streaming** `POST /api/v1/ai/analyze` — stream token response về frontend.
- [ ] **Test guardrail** — thử prompt LLM "bịa" số liệu, đảm bảo nó từ chối và dùng tool.

### ✅ Sprint 5 — Frontend Integration (Next.js)

> **Mục tiêu:** Người dùng có thể thực hiện toàn bộ luồng: Tìm kiếm → Xem Profile → So sánh → Hỏi AI.

- [ ] **Cấu hình Next.js API Routes (BFF)** — proxy request từ browser đến FastAPI backend.
- [ ] **Player Profile Page** (`/players/[id]`):
  - Biểu đồ radar (Recharts) hiển thị `xg_per_90`, `goals_per_90`, `xa_per_90`.
  - Shot Map bằng SVG: map tọa độ `(x, y)` từ chuẩn [0,120]×[0,80] sang viewBox SVG.
- [ ] **Comparison Page** (`/compare`): Side-by-side 2 player, highlight sự khác biệt.
- [ ] **AI Chat Interface** (`/ai`): Streaming UI, hiển thị response từng token.

---

## 4. 📐 Nguyên tắc kỹ thuật bắt buộc

### 4.1. ClickHouse là Read-Only với BE
- BE **chỉ được đọc (SELECT)** từ ClickHouse.
- **Không được phép INSERT, UPDATE, DELETE, ALTER TABLE** vào ClickHouse từ code API — đó là trách nhiệm của DE.

### 4.2. Không tự tính toán số liệu phức tạp
- Nếu cần `goals_per_90`, không tự tính trong Python service — query từ `mart_player_season_stats` (DE đã tính sẵn).
- Nếu mart chưa có số liệu cần thiết → yêu cầu DE thêm cột vào mart, không tự JOIN fact tables.

### 4.3. AI Tools là cầu nối duy nhất với dữ liệu
- LLM **không được phép** nhận raw ClickHouse SQL string từ user rồi thực thi.
- Mọi truy cập dữ liệu của AI phải thông qua hàm Python có định nghĩa rõ ràng (typed tools).

### 4.4. Async everywhere
- Mọi handler FastAPI, repository, service đều phải dùng `async/await`.
- Không dùng blocking I/O (requests, psycopg2 sync) trong context async.

### 4.5. Không hardcode credentials
- Tất cả connection string, API key đọc từ biến môi trường qua `core/config.py`.

---

## 5. 🤝 Giao diện tiếp xúc với DE (Interface Contract)

| Điều BE cần | BE phải hỏi DE |
| :--- | :--- |
| Thêm chỉ số mới vào mart | Yêu cầu DE thêm cột vào `mart_player_season_stats` |
| Schema bảng thay đổi | DE cam kết thông báo trước 1 ngày, BE cập nhật Pydantic models |
| Dữ liệu mùa giải mới | DE chạy Kestra pipeline nạp dữ liệu mùa mới vào ClickHouse |
| Kiểm tra dữ liệu bất thường | Mở issue trên GitHub, đánh tag `[DE]` để DE xử lý |
