# 🖥️ Frontend Engineer (FE) — Hướng Dẫn & Tiến Độ Phát Triển

> **Vai trò:** Frontend Engineer  
> **Thư mục ứng dụng:** `apps/frontend/`  
> **Techstack cốt lõi:** React 18, Vite 6, Vanilla CSS (Dark Glassmorphism Design System)  
> **Containerization:** Multi-stage Docker (Node 22 Alpine builder ➔ Nginx 1.27 Alpine runtime)  
> **Base URL:** `/api/v1` (gọi thẳng backend production hoặc localhost dev)

---

## 1. 🎯 Tổng quan Kiến trúc Frontend

Ứng dụng Frontend là **Single Page Application (SPA)** với giao diện dark-mode cao cấp (Glassmorphism, dynamic animations, no placeholder data):

```
[Người dùng] → [React SPA] → [api.js] → [Spring Boot REST API /api/v1]
                                        → [PostgreSQL analytics DB]
```

---

## 2. 🧰 Công nghệ Sử dụng

| Công cụ | Phiên bản | Mục đích |
| :--- | :--- | :--- |
| **React** | 18.3.1 | Core UI Framework |
| **Vite** | 6.4.3 | Build tool & Dev server HMR |
| **Vanilla CSS** | — | Custom design system (index.css) |
| **Nginx** | 1.27-alpine | Production web server |

---

## 3. 📂 Cấu trúc `apps/frontend/src/`

```
src/
├── main.jsx                  # Entry point ReactDOM
├── App.jsx                   # Main container, tab switching, footer
├── index.css                 # Design system: tokens, animations, all component styles
├── components/
│   ├── Header.jsx            # Navbar + live DB stats indicator (match/player count)
│   ├── MatchCenter.jsx       # Danh sách trận, nhóm theo ngày, xG bar, status badge
│   ├── PlayerIntelligence.jsx # Player list + stats + radar + shot map
│   ├── PlayerComparison.jsx  # So sánh đối đầu 2 cầu thủ, radar song song
│   ├── AiAnalystChat.jsx     # AI chatbot phân tích, grounded facts
│   ├── RadarChart.jsx        # SVG Radar 6 trục từ dữ liệu thực
│   ├── PitchShotMap.jsx      # Pitch SVG shot visualization
│   └── SystemModal.jsx       # System status modal
└── services/
    └── api.js                # API client, NO mock data, throws on failure
```

---

## 4. 🔑 Nguyên tắc thiết kế (Design Principles)

- **Không mock data**: `api.js` luôn throw error nếu backend không trả về — không có fallback giả.
- **Không hiện logo/avatar giả**: Team crest và player avatar chỉ hiển thị nếu có URL thật từ DB. Nếu thiếu: hiển thị initials fallback (CSS `.player-avatar-fallback`).
- **Không nhập thông tin kỹ thuật lên UI**: Xóa bỏ "StatsBomb", khỏi header/subtitle — chỉ hiển thị thông tin có nghĩa với user.
- **Grouping theo ngày**: Matches được nhóm theo ngày (dateKey), sắp xếp mới nhất lên đầu, với label "Hôm nay / Hôm qua / dd/mm/yyyy".
- **Empty state**: Mỗi view có empty state rõ ràng bằng tiếng Việt thay vì hiển thị blank.
- **Graceful stats missing**: PlayerIntelligence hiển thị "Chưa có dữ liệu thống kê" nếu `/stats` API trả về null — không crash.

---

## 5. 🚀 Hướng Dẫn Vận Hành

### Local Development

```bash
cd apps/frontend
npm install
npm run dev        # http://localhost:5173
```

### Production Build

```bash
npm --prefix apps/frontend run build
# dist/ được Nginx phục vụ
```

---

## 6. 📝 Trạng thái & Tiến độ FE & AI (Đã hoàn thiện)

- [x] **Header:** Bỏ hoàn toàn số lượng database kỹ thuật (`650 trận · 4.367 cầu thủ`) và chấm xanh nhấp nháy, thay bằng thanh điều hướng thể thao cao cấp PitchPulse với badge giải đấu 2026/27.
- [x] **Lịch thi đấu (MatchCenter):**
  - Đảo chiều lịch theo chuẩn quốc tế: Trục thời gian từ trái qua phải (từ quá khứ đến tương lai), có nút điều hướng tuần tự `‹` và `›`.
  - Loại bỏ hoàn toàn số trận thừa thãi trong từng nút ngày trên thanh lịch.
  - Phân cấp theo Giải đấu (Tournament/League-first): Gom các trận đấu vào từng bảng giải đấu (Premier League, La Liga, Serie A, Ligue 1...) gọn gàng.
  - Click vào bất kỳ trận đấu nào mở ngay popup `MatchDetailModal` với bảng tỷ số lớn, thời gian, sân vận động, thanh so sánh xG, và nút **"⚡ Hỏi AI Phân Tích Trận Này"**.
- [x] **Trung tâm Cầu thủ (PlayerIntelligence):**
  - Giao diện 2 cột chuẩn FotMob/Sofascore: Roster danh sách bên trái (hỗ trợ lọc theo vị trí FW, MF, DF, GK và tìm kiếm) + Khung Showcase bên phải.
  - Hero profile banner: Avatar, số áo, tên, đội bóng, quốc tịch, nút **"⚡ Hỏi AI Phân Tích"** và **"⚔️ So Sánh Cầu Thủ"**.
  - Lưới 6 chỉ số then chốt (Bàn thắng, Kiến tạo, xG, xA, Bàn/90, Kiến tạo/90) và tab chuyển đổi Radar đa trục vs Bản đồ dứt điểm (PitchShotMap).
- [x] **Đấu trường So sánh Đối đầu (PlayerComparison):**
  - Dual Player Cards (Xanh ngọc vs Xanh cyan), hỗ trợ đổi vị trí 2 cầu thủ nhanh chóng (`⇄ Đổi bên`).
  - Nút **"⚡ Hỏi AI Đánh Giá So Sánh"** kết nối trực tiếp sang AI Analyst với prompt tự động.
  - Thước đo đối đầu (Battle Meters): Hiển thị thanh tiến trình đối đầu theo từng chỉ số kèm vương miện 👑 cho bên vượt trội.
  - Radar đối chiếu song song và Bảng thông số chi tiết.
- [x] **Trợ lý AI Phân tích (AiAnalystChat & AiAnalystService):**
  - Xóa bỏ triệt để việc rò rỉ endpoint kỹ thuật (`GET /api/v1/players`, `GET /api/v1/matches`, `GET /api/v1/system/status`).
  - Lọc bỏ hoàn toàn các bảng debug (`query`, `status: DATA_UNAVAILABLE`, `dim_match / fact_match`).
  - Nâng cấp AI Analyst tự động nhận diện tên 2 đội bóng từ câu hỏi (VD: Bahia vs Mirassol) để đưa ra báo cáo chiến thuật chuyên sâu đa chiều (tỷ số, thế trận, hiệu suất xG, phong độ đội bóng).
  - Tự động cuộn mượt xuống tin nhắn mới nhất và cung cấp gợi ý câu hỏi tiếp theo hoàn toàn tự nhiên.
- [x] **Build & Kiểm thử:** `npm --prefix apps/frontend run build` vượt qua 100% không cảnh báo lỗi.
