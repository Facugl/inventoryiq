FROM node:20-alpine
WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .

EXPOSE 5173
# --host 0.0.0.0 es obligatorio: sin esto, Vite solo escucha en localhost
# DENTRO del contenedor y no vas a poder acceder desde tu navegador.
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]