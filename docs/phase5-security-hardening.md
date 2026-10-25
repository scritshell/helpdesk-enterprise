# Fase 5 — endurecimiento de autenticación

## Controles implementados

- **Secreto JWT obligatorio:** `JWT_SECRET` no tiene valor por defecto. `JwtService` rechaza secretos de menos de 32 bytes y duraciones JWT no positivas. Usa una clave de prueba aislada en `src/test/resources/application.yml`.
- **Caducidad JWT:** 30 minutos por defecto, configurable con `JWT_EXPIRATION_MINUTES`.
- **Seed demo seguro por defecto:** `APP_SEED_ENABLED=false` en la configuración principal. `.env.example` lo habilita únicamente para facilitar el desarrollo local; debe estar desactivado en entornos publicados. Cambiar la bandera no elimina cuentas demo ya existentes en la base de datos.
- **Rate limiting de login en memoria:** cinco fallos por combinación cuenta/IP en 15 minutos y límite agregado de 20 fallos por IP en la misma ventana. La respuesta bloqueada es `429`, incluye `Retry-After` y un mensaje genérico. Un login correcto limpia el contador de la combinación cuenta/IP; el agregado por IP no se limpia con un login correcto, para no permitir que se reinicie un contador de password spraying al acceder a otra cuenta.
- **Validación JWT:** pruebas para firmas de otra clave, firma manipulada y token expirado; exige `sub` y `exp`.
- **Pruebas HTTP de seguridad:** el endpoint `/api/auth/me` exige token; credenciales incorrectas reciben `401` genérico; CORS permite el origen configurado y rechaza otro; el rate limit devuelve `429` con `Retry-After`.
- **Configuración local:** Compose exige `JWT_SECRET`, usa 30 minutos por defecto y no activa las cuentas demo si `APP_SEED_ENABLED` no se define. Los puertos del host están vinculados a `127.0.0.1` para limitar su exposición local.

## Preparar entorno local

```bash
test -f .env || cp .env.example .env
openssl rand -hex 32
```

Copia el valor generado a `JWT_SECRET` en `.env` y mantén ese archivo fuera del repositorio. El ejemplo activa `APP_SEED_ENABLED=true` solo para desarrollo local con las cuentas de demostración. Antes de un despliegue publicado, establece `APP_SEED_ENABLED=false`, usa credenciales de base de datos propias y secretos gestionados fuera del repositorio.

## Límites conocidos

- El rate limiter vive en memoria: se pierde al reiniciar y no se comparte entre instancias. Para despliegues con varias réplicas, migra el control a un almacén compartido (por ejemplo, Redis) y establece límites operativos adecuados.
- Se usa `getRemoteAddr()` como IP del cliente para no confiar en una cabecera `X-Forwarded-For` controlada por el cliente. Si hay reverse proxy, configura únicamente proxies de confianza.
- El JWT en `sessionStorage` es accesible a JavaScript del mismo origen. Una vulnerabilidad XSS podría exfiltrarlo.
- El logout elimina la copia del navegador, pero no revoca un JWT previamente copiado; seguirá siendo válido hasta expirar. El siguiente paso arquitectónico podría ser un BFF con cookie `HttpOnly`, `Secure`, `SameSite`, protección CSRF y revocación de sesión.
- Para Internet, HTTPS, CSP y cabeceras de seguridad, gestión de secretos, revisión de permisos por recurso, logging/monitorización y auditoría regular siguen siendo necesarios.

## Validación

Ejecutar con PostgreSQL disponible (el test de contexto de Spring carga JPA/Flyway):

```bash
cd backend
mvn -B verify
cd ../frontend
npm ci
npm test
npm run build
npm audit
```

La suite debe ejecutarse con PostgreSQL disponible para validar el contexto completo de Spring, JPA y Flyway.
