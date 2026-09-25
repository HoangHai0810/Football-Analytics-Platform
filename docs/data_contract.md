# 📜 Data Contract: ClickHouse & Streaming Specifications

> **Mục đích:** Đây là bản giao kèo kỹ thuật (Data Contract) chính thức giữa **Data Engineer (DE)** và **Backend Engineer (BE)**.  
> **Nguyên tắc:** Bất kỳ thay đổi nào về tên cột, kiểu dữ liệu, hoặc cấu trúc bảng đều phải được thảo luận và thống nhất giữa 2 bên trước khi triển khai.

---

## 1. Kết nối ClickHouse (Connection Details)

| Thông số | Giá trị Local | Ghi chú |
| :--- | :--- | :--- |
| **HTTP Port** | `8123` | Dành cho REST client / curl / HTTP driver |
| **Native TCP Port** | `9000` | Dành cho clickhouse-connect / Python client tối ưu |
| **Database Name** | `football_analytics` | Database chính chứa toàn bộ marts |
| **User** | `default` | User mặc định dev |
| **Password** | `clickhouse_dev` | Cấu hình trong `.env` |

---

## 2. Dimension Tables (Bảng thứ nguyên)

### 2.1. `dim_competition` (Giải đấu)
- **Engine:** `ReplacingMergeTree(updated_at)`
- **Order Key:** `(competition_id)`

| Cột | Kiểu dữ liệu | Nullable | Mô tả | Ví dụ |
| :--- | :--- | :--- | :--- | :--- |
| `competition_id` | `UInt32` | No | ID định danh giải đấu | `2021` (Premier League) |
| `name` | `String` | No | Tên giải đấu | `"Premier League"` |
| `country` | `String` | No | Quốc gia | `"England"` |
| `type` | `LowCardinality(String)` | No | Thể thức | `"LEAGUE"` hoặc `"CUP"` |
| `updated_at` | `DateTime` | No | Thời điểm cập nhật cuối | `2026-09-24 10:00:00` |

### 2.2. `dim_season` (Mùa giải)
- **Engine:** `ReplacingMergeTree(updated_at)`
- **Order Key:** `(competition_id, season_id)`

| Cột | Kiểu dữ liệu | Nullable | Mô tả | Ví dụ |
| :--- | :--- | :--- | :--- | :--- |
| `season_id` | `UInt32` | No | ID mùa giải | `2024` |
| `competition_id` | `UInt32` | No | ID giải đấu tương ứng | `2021` |
| `name` | `String` | No | Tên hiển thị mùa giải | `"2024/2025"` |
| `start_date` | `Date` | No | Ngày bắt đầu | `2024-08-15` |
| `end_date` | `Date` | No | Ngày kết thúc | `2025-05-25` |
| `updated_at` | `DateTime` | No | Thời điểm cập nhật cuối | `2026-09-24 10:00:00` |

### 2.3. `dim_team` (Đội bóng)
- **Engine:** `ReplacingMergeTree(updated_at)`
- **Order Key:** `(team_id)`

| Cột | Kiểu dữ liệu | Nullable | Mô tả | Ví dụ |
| :--- | :--- | :--- | :--- | :--- |
| `team_id` | `UInt32` | No | ID định danh đội bóng | `65` |
| `name` | `String` | No | Tên đội bóng | `"Manchester City FC"` |
| `country` | `String` | No | Quốc gia | `"England"` |
| `stadium` | `String` | No | Sân vận động | `"Etihad Stadium"` |
| `logo_url` | `String` | No | Đường dẫn logo SVG/PNG | `"https://.../mancity.png"` |
| `updated_at` | `DateTime` | No | Thời điểm cập nhật cuối | `2026-09-24 10:00:00` |

### 2.4. `dim_player` (Cầu thủ)
- **Engine:** `ReplacingMergeTree(updated_at)`
- **Order Key:** `(player_id)`

| Cột | Kiểu dữ liệu | Nullable | Mô tả | Ví dụ |
| :--- | :--- | :--- | :--- | :--- |
| `player_id` | `UInt32` | No | ID định danh cầu thủ | `1024` |
| `name` | `String` | No | Tên đầy đủ cầu thủ | `"Erling Haaland"` |
| `date_of_birth` | `Nullable(Date)` | Yes | Ngày sinh | `2000-07-21` |
| `nationality` | `String` | No | Quốc tịch | `"Norway"` |
| `position` | `LowCardinality(String)` | No | Vị trí chính | `"FW"`, `"MF"`, `"DF"`, `"GK"` |
| `preferred_foot`| `LowCardinality(String)` | No | Chân thuận | `"LEFT"`, `"RIGHT"`, `"BOTH"` |
| `updated_at` | `DateTime` | No | Thời điểm cập nhật cuối | `2026-09-24 10:00:00` |

### 2.5. `dim_match` (Trận đấu)
- **Engine:** `ReplacingMergeTree(updated_at)`
- **Order Key:** `(season_id, match_date, match_id)`

| Cột | Kiểu dữ liệu | Nullable | Mô tả | Ví dụ |
| :--- | :--- | :--- | :--- | :--- |
| `match_id` | `UInt64` | No | ID định danh trận đấu | `3890251` |
| `competition_id` | `UInt32` | No | ID giải đấu | `2021` |
| `season_id` | `UInt32` | No | ID mùa giải | `2024` |
| `home_team_id` | `UInt32` | No | ID đội chủ nhà | `65` |
| `away_team_id` | `UInt32` | No | ID đội khách | `66` |
| `match_date` | `DateTime` | No | Thời gian diễn ra trận đấu | `2026-09-24 19:00:00` |
| `status` | `LowCardinality(String)` | No | Trạng thái | `"SCHEDULED"`, `"FINISHED"` |
| `updated_at` | `DateTime` | No | Thời điểm cập nhật cuối | `2026-09-24 21:00:00` |

---

## 3. Fact Tables (Bảng sự kiện & Thống kê)

### 3.1. `fact_match` (Thống kê tổng kết trận)
- **Engine:** `ReplacingMergeTree(ingested_at)`
- **Order Key:** `(match_id)`

| Cột | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `match_id` | `UInt64` | ID trận đấu |
| `home_team_id` | `UInt32` | ID đội nhà |
| `away_team_id` | `UInt32` | ID đội khách |
| `home_score` | `UInt8` | Bàn thắng đội nhà |
| `away_score` | `UInt8` | Bàn thắng đội khách |
| `home_xg` | `Float32` | Expected Goals đội nhà |
| `away_xg` | `Float32` | Expected Goals đội khách |
| `attendance` | `UInt32` | Lượng khán giả |
| `duration` | `UInt16` | Thời lượng trận (phút) |
| `ingested_at` | `DateTime` | Thời điểm nạp dữ liệu |

### 3.2. `fact_player_match` (Thống kê cầu thủ theo trận)
- **Engine:** `ReplacingMergeTree(ingested_at)`
- **Order Key:** `(player_id, match_id)`

| Cột | Kiểu dữ liệu | Mô tả |
| :--- | :--- | :--- |
| `match_id` | `UInt64` | ID trận đấu |
| `player_id` | `UInt32` | ID cầu thủ |
| `team_id` | `UInt32` | ID đội bóng thi đấu |
| `minutes` | `UInt8` | Số phút thi đấu trên sân |
| `goals` | `UInt8` | Số bàn thắng |
| `assists` | `UInt8` | Số kiến tạo |
| `shots` | `UInt8` | Tổng số cú sút |
| `shots_on_target` | `UInt8` | Cú sút trúng đích |
| `passes` | `UInt16` | Tổng số đường chuyền |
| `key_passes` | `UInt8` | Đường chuyền tạo cơ hội |
| `xg` | `Float32` | Expected Goals của cầu thủ trong trận |
| `xa` | `Float32` | Expected Assists của cầu thủ trong trận |
| `tackles` | `UInt8` | Số pha tắc bóng |
| `interceptions` | `UInt8` | Số pha đánh chặn |
| `duels` | `UInt8` | Số pha tranh chấp tay đôi |
| `pressures` | `UInt16` | Số lần pressing đối phương |
| `ingested_at` | `DateTime` | Thời điểm ghi nhận |

### 3.3. `fact_event` (Chi tiết sự kiện trên sân)
- **Engine:** `MergeTree()`
- **Partition By:** `toYYYYMM(ingested_at)`
- **Order Key:** `(match_id, event_type, minute, second)`

| Cột | Kiểu dữ liệu | Mô tả | Quy chuẩn tọa độ |
| :--- | :--- | :--- | :--- |
| `event_id` | `UUID` | Khóa duy nhất của sự kiện | UUID v4 |
| `match_id` | `UInt64` | ID trận đấu | |
| `player_id` | `UInt32` | Cầu thủ thực hiện | |
| `team_id` | `UInt32` | Đội bóng | |
| `event_type` | `LowCardinality(String)` | Loại sự kiện đã chuẩn hóa | `PASS`, `SHOT`, `TACKLE`, ... |
| `minute` | `UInt8` | Phút diễn ra ($0 \to 120$) | |
| `second` | `UInt8` | Giây diễn ra ($0 \to 59$) | |
| `x` | `Float32` | Tọa độ $x$ bắt đầu | Chuẩn hóa: $0.0 \to 120.0$ |
| `y` | `Float32` | Tọa độ $y$ bắt đầu | Chuẩn hóa: $0.0 \to 80.0$ |
| `end_x` | `Nullable(Float32)` | Tọa độ $x$ kết thúc | Dành cho đường chuyền/dẫn bóng |
| `end_y` | `Nullable(Float32)` | Tọa độ $y$ kết thúc | Dành cho đường chuyền/dẫn bóng |
| `outcome` | `LowCardinality(String)` | Kết quả | `"SUCCESS"`, `"FAIL"`, `"GOAL"`, `"BLOCKED"` |
| `ingested_at` | `DateTime` | Thời điểm nạp | |

---

## 4. Analytical Marts (Bảng pre-aggregated cho BE truy vấn nhanh)

BE nên ưu tiên query vào các bảng Marts do dbt tạo ra để đảm bảo tốc độ phản hồi API $< 50\text{ms}$:

### `mart_player_season_stats`
- Chứa các chỉ số tính sẵn theo mùa:
  - `total_minutes`, `total_goals`, `total_assists`, `total_xg`, `total_xa`
  - Chỉ số trên 90 phút: `goals_per_90`, `assists_per_90`, `xg_per_90`, `xa_per_90`, `shots_per_90`
  - Tỷ lệ: `pass_completion_rate`, `duel_win_rate`

---

## 5. Quy chuẩn giao tiếp Kafka (Kafka Envelope)

Mọi message đẩy vào Kafka topic đều tuân thủ schema JSON này:

```json
{
  "event_id": "c1f7b0e2-8b3d-4c7a-9c6a-4d2c8f6e7b1a",
  "event_type": "football.event.raw",
  "source": "statsbomb",
  "schema_version": 1,
  "occurred_at": "2026-09-24T20:15:30Z",
  "ingested_at": "2026-09-24T20:15:32Z",
  "payload": {
    "match_id": 3890251,
    "player_id": 1024,
    "team_id": 65,
    "event_type": "SHOT",
    "x": 108.5,
    "y": 42.0,
    "xg": 0.45,
    "outcome": "GOAL"
  }
}
```

---

## 6. Cam kết chất lượng dữ liệu (SLA & Quality Guarantees)

1. **Cam kết tính toàn vẹn:** Mọi `match_id` trong fact tables đều có bản ghi tương ứng trong `dim_match`.
2. **Khử trùng lặp:** Dữ liệu được deduplicate tự động bằng cơ chế `ReplacingMergeTree`. BE có thể sử dụng cú pháp `SELECT ... FINAL` khi cần dữ liệu tuyệt đối mới nhất trước đợt merge nền.
3. **Seed Data cho BE:** DE cam kết cung cấp sẵn 10 trận đấu hoàn chỉnh (đủ events, stats) vào ClickHouse để BE tiến hành phát triển API ngay từ Sprint 1.
