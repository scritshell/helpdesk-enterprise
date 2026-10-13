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

## Principios

1. **Monolito modular antes que microservicios**: despliegue sencillo y límites internos explícitos.
2. **Package-by-feature**: cada módulo agrupa controlador, aplicación, dominio e infraestructura.
3. **Seguridad desde el inicio**: los endpoints nacen con una política de acceso definida.
4. **Auditoría como requisito funcional**: los cambios relevantes generan historial.
5. **Configuración por entorno**: secretos fuera del código y perfiles separados.

Consulta [docs/architecture.md](docs/architecture.md) para la explicación de las decisiones y [docs/phase2-auth.md](docs/phase2-auth.md) para usuarios, roles, JWT y Flyway.
