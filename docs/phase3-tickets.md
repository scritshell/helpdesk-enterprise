# Fase 3 — Categorías y tickets

## Alcance

Esta fase añade la primera funcionalidad de negocio completa sobre la autenticación de la fase 2:

- categorías iniciales;
- creación y consulta de incidencias;
- estados y prioridades;
- asignación a técnicos;
- autorización por propietario, técnico asignado y administrador;
- migración Flyway `V2__create_categories_and_tickets.sql`;
- pruebas unitarias del dominio.

Los comentarios, adjuntos y auditoría detallada quedan para fases posteriores para mantener el alcance controlado.

## Modelo

```text
User 1 ─── N Ticket
Category 1 ─── N Ticket
User 1 ─── N Ticket como técnico asignado
```

Cada ticket contiene:

- título;
- descripción;
- categoría;
- estado;
- prioridad;
- usuario informador;
- técnico asignado opcional;
- fechas de creación y actualización.

## Estados

```text
OPEN → IN_PROGRESS → WAITING_USER → IN_PROGRESS → RESOLVED → CLOSED
```

En esta fase se persiste el estado y se permite modificarlo al técnico asignado o al administrador. La máquina de transiciones estricta y la auditoría se reforzarán en fases posteriores.

## Permisos actuales

| Acción | USER | TECHNICIAN | ADMIN |
|---|---:|---:|---:|
| Crear incidencia | Sí | Sí | Sí |
| Listar propias | Sí | — | — |
| Listar asignadas | — | Sí | — |
| Listar todas | — | — | Sí |
| Consultar propia/asignada | Sí | Sí | Sí |
| Cambiar estado/prioridad | No | Solo asignadas | Sí |
| Asignar técnico | No | Sí | Sí |
| Crear categoría | No | No | Sí |
| Listar categorías activas | Sí | Sí | Sí |

La protección se realiza en el caso de uso del backend, no solo en la interfaz.

## Endpoints

| Método | Ruta | Acceso |
|---|---|---|
| GET | `/api/categories` | Autenticado |
| POST | `/api/categories` | ADMIN |
| POST | `/api/tickets` | Autenticado |
| GET | `/api/tickets` | Según rol |
| GET | `/api/tickets/{id}` | Propietario, técnico asignado o ADMIN |
| PATCH | `/api/tickets/{id}/status` | Técnico asignado o ADMIN |
| PATCH | `/api/tickets/{id}/priority` | Técnico asignado o ADMIN |
| PATCH | `/api/tickets/{id}/assignment` | TECHNICIAN o ADMIN |

## Pruebas con curl

Obtener un token como usuario:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@helpdesk.local","password":"User123!"}' \
  | jq -r '.token')
```

Listar categorías:

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/categories
```

Crear un ticket usando la primera categoría:

```bash
curl -X POST http://localhost:8080/api/tickets \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"title":"No puedo acceder al correo","description":"El sistema muestra un error 403","categoryId":1,"priority":"HIGH"}'
```

Listar los tickets del usuario:

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/tickets
```

Un `USER` no puede cambiar el estado de un ticket:

```bash
curl -i -X PATCH http://localhost:8080/api/tickets/1/status \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"status":"IN_PROGRESS"}'
```

Debe responder `403 Forbidden` salvo que el usuario tenga permisos de técnico asignado o administrador.

## Flyway

La migración V2 crea:

```text
categories
tickets
```

También añade claves foráneas, índices y restricciones `CHECK` para que la base de datos no acepte estados o prioridades que no pertenezcan al dominio.

Nunca se modifica V1 después de aplicada. Si se cambia el esquema, se crea V3.

## Arranque

```bash
sudo docker compose up -d --build
```

Para probar las migraciones desde una base limpia durante esta fase inicial:

```bash
sudo docker compose down -v
sudo docker compose up -d --build
```

> `down -v` elimina los datos locales de PostgreSQL. No usar cuando haya información importante.
