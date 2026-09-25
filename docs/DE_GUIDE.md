# 🛠️ Data Engineer (DE) — Hướng Dẫn Toàn Diện

> **Vai trò:** Data Engineer  
> **Nhánh Git:** `DE/*` (ví dụ: `DE/notthin`)  
> **Thư mục phụ trách:** `data-platform/`, `dbt/`, `infrastructure/`  
> **Điểm bàn giao cho BE:** ClickHouse (`football_analytics` database) với dữ liệu đã được làm sạch và mô hình hóa.

---

## 1. 🎯 Mục tiêu chiến lược của DE

```
[Nguồn dữ liệu ngoài] ──► [DE sở hữu toàn bộ đoạn này] ──► [ClickHouse] ──► [BE sử dụng]

football-data.org ──┐
                    ├──► Ingestion ──► Kafka ──► MinIO (Raw) ──► ETL ──► ClickHouse Marts
StatsBomb OpenData ─┘
```

- DE là **chủ sở hữu dữ liệu**: mọi con số trong ClickHouse phải do DE đảm bảo tính chính xác.
- DE cung cấp **Seed Data sớm nhất có thể** để BE không phải chờ pipeline hoàn chỉnh mới làm được việc.
- DE thiết kế **schema ClickHouse** và không thay đổi tên cột/kiểu dữ liệu mà không thông báo BE trước.

---

## 2. 🧰 Bộ công cụ (Toolset) của DE

### 2.1. Ngôn ngữ & Runtime

| Công cụ | Phiên bản | Mục đích |
| :--- | :--- | :--- |
| **Python** | 3.11+ | Viết toàn bộ ingestion, consumer, transformer workers |
| **uv** hoặc **pip** | Latest | Quản lý dependencies (nên dùng `uv` vì tốc độ cao hơn) |

### 2.2. Data Streaming & Messaging

| Công cụ | Vai trò trong dự án | Ghi chú |
| :--- | :--- | :--- |
| **Apache Kafka** | Hệ thống message broker cốt lõi (Production) | Sử dụng Confluent Cloud hoặc self-hosted |
| **Redpanda** | Kafka-compatible cho local dev | Nhẹ hơn, không cần JVM — chạy bằng Docker 1 container |
| **kafka-python** hoặc **confluent-kafka** | Python client gửi/nhận message | Dùng `confluent-kafka` nếu cần hiệu năng cao |

### 2.3. Raw Data Storage (Data Lake)

| Công cụ | Vai trò | Ghi chú |
| :--- | :--- | :--- |
| **MinIO** | S3-compatible Object Storage cho local dev | Chạy Docker, expose port `9000` (API) + `9001` (Console) |
| **AWS S3** | Raw storage cho Production | Sử dụng cùng SDK `boto3` — chỉ đổi endpoint |
| **boto3** | Python SDK tương tác MinIO/S3 | `pip install boto3` |

### 2.4. Analytical Database

| Công cụ | Vai trò | Ghi chú |
| :--- | :--- | :--- |
| **ClickHouse** | OLAP engine chứa toàn bộ dữ liệu phân tích | Chạy Docker, port `8123` (HTTP) / `9000` (Native) |
| **clickhouse-connect** | Python client chính thức cho ClickHouse | Hỗ trợ async, insert batch, query trả về DataFrame |

### 2.5. Transformations & Modeling

| Công cụ | Vai trò | Ghi chú |
| :--- | :--- | :--- |
| **dbt-core** | Quản lý SQL transformation models, tests | `pip install dbt-core dbt-clickhouse` |
| **dbt-clickhouse** | Adapter kết nối dbt với ClickHouse | Cần cấu hình `profiles.yml` |

### 2.6. Orchestration & Scheduling

| Công cụ | Vai trò | Ghi chú |
| :--- | :--- | :--- |
| **Kestra** | Chạy pipeline theo lịch, quản lý DAG, retry | Chạy Docker, port `8081` — viết flow bằng YAML |

### 2.7. Data Quality & Validation

| Công cụ | Vai trò | Ghi chú |
| :--- | :--- | :--- |
| **Pydantic v2** | Validate schema của mỗi record trước khi nạp | Dùng ngay trong consumer workers |
| **dbt tests** | Assert dữ liệu trong ClickHouse sau khi transform | `not_null`, `unique`, `accepted_values`, custom SQL tests |
| **Great Expectations** *(optional)* | Profiling và kiểm tra thống kê nâng cao | Chỉ dùng nếu cần — không bắt buộc cho MVP |

### 2.8. Development Infrastructure

| Công cụ | Vai trò |
| :--- | :--- |
| **Docker Desktop** | Chạy toàn bộ hạ tầng local (ClickHouse, Redpanda, MinIO, Kestra, PostgreSQL, Redis) |
| **Docker Compose** | Orchestrate multi-container local stack |
| **Make** | Developer ergonomics (`make up`, `make ingest`, `make dbt-run`) |

---

## 3. 📋 Danh sách công việc theo từng Sprint (Checklist)

### ✅ Sprint 1 — Hạ tầng & Seed Data (Ưu tiên cao nhất)

> **Mục tiêu:** Dựng được môi trường local và cung cấp Seed Data cho BE làm việc.

- [ ] **Thiết lập Docker Compose** với đủ 6 service: ClickHouse, Redpanda, MinIO, Kestra, PostgreSQL, Redis.
- [ ] **Tạo Makefile** với các lệnh cơ bản: `make up`, `make down`, `make logs`.
- [ ] **Viết DDL ClickHouse** (trong `infrastructure/clickhouse/`) — tạo database `football_analytics` và tất cả bảng `dim_*`, `fact_*`.
- [ ] **Script Seed Data** — dùng StatsBomb Python library (`statsbombpy`) load khoảng 5–10 trận từ dataset free và nạp thẳng vào ClickHouse để BE có dữ liệu làm việc ngay.
- [ ] **Xác nhận với BE** về schema (tên cột, kiểu dữ liệu) đã khớp với [Data Contract](./data_contract.md).

```python
# Ví dụ: Seed Data nhanh bằng statsbombpy
from statsbombpy import sb

# Load free competition data
competitions = sb.competitions()
matches = sb.matches(competition_id=16, season_id=4)  # Champions League

# Load events của 1 trận
events = sb.events(match_id=7430)
```

### ✅ Sprint 2 — Provider Abstraction & Ingestion Engine

> **Mục tiêu:** Dựng được pipeline kéo dữ liệu từ API và lưu vào MinIO.

- [ ] **Viết `FootballDataProvider` abstract base class** (`data-platform/providers/base.py`).
- [ ] **Triển khai `FootballDataOrgProvider`** — gọi `football-data.org` API:
  - Endpoints cần thiết: `/v4/competitions`, `/v4/matches`, `/v4/teams`, `/v4/persons`.
  - Xử lý rate limiting (10 req/min cho free tier, dùng `time.sleep()` hoặc `tenacity`).
- [ ] **Triển khai `StatsBombProvider`** — đọc open data local/GitHub:
  - Load competitions, matches, events, lineups.
  - Chuẩn hóa hệ tọa độ về chuẩn của project (x: 0→120, y: 0→80).
- [ ] **Viết Raw Sink Worker** — sau khi fetch, lưu JSON thô vào MinIO theo path:
  ```
  football-lake/raw/source=statsbomb/year=2026/month=09/day=25/match_id=7430.json.gz
  ```
- [ ] **Unit test** cho mỗi provider (dùng `pytest` + `responses` library để mock HTTP).

### ✅ Sprint 3 — Kafka Streaming Pipeline

> **Mục tiêu:** Dữ liệu đi qua Kafka, không nạp thẳng vào DB.

- [ ] **Tạo Kafka Topics** (script trong `infrastructure/kafka/create_topics.py`):
  ```
  football.competition.raw     (3 partitions, retention 30 days)
  football.match.raw           (6 partitions)
  football.event.raw           (12 partitions — volume lớn nhất)
  football.player_stats.updated
  football.dlq                 (retention 90 days)
  ```
- [ ] **Kafka Producer** (`data-platform/ingestion/producers.py`):
  - Serialize message theo chuẩn envelope trong [Data Contract](./data_contract.md).
  - Đặt key message là `match_id` hoặc `player_id` để đảm bảo ordering trong partition.
- [ ] **Kafka Consumer + ClickHouse Writer** (`data-platform/consumers/`):
  - Consume từ `football.*.raw`.
  - Validate schema bằng Pydantic.
  - Batch insert vào ClickHouse (gom 1.000 records rồi mới ghi 1 lần để tối ưu I/O).
  - Nếu validate fail → đẩy record lỗi sang `football.dlq` với reason.
- [ ] **Kiểm tra idempotency**: Chạy producer 2 lần với cùng data → ClickHouse vẫn chỉ có 1 bản ghi (nhờ `ReplacingMergeTree`).

### ✅ Sprint 4 — dbt Transformations & Analytics Marts

> **Mục tiêu:** BE có thể query bảng mart và nhận số liệu chuẩn trong < 50ms.

- [ ] **Cấu hình dbt project** (`dbt/dbt_project.yml`, `dbt/profiles.yml`).
- [ ] **Staging models** (`dbt/models/staging/`):
  - `stg_events.sql` — deduplicate và làm sạch `fact_event`.
  - `stg_player_matches.sql` — lọc records hợp lệ từ `fact_player_match`.
- [ ] **Mart models** (`dbt/models/marts/`):
  - `mart_player_season_stats.sql`:
    ```sql
    SELECT
        player_id,
        season_id,
        SUM(goals)    AS total_goals,
        SUM(xg)       AS total_xg,
        SUM(minutes)  AS total_minutes,
        round(SUM(goals) / nullif(SUM(minutes),0) * 90, 2) AS goals_per_90,
        round(SUM(xg)    / nullif(SUM(minutes),0) * 90, 2) AS xg_per_90,
        round(SUM(xa)    / nullif(SUM(minutes),0) * 90, 2) AS xa_per_90
    FROM fact_player_match
    GROUP BY player_id, season_id
    ```
  - `mart_team_season_stats.sql` — tương tự nhưng theo `team_id`.
- [ ] **dbt tests**: Thêm YAML test file cho mỗi model (`not_null`, `unique` trên primary keys).
- [ ] **Chạy `dbt run && dbt test`** thành công với 0 error.

### ✅ Sprint 5 — Data Quality & Kestra Orchestration

> **Mục tiêu:** Pipeline tự chạy hàng ngày, có alert khi lỗi.

- [ ] **Viết Data Quality rules** (`data-platform/validators/`):
  - Tọa độ: `0 <= x <= 120` và `0 <= y <= 80`.
  - xG: `0.0 <= xg <= 1.0`.
  - Referential integrity: mỗi `match_id` trong fact phải tồn tại trong `dim_match`.
- [ ] **Kestra Flow YAML** (`infrastructure/kestra/flows/daily_pipeline.yml`):
  ```yaml
  id: daily_football_pipeline
  namespace: football.analytics
  triggers:
    - id: daily_schedule
      type: io.kestra.core.models.triggers.Schedule
      cron: "0 2 * * *"  # 2AM hàng ngày
  tasks:
    - id: fetch_competitions
      type: io.kestra.core.tasks.scripts.Python
      script: python data-platform/ingestion/fetch_competitions.py
    - id: publish_kafka
      ...
    - id: run_dbt
      ...
    - id: run_dbt_tests
      ...
  ```
- [ ] **Thiết lập alert** khi pipeline thất bại (email hoặc Slack webhook qua Kestra notification).

---

## 4. 📐 Nguyên tắc kỹ thuật bắt buộc

### 4.1. Dữ liệu thô là bất biến (Immutable Raw Data)
- JSON từ API phải được lưu vào MinIO **nguyên vẹn, không sửa đổi** trước khi xử lý.
- Nếu logic transform có lỗi, bạn phải có thể replay từ MinIO mà không cần gọi lại API.

### 4.2. Mọi thứ phải idempotent
- Chạy pipeline 2 lần với cùng input → kết quả trong DB phải giống hệt.
- Đây là điều kiện tối thiểu để Kestra có thể retry khi job thất bại giữa chừng.

### 4.3. Xử lý lỗi theo kiểu non-blocking
- 1 record lỗi không được phép làm cả batch consumer bị crash.
- Record lỗi → DLQ → tiếp tục xử lý record tiếp theo.

### 4.4. Không hardcode credentials
- Mọi API key, database password phải đọc từ biến môi trường (`.env` file + `python-dotenv`).

---

## 5. 🤝 Giao diện tiếp xúc với BE (Interface Contract)

| Điều BE cần | DE phải cung cấp |
| :--- | :--- |
| Truy vấn thống kê cầu thủ | Bảng `mart_player_season_stats` trong ClickHouse luôn có dữ liệu |
| Tọa độ cú sút cho Shot Map | Cột `x`, `y`, `xg`, `outcome` trong `fact_event` với `event_type = 'SHOT'` |
| Thông tin đội bóng | Bảng `dim_team` đầy đủ `team_id`, `name`, `logo_url` |
| Dữ liệu mới nhất | Seed data kịp thời; Kestra pipeline chạy đúng lịch hàng ngày |
| Schema ổn định | Thông báo BE **ít nhất 1 ngày trước** khi thay đổi cấu trúc bảng |
