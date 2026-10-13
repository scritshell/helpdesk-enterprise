# Fase 2 — Usuarios, roles, BCrypt, JWT y Flyway

## Qué se ha implementado

- Entidades `UserAccount` y `Role`.
- Roles `USER`, `TECHNICIAN` y `ADMIN`.
- Migración `V1__create_users_and_roles.sql`.
- Contraseñas cifradas con BCrypt.
- Login `POST /api/auth/login`.
- Tokens JWT firmados y con expiración.
- Filtro que valida el JWT en peticiones protegidas.
- Endpoint `GET /api/auth/me` para consultar el usuario autenticado.
- Usuarios de desarrollo creados automáticamente una sola vez.
- JPA en modo `validate`: Hibernate ya no modifica el esquema.

## Flyway

Flyway ejecuta las migraciones al arrancar el backend. Las migraciones viven en:

```text
backend/src/main/resources/db/migration/
```

La primera migración es:

```text
V1__create_users_and_roles.sql
```

La regla de nombres es:

```text
V<versión>__<descripción>.sql
```

Ejemplos futuros:

```text
V2__create_categories.sql
V3__create_tickets.sql
V4__create_ticket_comments.sql
```

Flyway crea la tabla `flyway_schema_history` y registra qué migraciones ya se ejecutaron. Por eso una migración aplicada no debe editarse: se crea otra versión nueva.

## Usuarios de desarrollo

Con `APP_SEED_ENABLED=true` —valor por defecto local— se crean estos usuarios si no existen:

| Email | Contraseña | Rol |
|---|---|---|
| `user@helpdesk.local` | `User123!` | `USER` |
| `technician@helpdesk.local` | `Technician123!` | `TECHNICIAN` |
| `admin@helpdesk.local` | `Admin123!` | `ADMIN` |

Las contraseñas se cifran con BCrypt antes de guardarse. Estas credenciales son solo para desarrollo y no deben utilizarse en producción.

Para desactivar la creación automática:

```bash
APP_SEED_ENABLED=false sudo -E docker compose up -d --build
```

## Arranque en CachyOS

Desde el directorio del proyecto:

```bash
cd /home/scritshell/Documentos/GitHub/helpdesk-enterprise
sudo docker compose up -d --build
```

Si se está usando un volumen anterior y quieres probar la migración desde cero:

```bash
sudo docker compose down -v
sudo docker compose up -d --build
```

> `down -v` borra los datos de PostgreSQL. Úsalo únicamente si no necesitas conservar la base de datos local.

## Probar el login

```bash
curl -i -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"user@helpdesk.local","password":"User123!"}'
```

La respuesta será similar a:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "email": "user@helpdesk.local",
    "firstName": "Usuario",
    "lastName": "Demo",
    "roles": ["USER"]
  }
}
```

Guarda el valor de `token` y úsalo sin el prefijo `Bearer` porque la respuesta ya lo separa en `tokenType`:

```bash
TOKEN='pega-aqui-el-token'
curl -i http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

Sin token, `/api/auth/me` responde `401 Unauthorized`.

## Qué ocurre internamente

```text
POST /api/auth/login
        ↓
AuthenticationManager
        ↓
UserAccountDetailsService consulta users
        ↓
BCrypt compara la contraseña
        ↓
JwtService firma el token
        ↓
Respuesta JSON
```

En una petición posterior:

```text
Authorization: Bearer <token>
        ↓
JwtAuthenticationFilter
        ↓
Comprueba firma y expiración
        ↓
Carga usuario y roles
        ↓
SecurityContext
        ↓
Endpoint protegido
```

## Por qué usamos BCrypt

Nunca guardamos la contraseña original. BCrypt genera un hash con salt y coste configurable. Dos usuarios con la misma contraseña no tienen por qué producir el mismo hash.

La aplicación solo puede comprobar:

```text
BCrypt.matches(contraseñaIntroducida, hashGuardado)
```

## Por qué usamos JWT stateless

El backend no guarda una sesión HTTP tradicional. Cada petición protegida lleva su token y el servidor valida su firma.

Esto simplifica el despliegue del backend y encaja bien con una API REST. El token tiene una expiración de 120 minutos por defecto, configurable con:

```text
JWT_EXPIRATION_MINUTES
```

El secreto se configura con:

```text
JWT_SECRET
```

En producción debe ser un secreto largo, aleatorio y gestionado fuera del repositorio.

## Decisiones importantes

- La ruta pública es solo `/api/auth/login`, no todo `/api/auth/**`.
- `/api/auth/me` requiere JWT.
- La sesión es `STATELESS`.
- `ddl-auto` cambió de `update` a `validate`.
- Flyway es la única herramienta que crea y modifica el esquema.
- Los usuarios de desarrollo se insertan desde Java para poder generar BCrypt correctamente.
- Los datos de desarrollo se pueden desactivar con `APP_SEED_ENABLED=false`.
