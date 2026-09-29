# ============================================================
# Stage 1: Build - Node 22 Alpine builder
# ============================================================
FROM node:22-alpine AS builder

WORKDIR /app

# Accept build argument for API URL (default: Nginx reverse proxy path)
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

# Copy package files first for better layer caching
COPY package*.json ./
RUN npm ci --silent

# Copy all source files and build
COPY . .
RUN npm run build

# ============================================================
# Stage 2: Runtime - Nginx serves the static build output
# ============================================================
FROM nginx:1.27-alpine AS runtime

# Remove default nginx page
RUN rm -rf /usr/share/nginx/html/*

# Copy built assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Nginx config for SPA + reverse proxy
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost/nginx-health || exit 1

CMD ["nginx", "-g", "daemon off;"]
