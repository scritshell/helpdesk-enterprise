# Teoría del proyecto HelpDesk empresarial

## 1. Qué estamos construyendo

HelpDesk es una aplicación web empresarial para registrar, asignar, seguir y resolver incidencias internas.

Una incidencia representa un problema comunicado por un usuario, por ejemplo:

> "No puedo acceder al sistema de nóminas."

La aplicación debe permitir que:

- un usuario cree y consulte sus incidencias;
- un técnico las analice, priorice, asigne y resuelva;
- un administrador gestione usuarios, técnicos, categorías y estadísticas;
- el sistema conserve un historial de los cambios importantes.

La diferencia respecto a un CRUD simple es que aquí existe **flujo de negocio**: permisos, estados, responsables, auditoría y reglas.

---

## 2. Arquitectura general

La aplicación se divide en tres partes:

```text
Navegador
    ↓ HTTP/JSON
Frontend Angular
    ↓ API REST
Backend Spring Boot
    ↓ SQL/JPA
PostgreSQL
```

### Frontend

Es la interfaz que utiliza la persona. Muestra formularios, tablas, filtros, mensajes y paneles según el rol del usuario.

### Backend

Es el núcleo de la aplicación. Comprueba la identidad, los permisos y las reglas de negocio. No debemos confiar en el frontend para proteger datos.

### Base de datos

Guarda usuarios, incidencias, comentarios, categorías, adjuntos e historial.

---

## 3. Por qué usamos un monolito modular

Un monolito es una aplicación backend que se despliega como una unidad. Modular significa que internamente está separada por responsabilidades.

No es lo mismo que tener todo mezclado:

```text
Monolito desordenado:
controllers/
services/
repositories/
entities/
```

En ese diseño, las clases de una misma funcionalidad están repartidas por todo el proyecto.

Nuestra idea es usar **package-by-feature**:

```text
com.portfolio.helpdesk
├── auth/
├── ticket/
├── comment/
├── category/
├── user/
├── audit/
└── shared/
```

Esto demuestra que sabemos separar módulos sin pagar todavía el coste de los microservicios:

- un solo despliegue;
- una sola base de datos;
- transacciones más sencillas;
- menos configuración y observabilidad que mantener;
- límites internos claros.

Los microservicios solo tendrían sentido si existiera una necesidad real: equipos independientes, escalado diferente, despliegues separados o requisitos técnicos distintos.

---

## 4. Backend: Java y Spring Boot

### Java 21

Java es el lenguaje principal del backend. La versión 21 es una versión LTS, es decir, una versión con soporte prolongado y adecuada para proyectos profesionales.

### Spring Boot

Spring Boot simplifica la creación de una aplicación Spring:

- configura el servidor HTTP embebido;
- descubre componentes automáticamente;
- gestiona dependencias;
- proporciona integración con seguridad, persistencia y pruebas.

La clase `HelpdeskApplication` contiene el punto de entrada:

```java
@SpringBootApplication
public class HelpdeskApplication {
    public static void main(String[] args) {
        SpringApplication.run(HelpdeskApplication.class, args);
    }
}
```

`@SpringBootApplication` combina configuración, descubrimiento de componentes y configuración automática.

### Maven

Maven es la herramienta de construcción del backend. El archivo `pom.xml` define:

- nombre y versión del proyecto;
- versión de Java;
- dependencias;
- plugins de compilación y empaquetado.

Las dependencias iniciales son:

- `spring-boot-starter-web`: controladores REST y servidor HTTP;
- `spring-boot-starter-security`: autenticación y autorización;
- `spring-boot-starter-data-jpa`: persistencia con JPA/Hibernate;
- `postgresql`: driver JDBC de PostgreSQL;
- `spring-boot-starter-validation`: validación de DTOs;
- `spring-boot-starter-test`: pruebas;
- `spring-boot-starter-actuator`: endpoints técnicos de salud y métricas.

---

## 5. API REST

REST es un estilo para exponer recursos mediante HTTP.

Ejemplos previstos:

```http
POST /api/auth/login
GET  /api/tickets
POST /api/tickets
GET  /api/tickets/152
PATCH /api/tickets/152/status
POST /api/tickets/152/comments
```

### Verbos HTTP

| Verbo | Uso habitual |
|---|---|
| GET | Consultar datos |
| POST | Crear un recurso o ejecutar una acción |
| PUT | Reemplazar un recurso completo |
| PATCH | Modificar una parte |
| DELETE | Eliminar un recurso |

### JSON

El frontend y el backend se comunicarán mediante JSON:

```json
{
  "title": "No puedo acceder al correo",
  "description": "El sistema muestra un error 403",
  "priority": "HIGH",
  "categoryId": 3
}
```

### DTOs

Un DTO es un objeto diseñado para transportar datos por la API. No debemos devolver directamente las entidades JPA porque eso puede:

- exponer campos internos;
- crear problemas de serialización;
- acoplar la API a la estructura de la base de datos;
- facilitar modificaciones no autorizadas.

---

## 6. Seguridad: autenticación y autorización

Son conceptos distintos.

### Autenticación

Responde a:

> ¿Quién eres?

El usuario inicia sesión con sus credenciales. El backend comprueba la contraseña y emite un JWT.

### Autorización

Responde a:

> ¿Qué puedes hacer?

Después de validar el JWT, Spring Security comprueba los roles y las reglas de acceso.

Flujo previsto:

```text
Usuario introduce credenciales
        ↓
POST /api/auth/login
        ↓
Backend valida usuario y contraseña
        ↓
Se devuelve un JWT
        ↓
El frontend lo envía en Authorization: Bearer <token>
        ↓
Spring Security valida el token
        ↓
Se comprueba el rol y la propiedad de la incidencia
```

### JWT

Un JWT es un token firmado que contiene información sobre la identidad del usuario y sus permisos. El backend puede validar la firma sin guardar necesariamente una sesión en memoria.

El token no debe contener información secreta. Su función es transportar claims verificables, como:

```text
sub: usuario-123
roles: [TECHNICIAN]
exp: fecha de expiración
```

### Roles

La jerarquía funcional prevista es:

- `USER`: crea incidencias y consulta las que le corresponden;
- `TECHNICIAN`: trabaja con incidencias asignadas y cambia su estado;
- `ADMIN`: administra usuarios, roles, categorías y estadísticas.

La interfaz puede ocultar botones, pero la seguridad real debe estar en el backend. Un usuario no autorizado podría llamar directamente a la API con `curl`.

---

## 7. La configuración de seguridad actual

La clase `SecurityConfig` ya establece la política inicial:

```java
.requestMatchers("/api/health", "/actuator/health", "/api/auth/**").permitAll()
.anyRequest().authenticated()
```

Esto significa:

- el health check puede consultarse sin iniciar sesión;
- el login debe ser público para poder obtener el token;
- cualquier endpoint de negocio futuro deberá exigir autenticación.

Todavía no existe el login JWT completo. La configuración actual prepara el punto de entrada de seguridad, pero faltan el usuario, las contraseñas, el servicio de autenticación y el filtro JWT.

---

## 8. Modelo de dominio

El dominio expresa las reglas del negocio.

### Ticket

Una incidencia podría tener:

```text
id
title
description
status
priority
createdBy
assignedTechnician
category
createdAt
updatedAt
```

### Estados

```text
OPEN
IN_PROGRESS
WAITING_USER
RESOLVED
CLOSED
```

El estado representa el ciclo de vida de la incidencia.

Ejemplo de flujo:

```text
OPEN → IN_PROGRESS → WAITING_USER → IN_PROGRESS → RESOLVED → CLOSED
```

No todos los cambios deberían permitirse. Por ejemplo, un usuario normal no debería cerrar directamente una incidencia.

### Prioridades

```text
LOW
MEDIUM
HIGH
CRITICAL
```

La prioridad indica la urgencia, no el estado del trabajo.

- Estado: qué está ocurriendo con la incidencia.
- Prioridad: qué importancia o urgencia tiene.

### Comentarios

Los comentarios permiten mantener una conversación asociada al ticket. Deben registrar autor y fecha.

### Categorías

Permiten clasificar las incidencias:

```text
Hardware
Software
Accesos
Red
Correo
Sistemas internos
```

### Adjuntos

La tabla de adjuntos puede guardar metadatos, como nombre, tamaño, tipo y ubicación. El contenido puede almacenarse inicialmente en disco local y posteriormente en un almacenamiento externo.

---

## 9. Base de datos y JPA

PostgreSQL es la base de datos relacional.

Las tablas propuestas son:

```text
users
roles
user_roles
tickets
ticket_comments
categories
attachments
ticket_history
```

### Relaciones principales

```text
User 1 ─── N Ticket
User 1 ─── N Comment
Ticket 1 ─── N Comment
Ticket N ─── 1 Category
Ticket 1 ─── N HistoryEntry
User N ─── N Role
```

JPA permite mapear objetos Java a tablas relacionales. Hibernate es la implementación que ejecuta ese mapeo.

Ejemplo conceptual:

```java
@Entity
class Ticket {
    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    private TicketStatus status;
}
```

Usaremos `EnumType.STRING` para que la base de datos almacene `OPEN` o `RESOLVED`, en lugar de un número cuyo significado podría cambiar.

---

## 10. Auditoría e historial

El historial no es lo mismo que un log técnico.

- Un log sirve para diagnosticar el funcionamiento del sistema.
- La auditoría forma parte del negocio y responde quién cambió los datos.

Cada entrada de `ticket_history` podría almacenar:

```text
id
ticket_id
actor_id
action
old_value
new_value
created_at
```

Ejemplo:

```text
actor: técnico-7
action: STATUS_CHANGED
old_value: IN_PROGRESS
new_value: RESOLVED
created_at: 2026-10-11 14:30
```

La auditoría debería ser append-only: se añaden hechos y no se modifican los hechos antiguos.

---

## 11. Frontend Angular

Angular es el framework encargado de construir la interfaz.

### Componentes

Un componente combina:

- plantilla HTML;
- estilos;
- lógica TypeScript.

La base actual contiene un componente raíz mínimo para demostrar que Angular arranca.

### Servicios

Los servicios encapsularán comunicación y lógica reutilizable:

```text
AuthService
TicketService
CategoryService
UserService
```

Por ejemplo, `TicketService` llamará a `/api/tickets` mediante `HttpClient`.

### RxJS

Angular usa RxJS para trabajar con operaciones asíncronas. Una petición HTTP devuelve normalmente un `Observable`.

Conceptualmente:

```typescript
this.ticketService.getTickets().subscribe(tickets => {
  this.tickets = tickets;
});
```

RxJS permite transformar, combinar, cancelar y reaccionar a flujos de datos.

### Guards e interceptores

Más adelante se usarán:

- **AuthInterceptor**: añade automáticamente el JWT a las peticiones;
- **AuthGuard**: impide entrar en rutas privadas sin sesión;
- **RoleGuard**: limita rutas de administración según el rol.

La protección del frontend mejora la experiencia, pero no sustituye la seguridad del backend.

### Bootstrap

Bootstrap aporta estilos y componentes visuales para construir rápidamente una interfaz consistente y responsive.

---

## 12. Docker

Docker empaqueta aplicaciones en contenedores reproducibles.

### Imagen y contenedor

- Una **imagen** es una plantilla inmutable.
- Un **contenedor** es una instancia ejecutándose a partir de esa imagen.

### Dockerfile del backend

El backend utiliza un build de varias fases:

```text
Fase 1: Maven compila el proyecto
Fase 2: una imagen Java más pequeña ejecuta el JAR
```

Esto evita incluir Maven y código fuente innecesario en la imagen final.

### Docker Compose

Compose coordina los servicios:

```text
postgres
backend
frontend
```

`depends_on` y el health check ayudan a que el backend espere a que PostgreSQL esté disponible.

El volumen `postgres_data` conserva la base de datos aunque se elimine el contenedor.

---

## 13. Configuración por entorno

La aplicación usa variables de entorno para no fijar credenciales en el código:

```yaml
url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/helpdesk}
username: ${SPRING_DATASOURCE_USERNAME:helpdesk}
password: ${SPRING_DATASOURCE_PASSWORD:helpdesk_dev}
```

La expresión significa:

1. usa la variable si existe;
2. si no existe, usa el valor de desarrollo indicado después de `:`.

En producción nunca se deben usar las credenciales de desarrollo ni subir secretos al repositorio.

---

## 14. Pruebas

La prueba actual comprueba que:

```http
GET /api/health
```

devuelve HTTP 200 y `status: UP`.

Más adelante añadiremos:

### Pruebas unitarias

Prueban una clase o regla aislada, por ejemplo:

- una transición de estado válida;
- una transición inválida;
- cálculo de permisos.

### Pruebas de integración

Comprueban que varios componentes trabajan juntos:

- controlador;
- servicio;
- repositorio;
- base de datos.

### Pruebas de seguridad

Verifican que un `USER` no pueda ejecutar operaciones de `ADMIN` o modificar tickets ajenos.

---

## 15. GitHub Actions

La integración continua ejecuta automáticamente tareas cada vez que se hace push o se abre un pull request.

Pipeline actual:

```text
git push
   ↓
GitHub Actions
   ↓
Checkout del código
   ↓
Configurar Java 21
   ↓
mvn verify
   ↓
Configurar Node 22
   ↓
npm ci
   ↓
npm test
   ↓
npm run build
```

El objetivo es detectar errores antes de integrar cambios.

Una evolución posterior puede añadir:

- análisis de calidad;
- escaneo de vulnerabilidades;
- construcción de imágenes Docker;
- publicación de artefactos;
- despliegue.

---

## 16. Qué existe y qué falta

### Ya existe

- estructura del monorepo;
- aplicación Spring Boot;
- endpoint de salud;
- dependencias de seguridad, JPA, PostgreSQL, Flyway, JWT y tests;
- configuración de Spring Security stateless;
- usuarios, roles y contraseñas BCrypt;
- login JWT y endpoint `/api/auth/me`;
- migración Flyway `V1__create_users_and_roles.sql`;
- base Angular compilable;
- Bootstrap preparado;
- Dockerfiles;
- Docker Compose;
- `.env.example`;
- prueba inicial del backend;
- workflow de GitHub Actions;
- documentación de arquitectura.

### Todavía falta implementar

- CRUD de categorías;
- entidad y API de tickets;
- workflow de estados;
- comentarios;
- asignación de técnicos;
- adjuntos;
- historial de auditoría;
- dashboard y estadísticas;
- pruebas de negocio y seguridad;
- migraciones posteriores para categorías, tickets y comentarios.

La base no pretende fingir que todo eso ya está terminado. Su objetivo es que las siguientes funcionalidades se incorporen sobre una arquitectura coherente.

---

## 17. Orden recomendado de implementación

1. Modelo `Category`.
2. Entidad `Ticket` y estados.
3. Creación y consulta de tickets.
4. Reglas de visibilidad según el rol.
5. Asignación a técnicos.
6. Cambios de prioridad y estado.
7. Comentarios.
8. Auditoría.
9. Frontend de login y dashboard.
10. Panel de usuario.
11. Panel de técnico.
12. Panel de administrador.
13. Tests, documentación de API y refinamiento visual.
