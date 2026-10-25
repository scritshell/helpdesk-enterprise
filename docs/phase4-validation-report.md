# Informe de validación — Fase 4

Fecha de validación: 2026-10-18.

## Backend

| Comando | Resultado |
|---|---|
| `mvn -B verify` | Correcto |
| Pruebas ejecutadas | 9 |
| Fallos | 0 |
| Errores | 0 |
| Flyway | V1, V2 y V3 validadas y aplicadas |
| Empaquetado Maven | Correcto |

El contexto Spring se ejecutó contra PostgreSQL 16.15 local. Flyway informó:

```text
Successfully validated 3 migrations
Successfully applied 3 migrations to schema "public", now at version v3
```

Las pruebas cubren salud del contexto, JWT, transiciones de estado, comentarios, auditoría y autorización.

## Frontend

| Comando | Resultado |
|---|---|
| `npm ci` | Correcto; 473 paquetes instalados |
| `npm test` | Correcto; 1 archivo y 2 pruebas |
| `npm run build` | Correcto |
| `npm audit` | 0 vulnerabilidades: 0 low, 0 moderate, 0 high, 0 critical |

La prueba de Angular verifica la creación del componente raíz y la presencia de su título y texto inicial.

## Docker Compose

La validación de Docker requiere una instalación local de Docker Engine y Compose. El workflow de GitHub Actions mantiene PostgreSQL como servicio para CI.

Para validar el entorno Docker sin borrar volúmenes:

```bash
sudo docker compose up -d --build
sudo docker compose ps
sudo docker compose logs --tail=120 backend
```

La migración V3 no requiere `down -v`; solo crea `ticket_comments`, `ticket_history`, índices y restricciones.

## Auditoría npm

La auditoría inicial tenía 9 vulnerabilidades transitivas procedentes del árbol Webpack de `@angular-devkit/build-angular`. La solución aplicada fue migrar al builder oficial moderno `@angular/build` de Angular 21, con Vitest y jsdom para pruebas. No se usó `npm audit fix --force` ni se cambió de versión principal de Angular.

La auditoría final real fue:

```text
info: 0
low: 0
moderate: 0
high: 0
critical: 0
total: 0
```

Durante `npm ci` aparece un aviso deprecando `@angular/animations@21.2.25`; no es una vulnerabilidad de seguridad. Se mantiene porque el proyecto sigue en Angular 21 y no se ha pedido una migración mayor.
