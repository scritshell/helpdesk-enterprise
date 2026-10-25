# HelpDesk empresarial

Aplicación de gestión de incidencias para portfolio, construida como **monolito modular**.

## Stack

- Backend: Java 21, Spring Boot 3, Spring Security, Spring Data JPA, PostgreSQL, Maven, JUnit
- Frontend: Angular 21, TypeScript, Bootstrap, RxJS
- DevOps: Docker Compose, GitHub Actions

## Arranque rápido

### Requisitos

- Docker y Docker Compose
- Java 21 y Maven (solo si se ejecuta el backend fuera de Docker)
- Node.js compatible con Angular 21 (solo si se ejecuta el frontend fuera de Docker)

### Desarrollo con Docker

1. Crea un archivo local de entorno y genera un secreto JWT aleatorio:

   ```bash
   cp .env.example .env
   openssl rand -hex 32
   ```

2. Copia la salida del segundo comando en `JWT_SECRET` dentro de `.env`.
3. Para desarrollo local, `.env.example` activa explícitamente las cuentas demo (`APP_SEED_ENABLED=true`). Mantén esta opción en `false` en cualquier entorno publicado.
4. Arranca los servicios:

   ```bash
   docker compose up --build
   ```

Servicios locales:

- Frontend: `http://localhost:4200`
- Backend: `http://localhost:8080`
- PostgreSQL: `localhost:5432` (vinculado a loopback para no exponerlo directamente a la red).

El backend falla al arrancar si no se proporciona `JWT_SECRET`. El secreto debe ser aleatorio y tener al menos 32 bytes; la expiración predeterminada del JWT es de 30 minutos. No reutilices el `.env` de desarrollo en producción.

## Estructura

```text
helpdesk-enterprise/
├── backend/          # API REST y reglas de negocio
├── frontend/         # Aplicación Angular
├── docs/             # Decisiones y documentación técnica
├── docker-compose.yml
└── .github/workflows/ci.yml
```

## Principios

1. **Monolito modular antes que microservicios**: despliegue sencillo y límites internos explícitos.
2. **Package-by-feature**: cada módulo agrupa controlador, aplicación, dominio e infraestructura.
3. **Seguridad desde el inicio**: los endpoints nacen con una política de acceso definida.
4. **Auditoría como requisito funcional**: los cambios relevantes generan historial.
5. **Configuración por entorno**: secretos fuera del código y perfiles separados.

Consulta [docs/architecture.md](docs/architecture.md) para la explicación de las decisiones, [docs/phase2-auth.md](docs/phase2-auth.md) para usuarios, roles, JWT y Flyway, [docs/phase3-tickets.md](docs/phase3-tickets.md) para categorías y tickets, [docs/phase4-comments-audit-tests.md](docs/phase4-comments-audit-tests.md) para comentarios, auditoría y pruebas, y [docs/phase5-frontend-auth.md](docs/phase5-frontend-auth.md) para el login Angular y la gestión de sesión, además de [docs/phase5-security-hardening.md](docs/phase5-security-hardening.md) para los controles de seguridad de autenticación.

La administración de usuarios, categorías, estadísticas y adjuntos está documentada en [docs/phase7-admin-enterprise.md](docs/phase7-admin-enterprise.md). La auditoría de calidad, seguridad y preparación para portfolio se recoge en [docs/phase8-release-hardening.md](docs/phase8-release-hardening.md).
