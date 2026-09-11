FROM node:20-alpine
WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .

EXPOSE 5173
# --host 0.0.0.0 es obligatorio: sin esto, Vite solo escucha en localhost
# DENTRO del contenedor y no vas a poder acceder desde tu navegador.
# PORT lo define Render en runtime; en Compose local no está seteada,
# así que cae al default 5173 (igual que antes).
CMD ["sh", "-c", "npm run dev -- --host 0.0.0.0 --port ${PORT:-5173}"]