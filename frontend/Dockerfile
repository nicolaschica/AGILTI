# --- Etapa 1: build de la app con Node LTS ---
FROM node:22-alpine AS build
WORKDIR /app

# Vite "hornea" las variables VITE_* en el bundle al compilar, por eso se
# pasa como build arg. Es la URL que usa el NAVEGADOR (localhost del host).
# El frontend solo conoce el graphql-gateway: todas sus peticiones van a
# POST $VITE_GATEWAY_URL/graphql.
ARG VITE_GATEWAY_URL=http://localhost:8082
ENV VITE_GATEWAY_URL=$VITE_GATEWAY_URL

COPY package.json package-lock.json* ./
RUN npm ci || npm install
COPY . .
RUN npm run build

# --- Etapa 2: servir los estaticos con nginx ---
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
