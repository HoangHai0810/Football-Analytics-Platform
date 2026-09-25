# ⚙️ Backend Engineer (BE) — Hướng Dẫn & Kiến Trúc Spring Boot

> **Vai trò:** Backend Engineer  
> **Thư mục phụ trách:** `apps/backend/`  
> **Techstack cốt lõi:** Java 17, Spring Boot 3.3.4, Maven, ClickHouse JDBC Client  
> **Containerization:** Multi-stage Docker (Maven 3.9 + Temurin JDK 17 builder ➔ Temurin JRE 17 runtime)  
> **Cổng dịch vụ:** `8000` (Container & Host)  
> **Điểm đầu vào dữ liệu:** ClickHouse `football_analytics` database (cổng 8123) + High-Fidelity In-Memory Store

---

## 1. 🎯 Mục tiêu chiến lược của BE

```
[ClickHouse OLAP] ◄── JDBC Connection ──► [Spring Boot REST API] ──► [Frontend React / Client]
(football_analytics)                      (apps/backend)              (http://localhost:3000)
                                                ▲
                                                │ Fallback / Standby
                                        [SeedDataStore]
                                 (High-Fidelity Official Stats)
```

- BE là **lớp phục vụ dữ liệu phân tích**: Truy vấn trực tiếp từ ClickHouse data marts (`mart_player_season_stats`, `dim_player`, `dim_match`, `dim_competition`).
- BE có cơ chế **High-Fidelity Seed Fallback**: Tự động phục vụ dữ liệu mẫu có độ tin cậy cao nếu ClickHouse chưa hoàn tất dữ liệu ingestion từ DE.
- Đảm bảo **Data Integrity & Consistency**: Không sinh số liệu ngẫu nhiên, số liệu tuân thủ nghiêm ngặt data contract.

---

## 2. 🧰 Bộ công cụ (Toolset) của BE

### 2.1. Ngôn ngữ & Framework
| Công cụ | Phiên bản | Mục đích |
| :--- | :--- | :--- |
| **Java** | 17 (LTS) | Nền tảng thực thi mạnh mẽ, type-safety cao |
| **Spring Boot** | 3.3.4 | Web MVC, Dependency Injection, Configuration |
| **Spring Boot Starter Web** | 3.3.4 | REST Controller, JSON Serialization (Jackson) |
| **Spring Boot Actuator** | 3.3.4 | Production health check & metrics (`/health`, `/actuator/health`) |
| **ClickHouse JDBC Driver** | 0.6.3 (all) | Kết nối hiệu năng cao với ClickHouse OLAP |
| **Maven** | 3.9+ | Quản lý dependencies và build artifact |

### 2.2. Profiles & Kết Nối Database
- **Local Profile (`application.yml`)**: Kết nối ClickHouse tại `localhost:8123`.
- **Docker Profile (`application-docker.yml`)**: Kết nối ClickHouse container qua internal network tại hostname `clickhouse:8123`.
- Biến môi trường hỗ trợ override linh hoạt:
  - `CLICKHOUSE_HOST` (mặc định: `localhost` hoặc `clickhouse`)
  - `CLICKHOUSE_PORT` (mặc định: `8123`)
  - `CLICKHOUSE_DB` (mặc định: `football_analytics`)
  - `CLICKHOUSE_USER` (mặc định: `default`)
  - `CLICKHOUSE_PASSWORD` (mặc định: `clickhouse_dev`)

---

## 3. 📂 Cấu trúc Thư mục `apps/backend/`

```
apps/backend/
├── Dockerfile                      # Multi-stage: Maven builder ➔ Temurin JRE 17 runtime
├── .dockerignore                   # Exclude target/, .mvn, .idea
├── pom.xml                         # Maven dependencies & plugins
├── src/main/
│   ├── java/com/football/analytics/
│   │   ├── FootballAnalyticsApiApplication.java # Spring Boot main entry
│   │   ├── config/
│   │   │   └── CorsConfig.java     # CORS configuration hỗ trợ React dev & Docker
│   │   ├── controller/
│   │   │   ├── AiAnalystController.java     # /api/v1/ai/analyze & /insights
│   │   │   ├── CompetitionController.java   # /api/v1/competitions
│   │   │   ├── MatchController.java         # /api/v1/matches
│   │   │   ├── PlayerController.java        # /api/v1/players, /compare, /radar
│   │   │   ├── SystemController.java        # /health, /api/v1/system/status
│   │   │   └── TeamController.java          # /api/v1/teams
│   │   ├── dto/
│   │   │   ├── ApiError.java
│   │   │   ├── ApiResponse.java             # Chuẩn hóa Envelope: data + meta
│   │   │   ├── Meta.java                    # execution_time_ms, cached, version
│   │   │   ├── PlayerComparisonDto.java
│   │   │   ├── RadarMetricDto.java          # 6 trục phân tích radar
│   │   │   └── SystemStatusDto.java
│   │   ├── model/
│   │   │   ├── Competition.java
│   │   │   ├── Match.java
│   │   │   ├── Player.java
│   │   │   ├── PlayerStats.java
│   │   │   └── Team.java
│   │   ├── repository/
│   │   │   ├── ClickHouseRepository.java    # Native JDBC queries sang ClickHouse
│   │   │   └── SeedDataStore.java           # Dữ liệu fallback chuẩn mực
│   │   └── service/
│   │       ├── AiAnalystService.java        # Xử lý phân tích chiến thuật
│   │       └── PlayerService.java           # Business logic + ClickHouse/Seed router
│   └── resources/
│       ├── application.yml                 # Default config (local)
│       └── application-docker.yml          # Docker container config
```

---

## 4. 🌐 Danh Sách API Endpoints

Tất cả endpoints trả về theo định dạng Response Envelope chuẩn:
```json
{
  "data": { ... },
  "meta": {
    "execution_time_ms": 4,
    "cached": false,
    "version": "1.0.0"
  }
}
```

| Method | Endpoint | Mô tả |
| :--- | :--- | :--- |
| `GET` | `/health` | Container liveness check (`{"status":"ok", ...}`) |
| `GET` | `/api/v1/system/status` | Tình trạng kết nối ClickHouse, pipeline DE, và counts |
| `GET` | `/api/v1/competitions` | Danh sách giải đấu (Premier League, La Liga, UCL,...) |
| `GET` | `/api/v1/teams` | Danh sách câu lạc bộ bóng đá |
| `GET` | `/api/v1/matches` | Danh sách trận đấu (hỗ trợ lọc `?season=2023-24`) |
| `GET` | `/api/v1/players` | Danh sách cầu thủ (hỗ trợ tìm kiếm `?q=`, `?position=`) |
| `GET` | `/api/v1/players/{id}` | Chi tiết thông tin cầu thủ |
| `GET` | `/api/v1/players/{id}/stats` | Thống kê mùa giải (Goals, xG, Assists, xA, Passes,...) |
| `GET` | `/api/v1/players/{id}/radar` | Dữ liệu biểu đồ radar 6 trục (thang điểm 0 - 100) |
| `GET` | `/api/v1/players/compare` | So sánh đối đầu 2 cầu thủ (`?player1=...&player2=...`) |
| `POST` | `/api/v1/ai/analyze` | AI Assistant phân tích chiến thuật dựa trên dữ liệu thật |

---

## 5. 🚀 Hướng Dẫn Vận Hành

### 5.1. Chạy với Docker
```bash
# Build backend image
docker-compose build backend

# Chạy container
docker-compose up -d backend

# Kiểm tra log
docker-compose logs -f backend
```

### 5.2. Chạy Local (Maven Wrapper hoặc mvn)
```bash
cd apps/backend
mvn spring-boot:run
# API sẵn sàng tại: http://localhost:8000
```
