# Node 22, no 20: pnpm 11 usa `node:sqlite`, que es built-in recién desde
# 22.5 — con Node 20 ni siquiera arranca. Es la misma versión que usa CI.
FROM node:22-alpine
WORKDIR /app

# El proyecto usa pnpm, no npm. El Dockerfile anterior hacía `npm install`
# tras copiar sólo `package*.json`, que NO incluye `pnpm-lock.yaml`: npm
# resolvía el árbol entero de cero, ignorando el lockfile, y terminaba
# fallando con "Cannot read properties of null (reading 'edgesOut')". El
# build de Render venía roto por eso desde el 10/10.
#
# La versión de pnpm sale del campo `packageManager` de package.json, que es
# la única fuente de verdad: CI usa ese mismo campo.
ENV COREPACK_ENABLE_DOWNLOAD_PROMPT=0
RUN corepack enable

# El lockfile se copia junto al manifiesto y la instalación es `--frozen-lockfile`:
# si el lockfile no concuerda, el build falla en vez de resolver otra cosa.
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY . .

EXPOSE 5173
# --host 0.0.0.0 es obligatorio: sin esto, Vite solo escucha en localhost
# DENTRO del contenedor y no vas a poder acceder desde tu navegador.
# PORT lo define Render en runtime; en Compose local no está seteada,
# así que cae al default 5173 (igual que antes).
# `pnpm exec vite` y no `pnpm run dev -- ...`: a diferencia de npm, pnpm no
# consume el `--`, se lo pasa literal al script y Vite ignora todo lo que
# viene detrás. El síntoma es traicionero — la imagen construye, el
# contenedor arranca, y Vite queda escuchando en localhost:5173 sin que el
# puerto de Render reciba nada.
CMD ["sh", "-c", "pnpm exec vite --host 0.0.0.0 --port ${PORT:-5173}"]
