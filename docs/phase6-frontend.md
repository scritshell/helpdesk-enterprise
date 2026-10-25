# Fase 6 — paneles y flujo de incidencias

## Alcance implementado

El frontend Angular usa el sistema visual Clear Workspace y se conecta a los endpoints reales de autenticación, categorías, tickets, comentarios e historial. Incluye:

- layout responsive con sidebar, topbar y navegación contextual por rol;
- dashboard con métricas calculadas a partir de la lista autorizada por el backend;
- listado con búsqueda, filtros de estado/prioridad/categoría, ordenación y paginación en cliente;
- creación de incidencias;
- detalle con comentarios e historial;
- cambios de estado, prioridad y asignación con manejo de `401`, `403`, `404` y `409`;
- estados de carga, skeleton, vacío, error y reintento;
- servicios HTTP y modelos TypeScript reutilizables.

## Endpoint añadido

`GET /api/users/technicians` devuelve técnicos para el selector de asignación. Solo está disponible para `TECHNICIAN` y `ADMIN`, y tiene una prueba de integración de autorización.

La cola técnica usa dos casos de uso separados:

- `GET /api/tickets/queue`: incidencias sin técnico, disponible para `TECHNICIAN` y `ADMIN`.
- `POST /api/tickets/{id}/claim`: un técnico reclama una incidencia sin asignar para sí mismo.
- `PATCH /api/tickets/{id}/assignment`: asignación o reasignación a otro técnico, únicamente `ADMIN`.

`GET /api/tickets` continúa devolviendo solo las incidencias propias del usuario, las asignadas al técnico o todas para ADMIN. No existe una vista global para técnicos.

Los tickets `RESOLVED` muestran «Cerrar incidencia» y utilizan la transición de dominio existente hacia `CLOSED`. El backend continúa rechazando transiciones inválidas con `409` y operaciones no autorizadas con `403`.

Las funcionalidades empresariales posteriores —administración, estadísticas y adjuntos— están documentadas en `phase7-admin-enterprise.md`.

## Validación ejecutada

Desde `backend`:

```bash
mvn -B verify
```

Resultado: correcto; 33 pruebas, 0 fallos y 0 errores. PostgreSQL local y Flyway validaron las migraciones V1, V2 y V3.

Desde `frontend`:

```bash
npm test
npm run build
npm audit --omit=optional
```

Resultados reales: 4 archivos de pruebas y 5 pruebas correctas; build de producción correcto; 0 vulnerabilidades npm.

## Rutas Angular en Docker

El frontend copia `frontend/nginx.conf`, cuya directiva `try_files` devuelve `index.html` para rutas que no son archivos estáticos. Así, una recarga directa de `/app/tickets/1` o `/tickets/1` llega al router Angular; las rutas `/api/...` siguen perteneciendo al backend en `localhost:8080` y no se ocultan ni se redirigen por Nginx.

Para comprobarlo con Docker, con `JWT_SECRET` definido en `.env`:

```bash
docker compose up -d --build
curl -i http://localhost:4200/app/tickets/1
curl -i http://localhost:4200/tickets/1
curl -i http://localhost:8080/api/health
```

Las dos primeras respuestas deben ser `200` y contener la aplicación Angular. La última debe ser la respuesta JSON real del backend. Después de una sesión válida, la navegación/recarga del detalle se prueba desde el navegador; un `403` o `404` de la API debe seguir mostrándose como error de aplicación.
