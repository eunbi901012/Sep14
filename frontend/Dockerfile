FROM node:20.18.1-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN if [ -f package-lock.json ]; then npm ci; else npm install --package-lock-only && npm ci; fi
COPY tsconfig.json vite.config.ts index.html ./
COPY src ./src
RUN npm run typecheck
RUN npm run build

FROM nginx:1.27.3-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
RUN touch /var/run/nginx.pid \
    && chown -R nginx:nginx /usr/share/nginx/html /var/cache/nginx /var/log/nginx /var/run/nginx.pid
EXPOSE 80
USER nginx
HEALTHCHECK --interval=30s --timeout=5s --retries=5 CMD wget -qO- http://127.0.0.1/ || exit 1
