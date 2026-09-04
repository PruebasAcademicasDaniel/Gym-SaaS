# GymFlow

Plataforma SaaS multi-tenant para gimnasios pequeños y medianos: socios, membresías, pagos, asistencia y detección de clientes en riesgo de abandono.

Monorepo en Fase 1 (inicialización). El análisis de arquitectura completo — multi-tenancy, roles, modelo de datos, roadmap de 19 fases — vive en el documento de Fase 0.

## Estructura

```
backend/    Spring Boot 4 (Java 21) — monolito modular, un paquete por dominio
frontend/   React 19 + TypeScript + Vite + Tailwind CSS 4
```

Cada módulo del backend (`auth`, `gym`, `user`, `member`, `plan`, `membership`, `payment`,
`attendance`, `risk`, `notification`, `dashboard`, `audit`) vive bajo `backend/src/main/java/com/gymflow/`.
Un módulo nunca importa clases de infraestructura de otro módulo.

## Requisitos

- Docker + Docker Compose (forma recomendada de correr todo)
- Java 21 y Maven 3.9+ (si corrés el backend fuera de Docker)
- Node 20+ / npm 10+ (si corrés el frontend fuera de Docker)

## Desarrollo local — todo junto

```bash
docker compose up
```

Levanta backend (`:8080`), Postgres (`:5433` en el host — ver nota abajo) y frontend (`:5173`) con un solo comando. Al bootear, el backend corre las migraciones de Flyway automáticamente contra Postgres.

> **Nota de puerto:** Postgres del contenedor se publica en `5433`, no `5432`, porque en muchas máquinas de desarrollo ya hay un PostgreSQL nativo escuchando en `5432`. La comunicación interna backend↔db (dentro de la red de Docker) sigue usando el `5432` normal; el `5433` es solo para conectarte desde el host (por ejemplo con un cliente SQL). Se puede cambiar con la variable `DB_HOST_PORT`.

## Desarrollo local — servicios sueltos

Backend (necesita Postgres accesible en `localhost:5433`, por ejemplo con `docker compose up db`):

```bash
cd backend
mvn spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

La app queda en `http://localhost:5173`. El proxy de Vite reenvía `/api/*` a `http://localhost:8080`.

## Variables de entorno

Copiar `.env.example` a `.env` en la raíz (usado por `docker-compose.yml` y por el backend) y `frontend/.env.example` a `frontend/.env`. Ningún `.env` real se versiona.

## Autenticación

JWT stateless: access token corto (15 min) + refresh token opaco rotado en cada uso (7 días), ambos devueltos por `/api/v1/auth/login`. Endpoints:

```
POST /api/v1/auth/login    { email, password } -> { accessToken, refreshToken, tokenType, expiresIn }
POST /api/v1/auth/refresh  { refreshToken }     -> nuevo par de tokens; el refresh usado queda revocado
POST /api/v1/auth/logout   { refreshToken }     -> revoca el refresh token
GET  /api/v1/auth/me       (Bearer token)       -> usuario autenticado actual
```

El resto de los endpoints requieren `Authorization: Bearer <accessToken>`. Con el perfil `dev` activo (por defecto en `docker compose up`) existe un usuario de prueba: `admin@gymflow.dev` / `GymFlow!Dev2026` (`SUPER_ADMIN`) — ver `backend/src/main/resources/db/migration-dev/`. Ese seed nunca corre fuera del perfil `dev`.

## Testing backend

```bash
cd backend
mvn test      # unit — rápido, sin Docker
mvn verify    # + integración (Testcontainers, necesita Docker corriendo)
```

## Estado

**Fase 3 — Seguridad y autenticación.** Spring Security stateless con JWT propio (sin Keycloak todavía), BCrypt, rotación de refresh token, CORS restringido, errores homogéneos (RFC 7807) y auditoría mínima de login. Multi-tenancy real (`TenantContext`, aislamiento por `tenant_id`) llega en la Fase 4 — hoy `gym_id` es solo una columna en `app_user`, todavía sin filtro automático. Ver el roadmap completo (Fase 0 a 18) en el documento de arquitectura.
