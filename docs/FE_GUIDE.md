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

## 6. 📝 Trạng thái & Tiến độ FE

- [x] Header: Bỏ "StatsBomb" branding → thay bằng live DB stats (match/player count)
- [x] MatchCenter: Nhóm theo ngày, hiển thị giờ kickoff, status badge tiếng Việt, xG bar chỉ khi có data
- [x] MatchCenter: Team crest với initials fallback thay vì ui-avatars
- [x] MatchCenter: Empty state Vietnamese khi không có data
- [x] PlayerIntelligence: Tìm kiếm, lọc vị trí, avatar fallback bằng initials
- [x] PlayerIntelligence: Load stats + shots song song (Promise.all), graceful no-stats state
- [x] PlayerComparison: Auto-pick 2 cầu thủ từ danh sách thực; error state tiếng Việt
- [x] AiAnalystChat: Grounded facts, không hallucinate số liệu
- [x] CSS: Thêm .player-avatar-fallback, .match-comp, .match-kickoff, .match-venue, .score-date, .player-layout, .stats-grid, v.v.
- [x] Footer: Bỏ "StatsBomb Open Data" — chỉ còn "PitchPulse — Nền tảng phân tích bóng đá"
- [x] Build production pass: vite build → dist 178KB JS / 16KB CSS
