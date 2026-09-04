# GymFlow

Plataforma SaaS multi-tenant para gimnasios pequeños y medianos: socios, membresías, pagos, asistencia y detección de clientes en riesgo de abandono.

Monorepo, Fases 0 a 5 completas. El análisis de arquitectura completo — multi-tenancy, roles, modelo de datos, roadmap de 19 fases — vive en el documento de Fase 0.

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

## Gimnasios y usuarios internos

```
POST  /api/v1/gyms              { name, slug }                    SUPER_ADMIN — alta de un gimnasio nuevo
GET   /api/v1/gyms/{id}                                           SUPER_ADMIN (cualquiera) o GYM_ADMIN (solo el propio)

POST  /api/v1/users             { email, password, role, gymId? } GYM_ADMIN (en su propio gym) o SUPER_ADMIN (con gymId — así se da de alta el primer GYM_ADMIN de un gimnasio nuevo)
GET   /api/v1/users                                               GYM_ADMIN — lista los usuarios de su propio gimnasio
PATCH /api/v1/users/{id}/disable                                  GYM_ADMIN — deshabilita un usuario de su propio gimnasio
```

`role` en `POST /api/v1/users` solo acepta `GYM_ADMIN` o `TRAINER` — `MEMBER` se da de alta en la Fase 6 (portal del socio), `SUPER_ADMIN` no se crea por API. El `gymId` del body solo lo usa un actor `SUPER_ADMIN`; si lo manda un `GYM_ADMIN` se ignora — su propio gimnasio (el del token) manda siempre, así no hay forma de crear ni tocar usuarios de otro gimnasio pasando un `gymId` distinto.

## Testing backend

```bash
cd backend
mvn test      # unit — rápido, sin Docker
mvn verify    # + integración (Testcontainers, necesita Docker corriendo)
```

## Multi-tenancy

Columna discriminadora (`gym_id`), no schema ni base por tenant — ver Fase 0. El aislamiento no depende de que cada query se acuerde de filtrar: toda entidad que extienda `AbstractTenantEntity` (`backend/.../shared/tenant/`) queda automáticamente restringida al tenant actual vía el mecanismo `@TenantId` de Hibernate. `TenantContext` (un `ThreadLocal`) lo puebla `JwtAuthenticationFilter` a partir del claim `gymId` del JWT — nunca de un parámetro de la request — y se limpia siempre al final de cada request.

Sin tenant resuelto (arranque de la app, o un `SUPER_ADMIN` sin `gymId`) el sistema **no muestra nada** a través de esas entidades, nunca "todo" — falla cerrado, no abierto. `User` es la única excepción deliberada: no extiende `AbstractTenantEntity` porque el login necesita poder buscar por email a través de todos los tenants antes de que exista una sesión.

## Estado

**Fase 5 — Gestión de gimnasios y usuarios.** `Gym` es la primera entidad de negocio real (raíz del tenant, no usa `AbstractTenantEntity` — es la unidad de aislamiento, no algo aislado). `app_user.gym_id` ya tiene su FK a `gym.id`. RBAC real con `@PreAuthorize` por rol. `User` sigue sin el mecanismo automático de la Fase 4 (necesita búsqueda cross-tenant en el login), así que su aislamiento por gimnasio en listados/bajas es manual — `UserRepository.findByGymId` — y quedó probado explícitamente, incluyendo que un `GYM_ADMIN` no puede escapar a otro gimnasio ni pasando un `gymId` distinto en el body. Ver el roadmap completo (Fase 0 a 18) en el documento de arquitectura.
