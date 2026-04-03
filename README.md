# Sistema de Gestión de Inventario Multi-Sucursal

Sistema web para la gestión de inventario en múltiples sucursales, desarrollado como prueba técnica para **OptiPlant Consultores**.

## Stack Tecnológico

| Capa | Tecnología |
|------|-----------|
| Backend | Spring Boot 3.2 + Java 21 |
| Frontend | Angular 17 (standalone components) |
| Base de datos | PostgreSQL 16 |
| Infraestructura | Docker + Docker Compose |
| Autenticación | JWT stateless |

---

## Inicio Rápido

### Requisitos Previos

- Docker Desktop 4.x+
- Docker Compose v2+
- (Opcional para desarrollo local) Java 21, Node 20+, Maven 3.9+

```bash
# 1. Clonar / descomprimir el proyecto
cd multi-branch-inventory-system

# 2. Levantar todo con un comando
docker compose up --build

# 3. Acceder
#    Frontend:   http://localhost:4200
#    Backend API: http://localhost:8080
#    Base de datos: localhost:5432
```

## Credenciales por Defecto

| Campo | Valor |
|-------|-------|
| Email | `admin@empresa.com` |
| Contraseña | `Admin1234!` |
| Rol | `ADMINISTRADOR` |

> **Importante:** El usuario administrador es creado automáticamente por `DataInitializer` al arrancar la aplicación. Cambiar la contraseña y el `JWT_SECRET` en `.env` antes de cualquier despliegue productivo.

---

## Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                        Docker Compose                        │
│                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │   Frontend   │    │   Backend    │    │  PostgreSQL  │  │
│  │  Angular 17  │───▶│ Spring Boot  │───▶│     16       │  │
│  │  nginx:80    │    │   :8080      │    │    :5432     │  │
│  └──────────────┘    └──────────────┘    └──────────────┘  │
│       :4200              /api/*                             │
└─────────────────────────────────────────────────────────────┘
```

**Flujo de una petición autenticada:**

```
Browser → nginx (Angular SPA)
       → GET /api/... → JwtAuthenticationFilter → Controller → Service → Repository → PostgreSQL
                              ↓ 401 si token inválido
```

### Capas del Backend

```
Controller   →  valida DTOs, expone endpoints REST, gestiona @PreAuthorize
Service      →  lógica de negocio, transacciones @Transactional, genera Movimientos
Repository   →  Spring Data JPA, queries JPQL
Entity       →  mapeo JPA de tablas PostgreSQL
Security     →  JwtService (sign/verify), JwtAuthenticationFilter (per-request)
```

---

## Diagrama de Entidad-Relación (simplificado)

```
sucursales ──────────────────────────────┐
    │                                    │
    │ 1:N                                │ 1:N
    ▼                                    ▼
usuarios                             inventario ◀──── productos ◀──── categorias
    │                                    │                │
    │ crea                               │ stock          │ SKU, precio
    ▼                                    │                │
movimientos ◀───────────────────────────┘                │
(inmutables: ENTRADA/SALIDA/                              │
 AJUSTE/TRANSFERENCIA_*)                                  │
                                                          │
transferencias                                            │
  ├── transferencia_items ─────────────────────────────▶ │
  │   (cantidad_solicitada / enviada / recibida)          │
  │                                                       │
ordenes_compra                                            │
  └── orden_compra_items ──────────────────────────────▶ │
                                                          │
ventas                                                    │
  └── venta_items ────────────────────────────────────▶ ─┘

alertas (stock_bajo, generadas automáticamente)
```

---

## Módulos del Sistema

| Módulo | Descripción |
|--------|-------------|
| Autenticación | Login JWT, roles por usuario |
| Inventario | Stock por sucursal, ajustes con trazabilidad |
| Productos | Catálogo con SKU, categorías, precios costo/venta |
| Ventas | Registro de ventas, descuento automático de stock |
| Compras | Órdenes de compra con flujo de aprobación y recepción |
| Transferencias | Movimiento entre sucursales (PENDIENTE → APROBADA → EN_TRANSITO → COMPLETADA) |
| Reportes | Stock actual, movimientos por período, resumen de ventas |
| Alertas | Notificaciones automáticas por stock bajo mínimo |
| Administración | Gestión de usuarios y sucursales (solo ADMINISTRADOR) |

---

## Estado de Transferencias

```
PENDIENTE ──▶ APROBADA ──▶ EN_TRANSITO ──▶ COMPLETADA
                                    │
                                    └──▶ INCOMPLETA
                                         (si cantidad recibida < enviada)
```

- El stock se **descuenta en origen** al pasar a `EN_TRANSITO` (al enviar).
- El stock se **acredita en destino** al confirmar recepción (`COMPLETADA`).
- Toda transición genera registros en la tabla `movimientos` (trazabilidad completa).

---

## Roles y Permisos

| Acción | ADMINISTRADOR | GERENTE_SUCURSAL | OPERADOR_INVENTARIO |
|--------|:---:|:---:|:---:|
| Gestionar usuarios y sucursales | ✓ | — | — |
| Ver reportes globales | ✓ | — | — |
| Aprobar transferencias | ✓ | ✓ | — |
| Registrar compras / órdenes | ✓ | ✓ | — |
| Registrar ventas | ✓ | ✓ | ✓ |
| Ajustar inventario | ✓ | ✓ | ✓ |
| Ver inventario y productos | ✓ | ✓ | ✓ |

---

## Estructura del Proyecto

```
multi-branch-inventory-system/
├── docker-compose.yml              # Orquestación de los 3 servicios
├── .env.example                    # Variables de entorno (copiar a .env)
├── db/
│   └── init.sql                    # Esquema PostgreSQL + datos semilla
├── backend/
│   ├── Dockerfile                  # Multi-stage: JDK build → JRE runtime
│   ├── pom.xml
│   └── src/main/java/com/inventory/
│       ├── config/                 # SecurityConfig, DataInitializer, ExceptionHandler
│       ├── controller/             # Endpoints REST (Auth, Inventario, Ventas, ...)
│       ├── dto/                    # Request / Response objects
│       ├── entity/                 # Entidades JPA + enums
│       ├── repository/             # Spring Data JPA
│       ├── security/               # JwtService, JwtAuthenticationFilter
│       └── service/                # Lógica de negocio + transacciones
└── frontend/
    ├── Dockerfile                  # Multi-stage: Node build → nginx serve
    ├── nginx.conf
    └── src/app/
        ├── core/                   # Guards, interceptors, services, models
        ├── features/               # dashboard, inventario, ventas, compras,
        │                           # transferencias, reportes, admin, auth
        └── layout/                 # Shell (sidebar + router-outlet)
```

---

## API — Endpoints Principales

### Autenticación

```
POST /api/auth/login
Body:     { "email": "admin@empresa.com", "password": "Admin1234!" }
Response: { "token": "...", "nombre": "...", "rol": "ADMINISTRADOR", "sucursalId": null }
```

### Uso con token

```bash
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/inventario
```

### Resumen de recursos

| Recurso | Método | Endpoint |
|---------|--------|----------|
| Login | POST | `/api/auth/login` |
| Inventario por sucursal | GET | `/api/inventario?sucursalId=1` |
| Ajustar stock | POST | `/api/inventario/ajustar` |
| Productos | GET/POST | `/api/productos` |
| Ventas | GET/POST | `/api/ventas` |
| Órdenes de compra | GET/POST | `/api/ordenes-compra` |
| Transferencias | GET/POST | `/api/transferencias` |
| Reportes de movimientos | GET | `/api/reportes/movimientos/{sucursalId}?desde=&hasta=` |
| Reportes de ventas | GET | `/api/reportes/ventas/{sucursalId}?desde=&hasta=` |
| Stock bajo mínimo | GET | `/api/reportes/stock-bajo` |
| Alertas | GET | `/api/alertas` |
| Usuarios | GET/POST | `/api/usuarios` |
| Sucursales | GET/POST | `/api/sucursales` |

---

## Desarrollo Local (sin Docker)

### Base de datos

```bash
# Crear la base de datos en PostgreSQL local
psql -U postgres -c "CREATE DATABASE inventory_db;"
psql -U postgres -c "CREATE USER inventory_user WITH PASSWORD 'inventory_pass';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE inventory_db TO inventory_user;"
psql -U inventory_user -d inventory_db -f db/init.sql
```

### Backend

```bash
cd backend
./mvnw spring-boot:run
# API disponible en http://localhost:8080
```

### Frontend

```bash
cd frontend
npm install
ng serve
# App disponible en http://localhost:4200
# El proxy redirige /api → http://localhost:8080
```

---

## Variables de Entorno

Copiar `.env.example` a `.env` y ajustar los valores:

```bash
cp .env.example .env
```

| Variable | Descripción | Default |
|----------|-------------|---------|
| `DB_NAME` | Nombre de la base de datos | `inventory_db` |
| `DB_USER` | Usuario de PostgreSQL | `inventory_user` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `inventory_pass` |
| `JWT_SECRET` | Clave secreta para firmar tokens JWT (mín. 32 chars) | ver `.env.example` |
| `JWT_EXPIRATION` | Duración del token en ms | `86400000` (24 h) |
| `SPRING_PROFILE` | Perfil activo de Spring Boot | `docker` |

---

## Decisiones de Diseño

### ¿Por qué PostgreSQL y no MongoDB?
Las operaciones de inventario requieren **garantías ACID**: un ajuste de stock debe ser atómico (no puede quedar a medias). Además, los reportes necesitan JOINs entre sucursales, productos y movimientos — algo que PostgreSQL resuelve con eficiencia y que NoSQL complicaría.

### ¿Por qué movimientos obligatorios?
Toda modificación de stock genera un registro inmutable en `movimientos`. Esto provee **trazabilidad completa**: se puede reconstruir el historial de cualquier producto en cualquier sucursal en cualquier momento. Sin movimiento registrado, el cambio no es válido a nivel de negocio.

### ¿Por qué la transferencia descuenta stock al enviar y no al aprobar?
Aprobar es una decisión administrativa; enviar es el acto físico de sacar mercadería del almacén. Descontar al aprobar generaría stock "fantasma" (comprometido pero aún presente). El modelo `EN_TRANSITO` refleja la realidad: la mercadería salió del origen pero aún no llegó al destino.

### ¿Por qué JWT stateless?
Sin estado en el servidor → escala horizontalmente sin sesiones compartidas. El token lleva el rol y el `sucursalId`, suficiente para autorizar cada petición sin consultar la base de datos.

### ¿Por qué multi-stage Docker?
El stage de build usa JDK (~600 MB); el stage de runtime usa JRE slim (~200 MB). La imagen final no contiene código fuente ni herramientas de compilación — reduce superficie de ataque y tamaño de imagen en ~70%.

### ¿Por qué Angular standalone components?
Evita el overhead de NgModules para una SPA de tamaño medio. Cada componente declara sus dependencias explícitamente, lo que facilita el tree-shaking y el lazy loading por ruta.
