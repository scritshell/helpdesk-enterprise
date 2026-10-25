# Fase 8 — calidad, seguridad y preparación para portfolio

## Alcance

Esta fase consolida la calidad, la seguridad, la documentación y la reproducibilidad del proyecto antes de su publicación.

## Auditoría y correcciones

- Se mantiene la autorización en los casos de uso backend; las rutas Angular no se consideran una frontera de seguridad.
- Se añadió un manejador REST común para validaciones, errores esperados, archivos demasiado grandes y errores inesperados. Las respuestas de servidor no exponen trazas, rutas internas ni causas.
- Spring se configura para no incluir mensajes, trazas ni errores de binding en su respuesta de fallback.
- Nginx añade `nosniff`, `DENY`, `Referrer-Policy` y una CSP compatible con el bundle actual. La CSP local permite conectar con `http://localhost:8080`; en un despliegue real debe sustituirse por el origen HTTPS real de la API.
- La compilación de producción desactiva la inserción automática de CSS crítico inline (`inlineCritical: false`). Angular generaba un `onload="this.media='all'"` en `index.html`; ese manejador violaba `script-src 'self'` y podía dejar la carga visual en un estado incompleto. Las plantillas y componentes no contienen manejadores HTML inline.
- JWT sigue siendo obligatorio, stateless y con secreto externo obligatorio. El rate limiting continúa siendo local al proceso y el logout elimina el token del navegador, pero no revoca copias ya obtenidas. Son limitaciones conocidas que requieren Redis o un BFF con cookie `HttpOnly` si el sistema se publica a escala.

## Revisión de datos sensibles

Las respuestas administrativas y de adjuntos no devuelven hashes BCrypt, tokens ni claves internas de almacenamiento. Las descargas requieren autorización sobre la incidencia. Los archivos se guardan en el volumen privado `attachments_data`, fuera del frontend.

## Validación reproducible

```bash
cp .env.example .env
openssl rand -hex 32
# Copiar el valor generado en JWT_SECRET dentro de .env
docker compose config --quiet
docker compose up -d --build

cd backend && mvn -B verify
cd ../frontend && npm ci && npm test && npm run build
npm audit --audit-level=moderate
```

No se deben publicar `.env`, tokens ni datos de la base local. No se deben eliminar volúmenes para validar una actualización normal; para probar migraciones desde cero se debe usar un proyecto Compose aislado.

## Comprobaciones manuales pendientes

- Login, logout y expiración de token con los tres roles.
- Acceso horizontal a tickets y adjuntos cambiando los identificadores en las rutas.
- Rechazo de cambios administrativos con `USER` y `TECHNICIAN`.
- Recarga directa de `/app/tickets/1` y comprobación de que `/api/...` no se redirige a `index.html`.
- Subida de archivos permitidos, rechazados, sobredimensionados y con contenido incompatible.
- Revisión visual responsive y lectura con teclado en las pantallas principales.

## Decisiones pendientes de despliegue público

HTTPS, un gestor de secretos, rate limiting compartido, revocación de sesiones, cookies `HttpOnly` mediante un BFF y una CSP parametrizada por entorno son requisitos adicionales para un despliegue público. No se han simulado como resueltos en esta fase.
