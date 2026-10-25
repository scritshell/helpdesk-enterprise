# Fase 7 — Administración y funcionalidades empresariales

## Usuarios y roles

`GET /api/users` y `PATCH /api/users/{id}` están restringidos a `ADMIN`. Las respuestas contienen únicamente identificador, correo, nombre, roles, estado y fecha de alta; nunca incluyen contraseña, hash ni tokens. El servicio impide retirar el rol `ADMIN` o desactivar al último administrador activo. La desactivación conserva las incidencias y el historial asociados.

## Categorías

Las categorías siguen siendo la tabla existente `categories`; no se creó una segunda fuente de verdad. `GET /api/categories/manage`, `POST /api/categories`, `PATCH /api/categories/{id}` y `PATCH /api/categories/{id}/active` requieren `ADMIN`. Los nombres se recortan, se validan y se comparan sin distinguir mayúsculas. La desactivación es lógica para conservar referencias históricas.

## Estadísticas

`GET /api/statistics?days=30` requiere `ADMIN` y calcula los totales directamente mediante consultas agrupadas sobre PostgreSQL. El periodo se limita a 1–365 días. La evolución usa tickets creados y cambios auditados a `RESOLVED` y `CLOSED`; no depende de la página cargada en el navegador.

## Adjuntos

La migración `V4__create_attachments.sql` añade metadatos en PostgreSQL y mantiene la relación con ticket y usuario. Los archivos se guardan fuera del frontend, con UUID interno, nombre original saneado, validación de extensión/tipo y cabecera para PDF/PNG/JPEG. Las descargas requieren autenticación y autorización sobre el ticket, y la ruta comprueba que el identificador del adjunto pertenece al ticket solicitado.

Configuración:

- `ATTACHMENTS_STORAGE_PATH`: directorio de almacenamiento; en Compose se monta el volumen `attachments_data` en `/var/lib/helpdesk/attachments`.
- `ATTACHMENTS_MAX_SIZE`: límite de Spring Multipart, por defecto `10MB`.
- `ATTACHMENTS_MAX_SIZE_BYTES`: límite de negocio, por defecto `10485760`.

Los adjuntos no se sirven mediante URLs públicas. El volumen debe tener permisos de lectura/escritura para el usuario del proceso backend en el entorno de despliegue.

## Validación

Con PostgreSQL disponible, ejecutar:

```bash
mvn -B verify
cd frontend && npm test -- --watch=false && npm run build && npm audit
```

Para comprobar una ruta Angular directamente, arranca Compose y visita `http://localhost:4200/app/tickets/1` en una pestaña nueva o tras refrescar. Nginx debe devolver `index.html` para la ruta de aplicación, mientras que `/api/...` continúa devolviendo la respuesta real del backend.
