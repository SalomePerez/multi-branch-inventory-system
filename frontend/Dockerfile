# ---- Etapa 1: Build Angular ----
FROM node:20-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm install --legacy-peer-deps

COPY . .
RUN NODE_OPTIONS=--max-old-space-size=1536 npm run build:prod

# ---- Etapa 2: Serve con nginx ----
FROM nginx:alpine AS runtime

COPY --from=build /app/dist/inventory-frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
