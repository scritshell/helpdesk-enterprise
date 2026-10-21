# Fase 5 — Login frontend y sesión segura

## Objetivo

Conectar Angular con la autenticación existente del backend sin convertir el frontend en la autoridad de seguridad. El backend sigue validando credenciales, firma JWT, expiración y permisos.

## Flujo

```text
Formulario Angular
    ↓ POST /api/auth/login
Spring Security + BCrypt
    ↓
JWT con expiración
    ↓
sessionStorage durante la sesión del navegador
    ↓
Interceptor: Authorization: Bearer <token>
    ↓
GET /api/auth/me al arrancar la aplicación
```

## Decisiones de seguridad

- La contraseña solo existe en memoria mientras se envía el formulario; nunca se guarda en `localStorage`, `sessionStorage` ni en logs.
- El JWT se guarda en `sessionStorage`, no en `localStorage`, por lo que desaparece al cerrar la pestaña o la sesión del navegador.
- Al recibir `401`, el interceptor elimina la sesión local.
- El backend utiliza `STATELESS`; no se crea una sesión HTTP tradicional.
- CSRF está deshabilitado porque la autenticación se transporta mediante el header `Authorization`, no mediante una cookie de autenticación.
- CORS permite únicamente `APP_CORS_ALLOWED_ORIGIN`, por defecto `http://localhost:4200`.
- No se utiliza `Access-Control-Allow-Origin: *`.
- El frontend oculta rutas mediante guards, pero el backend continúa siendo la protección real.
- El mensaje de login fallido es genérico para no revelar si existe un correo concreto.
- Un endpoint protegido sin token responde `401 Unauthorized`; un usuario autenticado sin permisos responde `403 Forbidden`.

## Rutas frontend

```text
/login       pública
/dashboard   protegida por authGuard
```

El `AuthService` expone el usuario actual mediante una signal y ofrece `login`, `loadCurrentUser`, `logout`, `getToken` y `hasRole`.

## Configuración de API

En desarrollo:

```typescript
export const API_BASE_URL = 'http://localhost:8080/api';
```

Para producción debe sustituirse por una configuración de entorno o por un reverse proxy con HTTPS. No se deben incluir secretos en los archivos del frontend: todo lo que se compila en Angular es público.

## Pruebas funcionales realizadas

- Login válido: `200` y JWT Bearer presente.
- `/api/auth/me` sin token: `401`.
- `/api/auth/me` con JWT válido: `200`.
- Contraseña incorrecta: `401`.
- Preflight CORS desde `http://localhost:4200`: permitido.
- Preflight CORS desde un origen externo: rechazado.
- Las comprobaciones funcionales de la entrega inicial se deben volver a ejecutar después del endurecimiento. Consulta `phase5-security-hardening.md` para la batería actual de pruebas y sus límites.


## Controles de autenticación añadidos

- `JWT_SECRET` es obligatorio: no existe un secreto predeterminado en la configuración de producción. Debe generarse de forma aleatoria con al menos 32 bytes (`openssl rand -hex 32`). Los tests usan una clave independiente en `src/test/resources/application.yml`.
- La expiración por defecto es de 30 minutos y se puede configurar con `JWT_EXPIRATION_MINUTES`.
- Las cuentas demo se desactivan por defecto (`APP_SEED_ENABLED=false`). El archivo `.env.example` solo las activa para desarrollo local; no actives el seed en un entorno publicado.
- El login limita intentos fallidos: cinco intentos por combinación de cuenta/IP dentro de 15 minutos, además de un límite agregado de 20 intentos fallidos por IP en la misma ventana. La respuesta de bloqueo es `429 Too Many Requests` con `Retry-After`.
- El rate limiting se mantiene en memoria y sirve para una instancia. No es suficiente si se despliegan varias instancias o si se requiere persistir los límites tras reinicios; en esos casos debe usarse un almacenamiento compartido.
- Las peticiones toman la IP de `HttpServletRequest.getRemoteAddr()` y no confían directamente en `X-Forwarded-For`. Si se coloca un proxy delante, configura correctamente una cadena de proxies de confianza antes de interpretar cabeceras reenviadas.
- La clave de JWT se valida por longitud mínima de 32 bytes, y el parser rechaza firma inválida, tokens expirados o tokens sin `sub`/`exp`.

## Límites conocidos

`sessionStorage` no es inaccesible para JavaScript del mismo origen. Una vulnerabilidad XSS podría robar el token. El cierre de sesión elimina el token local, pero no revoca una copia robada: esa copia puede permanecer válida hasta su expiración. Para producción se recomienda valorar un BFF con cookie `HttpOnly; Secure; SameSite`, además de HTTPS, CSP, monitorización, gestión de secretos y pruebas de autorización para cada recurso.

El rate limiting implementado es un control defensivo de una sola instancia, no protección distribuida contra bots. El secreto JWT y las credenciales de base de datos deben gestionarse mediante configuración segura en cualquier despliegue publicado.
