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

- [x] Header: Bỏ hoàn toàn số lượng database/chấm xanh kỹ thuật, thay bằng thanh điều hướng thể thao cao cấp và chuyên nghiệp
- [x] MatchCenter: Thiết kế lại theo chuẩn Sofascore/FotMob:
  - Thanh chọn ngày ngang (Date Ribbon) trực quan: Hôm nay, Hôm qua, Ngày mai và các ngày có trận đấu kèm số lượng trận
  - Phân cấp chuẩn theo Giải đấu (Tournament/League-first): Gom các trận vào từng bảng giải đấu (Premier League, La Liga, Serie A...) thay vì dàn trải lộn xộn
  - Match Row Card tương tác cao: Logo đội, tỷ số/giờ đá, sân vận động, badge xG
- [x] MatchDetailModal: Bấm vào bất kỳ trận đấu nào để mở popup chi tiết (Bảng tỷ số lớn, thời gian, sân vận động, khán giả, thanh so sánh xG, nút "⚡ Hỏi AI Phân Tích Trận Này")
- [x] AI Analyst: Kết nối trực tiếp từ MatchCenter và PlayerIntelligence qua `onAskAi`, tự động điều hướng sang tab AI kèm context trận/cầu thủ
- [x] AiAnalystChat: Thay thế các prompt lỗi bằng bộ prompt thực chiến hoạt động 100%, bổ sung chip gợi ý câu hỏi tiếp theo
- [x] Backend AI: `AiAnalystService` hỗ trợ phân tích chi tiết trận đấu (`matchId`), phân tích phong độ đội bóng và tổng quan các giải đấu
- [x] Build production pass: vite build → dist hoàn tất thành công không lỗi
