# HelpDesk empresarial

Aplicación de gestión de incidencias para portfolio.

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

```bash
docker compose up --build
```

Servicios previstos:

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:4200`
- PostgreSQL: `localhost:5432`

> En esta primera base, el backend expone un endpoint de salud. Las funcionalidades de tickets se implementarán por iteraciones.

## Estructura

```text
helpdesk-enterprise/
├── backend/          # API REST y reglas de negocio
├── frontend/         # Aplicación Angular
├── docs/             # Decisiones y documentación técnica
├── docker-compose.yml
└── .github/workflows/ci.yml
```

## Nota

Consulta [docs/architecture.md](docs/architecture.md) para la explicación de las decisiones.
