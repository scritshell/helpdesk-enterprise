# Fase 4 — Comentarios, auditoría, transiciones y pruebas

## Funcionalidades

Esta fase añade:

- comentarios asociados a tickets;
- historial inmutable de acciones relevantes;
- transiciones de estado explícitas;
- autorización para leer y modificar tickets;
- endpoints para comentarios e historial;
- migración Flyway V3 compatible con esquemas que ya tienen V1 y V2;
- pruebas de dominio y de servicio para autorización, comentarios y auditoría;
- ejecutor de pruebas Vitest oficial de Angular 21;
- pruebas reales del componente raíz del frontend;
- CI ejecutando `npm ci`, `npm test` y `npm run build`.

## Transiciones

```text
OPEN → IN_PROGRESS
IN_PROGRESS → WAITING_USER | RESOLVED
WAITING_USER → IN_PROGRESS
RESOLVED → CLOSED | IN_PROGRESS
CLOSED → (ninguna)
```

Una transición no permitida responde con `409 Conflict`. La regla está en el dominio, por lo que no depende del controlador ni de la interfaz.

## Comentarios y auditoría

Las tablas nuevas son:

```text
ticket_comments
ticket_history
```

Se registran como mínimo:

- creación de ticket;
- cambio de estado;
- cambio de prioridad;
- asignación de técnico;
- comentario añadido.

El historial no expone operaciones de edición o borrado. Los comentarios tampoco se editan en esta fase, reduciendo el riesgo de alterar la trazabilidad.

## Endpoints

```text
POST /api/tickets/{id}/comments
GET  /api/tickets/{id}/comments
GET  /api/tickets/{id}/history
```

El usuario propietario, el técnico asignado y el administrador pueden consultar y comentar. Solo el técnico asignado o el administrador pueden modificar estado y prioridad.

## Migración sin borrar datos

`V3__create_comments_and_ticket_history.sql` solo crea tablas, índices y restricciones nuevas. No modifica ni elimina V1 o V2 y no requiere `docker compose down -v`.

Para probar sobre una base existente:

```bash
sudo docker compose up -d --build
sudo docker compose logs -f backend
```

Flyway debe detectar V1 y V2 como aplicadas y ejecutar únicamente V3.

## Angular 21

Se reemplazó `@angular-devkit/build-angular` por `@angular/build`, manteniendo Angular 21. El objetivo de pruebas usa:

```json
"builder": "@angular/build:unit-test",
"runner": "vitest",
"watch": false
```

El script CI es:

```bash
npm ci
npm test
npm run build
```

Las pruebas comprueban que el componente raíz se crea y que presenta el título y el texto de la aplicación.

## Vulnerabilidades npm

La auditoría inicial registraba 9 vulnerabilidades: 7 altas y 2 moderadas, todas dentro del árbol de desarrollo de `@angular-devkit/build-angular` y su cadena Webpack (`webpack-dev-server`, `chokidar`, `braces`, `micromatch`, `http-proxy-middleware`, `sockjs` y `uuid`).

La corrección compatible aplicada es migrar al builder moderno `@angular/build` de la misma versión mayor 21, que utiliza Vite y no arrastra esa cadena vulnerable. No se ejecutó `npm audit fix --force` y no se actualizó Angular a otra versión principal.

El resultado real después de `npm ci` y de la migración fue:

```bash
npm audit --omit=optional
```

Resultado: `0` vulnerabilidades en todas las severidades. Durante la instalación aparece un aviso de deprecación de `@angular/animations@21.2.25`, pero no es una vulnerabilidad de seguridad y no justifica cambiar de versión principal en esta fase.
