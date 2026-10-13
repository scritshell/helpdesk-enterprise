# Arquitectura inicial y decisiones

## 1. Por qué un monolito modular

El proyecto tendrá un único backend desplegable, pero dividido en módulos con responsabilidades claras. Para un proyecto junior esto demuestra más criterio que crear varios servicios artificiales:

- menos complejidad operativa;
- transacciones y consistencia más sencillas;
- una sola base de datos y un único pipeline;
- posibilidad de extraer un módulo en el futuro si existe una razón real.

**Monolito no significa código desordenado**. Los límites se mantienen mediante paquetes, dependencias y reglas de acceso.

## 2. Organización por funcionalidad

Se usará `package-by-feature` en lugar de agrupar todo por tipo técnico:

```text
com.portfolio.helpdesk
├── shared/       # errores, respuesta API, configuración común
├── auth/         # login, JWT y usuario autenticado
├── ticket/       # incidencias, estados, prioridades y workflow
├── comment/      # comentarios y respuestas
├── category/     # categorías administrables
├── user/         # usuarios, roles y administración
└── audit/        # historial de cambios
```

**Por qué:** cuando se trabaja en una funcionalidad, sus clases están juntas. Así se reduce el acoplamiento accidental y la estructura comunica el negocio.

## 3. Capas dentro de cada módulo

Cada módulo podrá evolucionar hacia esta forma:

```text
 ticket/
 ├── api/            # controllers y DTOs HTTP
 ├── application/    # casos de uso
 ├── domain/         # entidades, enums y reglas
 └── infrastructure/ # repositorios JPA y adaptadores
```

El controlador no decidirá reglas de negocio. Recibirá HTTP, validará la entrada básica y delegará en un caso de uso.

## 4. Seguridad

Flujo previsto:

```text
credenciales → /api/auth/login → JWT → filtro Spring Security → roles → autorización
```

- `USER`: crea y consulta sus incidencias, responde cuando se solicita información.
- `TECHNICIAN`: gestiona incidencias asignadas, cambia prioridad/estado y comenta.
- `ADMIN`: administra usuarios, categorías y configuración.

La seguridad se comprobará en dos niveles: autorización HTTP y reglas dentro del caso de uso. Esto evita confiar únicamente en que una ruta esté oculta en el frontend.

## 5. Modelo de datos inicial

Entidades principales:

- `users`, `roles`, `user_roles`
- `tickets`
- `ticket_comments`
- `categories`
- `attachments`
- `ticket_history`

`ticket_history` será append-only: no se sobreescriben los hechos anteriores. Cada registro guardará actor, acción, fecha y valores relevantes. Esto permite explicar quién hizo qué y cuándo.

## 6. API inicial

| Método | Ruta | Objetivo | Acceso inicial |
|---|---|---|---|
| GET | `/api/health` | Comprobar disponibilidad | Público |
| POST | `/api/auth/login` | Obtener JWT | Público |
| GET | `/api/tickets` | Listar según rol | Autenticado |
| POST | `/api/tickets` | Crear incidencia | USER |
| GET | `/api/tickets/{id}` | Ver detalle autorizado | Autenticado |
| PATCH | `/api/tickets/{id}/status` | Cambiar estado | TECHNICIAN/ADMIN |
| POST | `/api/tickets/{id}/comments` | Añadir comentario | Autenticado según incidencia |

Las rutas son un contrato; los DTOs evitarán exponer directamente entidades JPA.

## 7. Decisiones deliberadamente aplazadas

- almacenamiento real de archivos: inicialmente se dejará una abstracción para poder usar filesystem local en desarrollo y S3/MinIO después;
- refresh tokens y revocación avanzada;
- notificaciones por correo;
- despliegue cloud;
- extracción a microservicios.

Aplazar no significa ignorar: significa no añadir complejidad antes de tener un caso de uso que la justifique.
