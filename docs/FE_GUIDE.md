# 🖥️ Frontend Engineer (FE) — Hướng Dẫn & Tiến Độ Phát Triển

> **Vai trò:** Frontend Engineer  
> **Thư mục ứng dụng:** `apps/frontend/`  
> **Techstack cốt lõi:** React 18, Vite 6, Tailwind CSS, Lucide Icons  
> **Containerization:** Multi-stage Docker (Node 22 Alpine builder ➔ Nginx 1.27 Alpine runtime)  
> **Base URL:** `/api/v1` (Nginx reverse-proxy ➔ Spring Boot backend:8000)

---

## 1. 🎯 Tổng quan Kiến trúc Frontend

Ứng dụng Frontend được xây dựng dưới dạng **Single Page Application (SPA)** hiện đại, hướng đến trải nghiệm cao cấp (Glassmorphism, Dark Mode Slate-900, Dynamic Visualizations):

```
[Trình duyệt Người dùng] (http://localhost:3000)
         │
         ▼
[Nginx Reverse Proxy & Static Server] (Port 80 bên trong container `football_frontend`)
   ├── /assets/*  ──► Phục vụ file bundle JS/CSS đã nén gzip & cached
   ├── /health    ──► Proxy đến Spring Boot Backend (http://backend:8000/health)
   ├── /nginx-health ──► Docker Container Healthcheck endpoint
   └── /api/*     ──► Proxy trong suốt đến Spring Boot Backend (http://backend:8000/api/*)
```

---

## 2. 🧰 Thư viện & Công nghệ Sử dụng

| Công cụ / Thư viện | Phiên bản | Mục đích & Vai trò |
| :--- | :--- | :--- |
| **React** | 18.3.1 | Core UI Framework với React Hooks (`useState`, `useEffect`, `useMemo`) |
| **Vite** | 6.4.3 | Fast build tool & Dev server HMR (port 5173 ở dev, static build ở prod) |
| **Tailwind CSS** | 3.4.17 | Styling tiện ích, bảng màu Dark Football theme (Emerald, Blue, Amber, Slate) |
| **Lucide React** | 1.16.0 | Hệ thống icon chuẩn mực, hiện đại |
| **Nginx** | 1.27-alpine | Web server production, hỗ trợ gzip, browser caching, reverse proxy |

---

## 3. 📂 Cấu trúc Thư mục `apps/frontend/`

```
apps/frontend/
├── Dockerfile                   # Multi-stage Docker: Node 22 build ➔ Nginx runtime
├── .dockerignore                # Loại trừ node_modules, dist, .env
├── nginx.conf                   # Cấu hình Nginx: SPA fallback, gzip, proxy_pass /api/
├── package.json                 # Dependencies & build scripts
├── vite.config.js               # Vite config + proxy dev port 8000
├── src/
│   ├── main.jsx                 # Entry point ReactDOM
│   ├── App.jsx                  # Main Dashboard Container, tab switching, live status banner
│   ├── index.css                # Tailwind directives & custom animations
│   ├── components/
│   │   ├── Header.jsx           # Topbar navigation, live status indicator, season selector
│   │   ├── PlayerIntelligence.jsx # Danh sách cầu thủ, metric filter, radar chart preview
│   │   ├── RadarChart.jsx       # Custom SVG Radar 6 trục (Shooting, Passing, Dribbling, etc.)
│   │   ├── PlayerComparison.jsx # So sánh đối đầu 2 cầu thủ, diff bars & radar song song
│   │   └── AiAnalystChat.jsx    # Chatbot phân tích chiến thuật, canned prompts, markdown formatting
│   └── services/
│       └── api.js               # API Client trung tâm, fallback mock data thông minh
```

---

## 4. 🚀 Hướng Dẫn Vận Hành

### 4.1. Chạy với Docker (Khuyến nghị cho Production/Staging)

```bash
# Build image frontend
docker-compose build frontend

# Chạy container frontend (sẽ tự chạy cùng backend nếu khởi động stack)
docker-compose up -d frontend

# Truy cập Dashboard tại:
http://localhost:3000
```

### 4.2. Chạy Local Development (Vite Dev Server)

```bash
cd apps/frontend
npm install
npm run dev
# Mở trình duyệt tại: http://localhost:5173
```

---

## 5. 🛡️ Xử lý Dữ liệu Ngoại tuyến & Kết nối Backend

Để đảm bảo frontend luôn hoạt động mượt mà ngay cả khi Data Engineering pipeline đang chạy hoặc ClickHouse chưa hoàn tất sync:
- `api.js` được tích hợp cơ chế **Graceful Fallback**:
  - Gửi request đến Spring Boot REST API (`/api/v1/...`).
  - Nếu kết nối thành công: hiển thị nhãn **LIVE BACKEND** (màu xanh ngọc).
  - Nếu backend đang khởi động hoặc chưa online: tự động phục vụ **High-Fidelity Seed Data Store** với độ chính xác cao và hiển thị nhãn **DEMO SEED MODE** (màu hổ phách) minh bạch với người dùng.

---

## 6. 📝 Trạng thái & Tiến độ FE

- [x] Giao diện Dashboard tổng quan (`App.jsx`, `Header.jsx`)
- [x] Radar Chart phân tích 6 chiều kỹ chiến thuật bằng SVG toán học chuẩn xác
- [x] Module Phân tích Cầu thủ (`PlayerIntelligence.jsx`) với bộ lọc vị trí & chỉ số
- [x] Module So sánh Đối đầu (`PlayerComparison.jsx`) trực quan với delta bars
- [x] Module AI Assistant (`AiAnalystChat.jsx`) gợi ý chiến thuật, tóm tắt phong độ
- [x] Docker hóa hoàn toàn với Nginx reverse-proxy
- [x] Đã kiểm tra build production thành công 100% (`vite build` -> dist)
