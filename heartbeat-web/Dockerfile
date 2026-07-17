# syntax=docker/dockerfile:1

# ── Stage 1: Build frontend ───────────────────────────────────────────────────
FROM node:20-alpine AS build

ARG VITE_ENABLE_OBFUSCATION=false
ARG VITE_DISABLE_OBFUSCATION=false
ENV VITE_ENABLE_OBFUSCATION=${VITE_ENABLE_OBFUSCATION}
ENV VITE_DISABLE_OBFUSCATION=${VITE_DISABLE_OBFUSCATION}

WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci

COPY . .
RUN npm run build

# ── Stage 2: Nginx static server ──────────────────────────────────────────────
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -qO- http://127.0.0.1/ || exit 1
