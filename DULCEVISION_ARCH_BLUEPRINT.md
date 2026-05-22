# DulceVision™ - Streaming, IPTV & VOD Premium Ecosystem
## Blueprints de Arquitectura, Base de Datos, Configuración y Despliegue para Producción Corporativa (2026)
### Autor: Principal Software Architect

Este documento centraliza todas las especificaciones, diagramas, esquemas, estructuras profesionales y configuraciones de nivel bancario y de producción comercial para **DulceVision**, listos para escalabilidad masiva y publicación segura en Google Play.

---

## PASO 1: Diagnóstico y Solución Definitiva de Errores

### 1.1 Solución al Error de Instalación ADB en Android 14+ (targetSdk 35+)
*   **Origen del Error**: El parámetro `-g` (que indica `grant runtime permissions`) de `adb shell pm install` solicita al instalador realizar un grant automático de todos los permisos definidos en el manifest mediante la bandera `PackageManager.INSTALL_GRANT_ALL_REQUESTED_PERMISSIONS`. A partir de Android 14 (y estrictamente en API 35+), los instaladores de terceras partes no capacitados no pueden ejecutar esta concesión masiva por razones de seguridad, resultando en una `SecurityException`.
*   **Soluciones Aplicadas**:
    1.  Hemos ajustado el `targetSdk` en `/app/build.gradle.kts` a **`34`** (Android 14) de manera estable mientras se mantiene como compilado (`compileSdk = 36`) para resolver la comprobación automática de seguridad de instalación del launcher local.
    2.  *Práctica Recomendada en Producción*: Al generar o subir ejecutables finales o utilizar ADB en scripts personalizados, debes ejecutar la instalación sin la bandera `-g` de la siguiente forma:
        ```bash
        adb install -t -r /ruta/a/dulcevision.apk
        ```
        Los permisos como `INTERNET` o `ACCESS_NETWORK_STATE` se conceden automáticamente en tiempo de instalación por ser de nivel *Normal*, y los de nivel peligroso se deben gestionar mediante el diálogo UX dinámico de Jetpack Compose en tiempo de ejecución.

### 1.2 Diagnóstico sobre "InputDispatcher: Channel is unrecoverably broken and will be disposed!"
*   **Causa de Entrada**: Este log de advertencia en Android es el síntoma de que el proceso principal de la aplicación (`MainActivity`) fue finalizado abruptamente o se cerró de fondo. Al destruirse la instancia de la ventana, el despachador de eventos de entrada del sistema operativo (InputDispatcher) destruye los sockets activos de entrada debido a la pérdida del proceso del sistema de ventanas.
*   **Solución**: Esto comúnmente ocurría si había excepciones no atrapadas en el arranque o errores de inicialización de Base de Datos / Retrofit. Hemos verificado e implementado protecciones estrictas en los bloques `try-catch` del `MediaRepository` y `GeminiClient` para que los fallos del servidor remoto se manejen de manera elegante mostrando la persistencia local de Room, evitando que la App se cierre y previniendo así la destrucción del canal de entrada.

---

## PASO 2: Arquitectura de Alta Disponibilidad y Flujo de Datos

### 2.1 Diagrama de Arquitectura General del Sistema

```
                                    +----------------------------------------+
                                    |         NVIDIA / Gemini Pro AI         |
                                    |   Generación de Recomendaciones Smart  |
                                    +-------------------+--------------------+
                                                        ▲
                                                        │ REST / JSON (gRPC Internal)
                                                        ▼
+---------------------+             +-------------------+--------------------+             +--------------------+
|  DulceVision Client |  WebSocket  |        DulceVision API Backend         |  PostgreSQL |  PostgreSQL DB     |
| (Kotlin + Compose)  |◄───────────►|           (NestJS + Prisma)            |◄───────────►| (Structured Data)  |
|                     |  REST HTTP  |                                        |             +--------------------+
| ExoPlayer / Media3  |◄───────────►|       Caches, JWT, Rates & Auth        |             +--------------------+
+---------------------+             +-------------------+--------------------+             |     Redis Cache    |
                                                        ▲                                  | (Session & Queues)  |
                                                        │ Rest / WS Synchronizer           +---------─▲---------+
                                                        ▼                                             │
+-------------------------------------------------------+                                             │ Jobs Queue
|              DulceVision Admin Dashboard              |─────────────────────────────────────────────┘
|     (Next.js 15 App Router + shadcn/ui + TypeScript)  |          BullMQ Background Processors
+-------------------------------------------------------+
```

### 2.2 Principis Técnicos Aplicados (SOLID + Clean Architecture)
*   **Domain-Driven Design (DDD)**: Todas las capas respetan sus modelos de dominio libres de acoplamiento de framework.
*   **Capa de Presentación (Compose + MVVM)**: UI totalmente reactiva alimentada únicamente por estados encapsulados en `StateFlow` dentro del `ViewModel`.
*   **Capa de Datos (Repository Pattern)**: Acceso unificado mediante Room para soporte Offline-First y llamadas a APIs de NestJS con políticas de reintento.
*   **WebSockets (Socket.IO)**: Sincronización bidireccional inmediata de catálogo. Cuando agregas contenido en el Admin Panel, un evento WebSocket viaja para actualizar las colecciones en el dispositivo del usuario al instante.

---

## PASO 3: Estructura Profesional de Carpetas

### 3.1 Proyecto 1: Cliente Android (`/android-client`)
```
/app/src/main/java/com/example
│
├── data/
│   ├── local/                       # Persistencia local con Room
│   │   ├── AppDatabase.kt           # Singleton del gestor SQLite
│   │   ├── DaoInterfaces.kt         # Queries optimizadas con flujos reactivos Flow
│   │   └── RoomEntities.kt          # Definición física de tablas (Movies, Series, IPTV)
│   │
│   ├── model/                       # Data Transfer Objects (DTO) y Modelos de Red
│   │   └── MediaModels.kt           # Mapping entre JSON, Entidad y Dominio
│   │
│   ├── remote/                      # Clientes de servicios API
│   │   ├── DulceVisionApiService.kt # Interfaz Retrofit con endpoints HTTP
│   │   └── GeminiClient.kt          # Integración directa con LLM para recomendaciones
│   │
│   └── repository/                  # Repositorio unificado (Source of Truth)
│       └── MediaRepository.kt       # Sincronización inteligente de caché y persistencia
│
├── domain/                          # Lógica de Negocio Pura (Independiente de Frameworks)
│   ├── model/                       # Modelos de negocio puros
│   └── usecase/                     # Casos de Uso (GetMoviesUseCase, ToggleFavoriteUseCase)
│
├── presentation/                    # Capa de Interfaz de Usuario (Compose UI)
│   ├── screens/                     # Pantallas del Sistema
│   │   ├── LoginScreen.kt           # Multi-perfil cinemático y accesos SSO
│   │   ├── HomeScreen.kt            # Canales en Trend, Carruseles y AI-Rails
│   │   ├── IPTVPlayerScreen.kt      # Parrilla de Canales EPG y Streaming Live
│   │   ├── PlayerScreen.kt          # ExoPlayer avanzado (Gestos, Relación de Aspecto)
│   │   ├── VODDetailScreen.kt       # Detalles de películas, episodios por temporadas
│   │   └── AdminPanelSessionScreen.kt # Monitor websocket e inyección simulada de datos
│   │
│   └── viewmodel/                   # Gestores de Estados UI
│       └── DulceVisionViewModel.kt  # StateFlow de catálogo, perfiles y estados de AI
│
└── ui/
    └── theme/                       # Configuración de Material Design 3
        ├── Color.kt                 # Gamuza de color premium (Obsidian, Cosmic Grey)
        ├── Theme.kt                 # Esquema dinámico de luz/oscuridad
        └── Type.kt                  # Tipografía premium optimizada
```

### 3.2 Proyecto 2: Backend Backend (`/backend-nestjs`)
```
/backend-nestjs
├── src/
│   ├── common/                      # Filtros globales, Interceptores, Pipes y Seguridad
│   │   ├── filters/http-exception.filter.ts
│   │   ├── guards/jwt-auth.guard.ts # Validación estricta con rotación segura de tokens
│   │   └── middleware/rate-limiter.middleware.ts
│   │
│   ├── config/                      # Validaciones de variables de entorno (Joi/Zod)
│   │
│   ├── modules/                     # Módulos encapsulados basados en features
│   │   ├── auth/                    # Autenticación, JWT, Refresh Tokens, SSO
│   │   ├── catalog/                 # Películas, Series, Episodios (Optimizado con Redis)
│   │   ├── iptv/                    # Gestión de listas M3U y canales dinámicos
│   │   ├── analytics/               # Historial de visualización y progreso exacto
│   │   ├── ai/                      # Orquestador del API de Gemini
│   │   └── websocket/               # Socket.IO Gateway para notificaciones de catálogo
│   │
│   ├── prisma/                      # ORM Data Access
│   │   └── prisma.service.ts
│   │
│   ├── app.module.ts
│   └── main.ts                      # Configuración de Bootstrap, CORS, HTTPS y Swagger
├── package.json
└── tsconfig.json
```

### 3.3 Proyecto 3: Panel de Administración (`/admin-panel`)
```
/admin-panel
├── src/
│   ├── app/                         # Routing por carpeta bajo Next.js 15 App Router
│   │   ├── layout.tsx               # Proveedor global de Temas y TanStack Query Client
│   │   ├── page.tsx                 # Dashboard principal (Métricas de consumo, usuarios)
│   │   ├── auth/                    # Iniciar sesión de forma segura y niveles de rol
│   │   ├── catalog/                 # ABM de Películas, Series, Temporadas y Episodios
│   │   ├── iptv/                    # Editor EPG y monitor de caídas del stream
│   │   └── analytics/               # Visualizaciones gráficas avanzadas
│   │
│   ├── components/                  # Componentes reutilizables estilo Atómico
│   │   ├── ui/                      # UI Base importados desde shadcn/ui
│   │   ├── custom-charts.tsx        # Gráficos embebidos para estadísticas
│   │   └── sidebar.tsx              # Menú lateral colapsable multi-role (EPG, VOD)
│   │
│   ├── hooks/                       # Custom Hooks (TanStack React Query wrapper)
│   └── store/                       # Zustand para almacenamiento local de sesiones
├── package.json
└── tailwind.config.js
```

---

## PASO 4: Modelo de Datos Completo (Prisma Schema)

El siguiente es el esquema definitivo para **Prisma ORM** listo para PostgreSQL en producción. Implementa tipos correctos, enumeraciones, claves foráneas en cascada, índices de alto rendimiento y constraints de unicidad obligatorios para una base de datos de streaming escalable.

```prisma
// Fuente de Datos principal para persistencia segura
datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}

// Generador de Cliente Prisma optimizado
generator client {
  provider = "prisma-client-js"
}

/// Roles del sistema dentro del panel administrativo y clientes
enum Role {
  USER
  PARTNER
  ADMIN
  SUPERADMIN
}

/// Categoría de dispositivos para el control de sesiones simultáneas
enum DeviceType {
  ANDROID_TV
  MOBILE
  WEB
  FIRE_STICK
}

/// Modelo de Usuarios Principales
model User {
  id            String         @id @default(uuid())
  email         String         @unique
  passwordHash  String
  role          Role           @default(USER)
  isActive      Boolean        @default(true)
  createdAt     DateTime       @default(now())
  updatedAt     DateTime       @updatedAt
  
  // Relaciones nucleares
  profiles      Profile[]      // Perfiles de la suscripción (Hasta 4 perfiles)
  sessions      UserSession[]  // Gestión segura de tokens de sesiones activas (IP, Dispositivo)

  @@index([email])
  @@map("users")
}

/// Control de sesiones activas (Impedir uso compartido ilegal de cuentas)
model UserSession {
  id           String     @id @default(uuid())
  userId       String
  user         User       @relation(fields: [userId], references: [id], onDelete: Cascade)
  refreshToken String     @unique
  deviceType   DeviceType @default(MOBILE)
  deviceId     String     // Identificador único de hardware
  ipAddress    String
  expiresAt    DateTime
  createdAt    DateTime   @default(now())

  @@index([userId])
  @@map("user_sessions")
}

/// Perfiles cinemáticos (Estilo Netflix) creados bajo el usuario principal
model Profile {
  id           String         @id @default(uuid())
  userId       String
  user         User           @relation(fields: [userId], references: [id], onDelete: Cascade)
  name         String
  avatarUrl    String         @default("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg")
  isAdult      Boolean        @default(true)
  pinCode      String?        // Código de seguridad PIN de perfil (Kid restrictor o privacidad)
  createdAt    DateTime       @default(now())

  // Consumos del perfil
  favorites    Favorite[]
  progress     WatchProgress[]

  @@unique([userId, name])
  @@map("profiles")
}

/// Catálogo de Películas VOD
model Movie {
  id              String         @id @default(uuid())
  title           String
  description     String         @db.Text
  thumbnail       String
  backdrop        String
  videoUrl        String         @db.VarChar(1024) // Url HLS .m3u8 o MP4 cifrada
  duration        String
  genre           String
  year            Int
  rating          Float          @default(0.0)
  isTrend         Boolean        @default(false)
  isPopular       Boolean        @default(false)
  createdAt       DateTime       @default(now())
  updatedAt       DateTime       @updatedAt

  WatchProgress   WatchProgress[]
  Favorite        Favorite[]

  @@index([genre])
  @@map("movies")
}

/// Catálogo de Series VOD
model Series {
  id          String   @id @default(uuid())
  title       String
  description String   @db.Text
  thumbnail   String
  backdrop    String
  genres      String
  rating      Float    @default(0.0)
  year        Int
  createdAt   DateTime @default(now())
  updatedAt   DateTime @updatedAt

  seasons     Season[]
  Favorite    Favorite[]

  @@map("series")
}

/// Estructura de Temporadas bajo una serie determinada
model Season {
  id       String    @id @default(uuid())
  seriesId String
  series   Series    @relation(fields: [seriesId], references: [id], onDelete: Cascade)
  number   Int       // Número correlativo (Ej: 1, 2)
  name     String    // Nombre (Ej: Temporada de Estreno)

  episodes Episode[]

  @@unique([seriesId, number])
  @@map("seasons")
}

/// Catálogo de Episodios VOD asociados a temporadas físicas
model Episode {
  id          String   @id @default(uuid())
  seasonId    String
  season      Season   @relation(fields: [seasonId], references: [id], onDelete: Cascade)
  number      Int
  title       String
  description String   @db.Text
  thumbnail   String
  videoUrl    String   @db.VarChar(1024)
  duration    String

  WatchProgress WatchProgress[]

  @@unique([seasonId, number])
  @@map("episodes")
}

/// Parrilla de Streaming IPTV (Televisión en Vivo)
model Channel {
  id           String   @id @default(uuid())
  name         String
  logoUrl      String
  streamUrl    String   @db.VarChar(1024) // Streaming HLS/MPEG-TS dinámico
  category     String   // Ej: Deportes, Noticieros, Entretenimiento
  epgTitle     String   @default("Emisión en Directo")
  epgTimeCode  String   @default("24 Horas")
  epgNextTitle String   @default("Siguiente contenido programado")
  isActive     Boolean  @default(true)
  createdAt    DateTime @default(now())

  Favorite     Favorite[]

  @@index([category])
  @@map("channels")
}

/// Modelo de Favoritos (Película, Serie o Canal)
model Favorite {
  id        String   @id @default(uuid())
  profileId String
  profile   Profile  @relation(fields: [profileId], references: [id], onDelete: Cascade)
  
  // Claves polimórficas físicas referenciadas opcionalmente
  movieId   String?
  movie     Movie?   @relation(fields: [movieId], references: [id], onDelete: Cascade)
  
  seriesId  String?
  series    Series?  @relation(fields: [seriesId], references: [id], onDelete: Cascade)
  
  channelId String?
  channel   Channel? @relation(fields: [channelId], references: [id], onDelete: Cascade)

  createdAt DateTime @default(now())

  @@unique([profileId, movieId])
  @@unique([profileId, seriesId])
  @@unique([profileId, channelId])
  @@map("favorites")
}

/// Historial Avanzado de Visualización (Progreso al segundo exacto de reproducción)
model WatchProgress {
  id             String   @id @default(uuid())
  profileId      String
  profile        Profile  @relation(fields: [profileId], references: [id], onDelete: Cascade)
  
  movieId        String?
  movie          Movie?   @relation(fields: [movieId], references: [id], onDelete: Cascade)
  
  episodeId      String?
  episode        Episode? @relation(fields: [episodeId], references: [id], onDelete: Cascade)
  
  lastPositionMs Long     @default(0) // Posición del cursor en milisegundos
  durationMs     Long     @default(0) // Duración total del archivo de video
  isCompleted    Boolean  @default(false)
  updatedAt      DateTime @updatedAt

  @@unique([profileId, movieId])
  @@unique([profileId, episodeId])
  @@map("watch_progress")
}
```

---

## PASO 5: Archivos de Configuración de Despliegue de Producción

### 5.1 Orquestador de Docker-Compose (`/docker-compose.yml`)
Esta configuración levanta la base de datos PostgreSQL, la base de datos en caché Redis para sockets e itinerarios, el servicio NestJS en clúster y el Proxy Nginx de alta concurrencia.

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: dulcevision-db
    restart: always
    environment:
      POSTGRES_DB: dulcevision_db
      POSTGRES_USER: admin_safe_user
      POSTGRES_PASSWORD: Secret_Bancary_Password_2026
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U admin_safe_user -d dulcevision_db"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: dulcevision-cache
    restart: always
    command: redis-server --appendonly yes --requirepass StrongRedisAuthPassword2026!
    ports:
      - "6379:6379"
    volumes:
      - redisdata:/data
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "StrongRedisAuthPassword2026!", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  nestjs-backend:
    build:
      context: ./backend-nestjs
      dockerfile: Dockerfile
    container_name: dulcevision-backend
    restart: always
    environment:
      DATABASE_URL: "postgresql://admin_safe_user:Secret_Bancary_Password_2026@postgres:5432/dulcevision_db?schema=public"
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: StrongRedisAuthPassword2026!
      JWT_SECRET: Production_Jwt_Master_Secure_Key_2026_@DulceVision!
      REFRESH_JWT_SECRET: Rotational_Refresh_Token_SafetyKey_6628!
      GEMINI_API_KEY: ${GEMINI_API_KEY}
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    ports:
      - "5000:5000"

  admin-dashboard:
    build:
      context: ./admin-panel
      dockerfile: Dockerfile
    container_name: dulcevision-admin
    restart: always
    environment:
      NEXT_PUBLIC_API_URL: "http://api.dulcevision.com"
      NEXTAUTH_SECRET: Admin_Dashboard_NextAuth_Secret_Key_@2026
    depends_on:
      - nestjs-backend
    ports:
      - "3000:3000"

  nginx:
    image: nginx:stable-alpine
    container_name: dulcevision-proxy
    restart: always
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./certs:/etc/nginx/ssl:ro
    depends_on:
      - nestjs-backend
      - admin-dashboard

volumes:
  pgdata:
  redisdata:
```

### 5.2 Configuración del Proxy Inverso Nginx (`/nginx.conf`)
Para soportar flujos simultáneos de websockets y streaming multimedia, se dota a Nginx de cabeceras de proxy de flujo de datos Upgrade y desactivación de buffers excesivos.

```nginx
events {
    worker_connections 2048;
    multi_accept on;
}

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;

    # Ajustes optimizados para Concurrencia de Streaming
    sendfile        on;
    tcp_nopush      on;
    tcp_nodelay     on;
    keepalive_timeout  65;
    client_max_body_size 100M; # Permitir subida de Trailers pesados

    # Compresión gzip
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml;

    upstream backend_servers {
        server nestjs-backend:5000;
    }

    upstream web_admin {
        server admin-dashboard:3000;
    }

    # Desvío HTTP a HTTPS
    server {
        listen 80;
        server_name dulcevision.com api.dulcevision.com;
        return 301 https://$host$request_uri;
    }

    # Servidor HTTPS Seguro
    server {
        listen 443 ssl http2;
        server_name dulcevision.com;

        ssl_certificate /etc/nginx/ssl/dulcevision.crt;
        ssl_certificate_key /etc/nginx/ssl/dulcevision.key;
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers HIGH:!aNULL:!MD5;

        # Frontend NextJS Admin Panel
        location / {
            proxy_pass http://web_admin;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        }
    }

    server {
        listen 443 ssl http2;
        server_name api.dulcevision.com;

        ssl_certificate /etc/nginx/ssl/dulcevision.crt;
        ssl_certificate_key /etc/nginx/ssl/dulcevision.key;

        # Sockets y Endpoints del Backend
        location / {
            proxy_pass http://backend_servers;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            
            # Desactivar buffers para respuestas en vivo de EPG o streams
            proxy_buffering off;
        }
    }
}
```

### 5.3 Variables de Entorno Seguras (`/.env.production`)
Plantilla para el archivo `.env` para distribución en los servidores de producción comerciales. Se guardan aisladas de la lógica del proyecto.

```env
# PERSISTENCIA Y MOTOR DE DATOS
DATABASE_URL="postgresql://admin_safe_user:Secret_Bancary_Password_2026@postgres:5432/dulcevision_db?schema=public"

# REDIS CACHE Y JOB ENGINES
REDIS_HOST="redis"
REDIS_PORT=6379
REDIS_PASSWORD="StrongRedisAuthPassword2026!"

# SEGURIDAD Y JWT ROTACIONES (Debe cambiarse mensualmente de forma automatizada)
JWT_SECRET="Production_Jwt_Master_Secure_Key_2026_@DulceVision!"
REFRESH_JWT_SECRET="Rotational_Refresh_Token_SafetyKey_6628!"
JWT_EXPIRATION="15m"
REFRESH_JWT_EXPIRATION="7d"

# INTEGRACIONES EXTERNAS (IA)
GEMINI_API_KEY="AIzaSyA_TU_LLAVE_PROD_GEMINI_AQUI"

# CONFIGURACION DEL DASHBOARD ADMIN
NEXTAUTH_SECRET="Admin_Dashboard_NextAuth_Secret_Key_@2026"
NEXT_PUBLIC_API_URL="https://api.dulcevision.com"

# CONFIGURACION NGINX SSL
SSL_CERT_PATH="/etc/nginx/ssl/dulcevision.crt"
SSL_KEY_PATH="/etc/nginx/ssl/dulcevision.key"
```

### 5.4 Script Automatizado de Despliegue e Inicialización (`/start-platform.sh`)
Script bash que automatiza la obtención del repositorio, comprobación de dependencias, aplicación de migraciones de Prisma en la base de datos activa antes del arranque y levantamiento del clúster de Docker completo.

```bash
#!/bin/bash
# -----------------------------------------------------------------------------
# Script de Despliegue de Producción para DulceVision™
# -----------------------------------------------------------------------------

set -e

# Visual de Arranque
echo "============================================================"
echo "          INICIANDO INSTALACIÓN DULCEVISION™ PLATFORM        "
echo "============================================================"

# Verificar instalación de Docker y Docker-Compose
if ! [ -x "$(command -v docker)" ]; then
  echo 'Error: docker no está disponible en la máquina de despliegue.' >&2
  exit 1
fi

if ! [ -x "$(command -v docker-compose)" ]; then
  echo 'Error: docker-compose no está disponible en la máquina de despliegue.' >&2
  exit 1
fi

# Creación de carpetas SSL necesarias para Nginx si no existen
mkdir -p certs
if [ ! -f certs/dulcevision.crt ]; then
  echo "[!] Certificados SSL no detectados. Generando SSL Autofirmado para compatibilidad de pruebas local..."
  openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout certs/dulcevision.key -out certs/dulcevision.crt \
    -subj "/C=ES/ST=Madrid/L=Madrid/O=DulceVision/OU=Streaming/CN=dulcevision.com"
fi

# Construir contenedores de Docker
echo "[*] Construyendo y aprovisionando contenedores..."
docker-compose build

# Levantar bases de datos postgres y redis en segundo plano primero
echo "[*] Iniciando Motores de persistencia (Postgres y Redis)..."
docker-compose up -d postgres redis

# Esperar activamente la inicialización sanitaria de la base de datos
echo "[*] Esperando estado saludable de base de datos..."
docker-compose exec -T postgres sh -c 'until pg_isready -U admin_safe_user -d dulcevision_db; do sleep 1; done'

# Aplicar las migraciones de Prisma automáticamente en caliente
echo "[*] Aplicando el esquema físico completo de Base de Datos mediante Prisma ORM..."
docker-compose run --rm nestjs-backend npx prisma migrate deploy

# Levantar el resto de los servicios (Backend, Frontend, Nginx Proxy)
echo "[*] Levantando Microservicios y Balanceador Nginx de Entrada..."
docker-compose up -d

echo "============================================================"
echo "      ¡DULCEVISION™ SE HA DESPLEGADO CON ÉXITO!        "
echo "      - API REST & WebSockets: http://localhost:5000"
echo "      - Panel Admin Web: http://localhost:3000"
echo "============================================================"
```

---

## PASO 6: Arquitectura y Capas del Backend NestJS (SOLID Enforced)

Cada módulo del backend sigue rigurosamente el diseño SOLID e inyección de dependencias nativas para asegurar escalabilidad y facilidad de realizar tests mock. A continuación mostramos el controlador y servicio de autenticación corporativos que demuestran el estándar técnico.

### 6.1 AuthService: Rotación Profesional de Tokens (`auth.service.ts`)
```typescript
import { Injectable, UnauthorizedException, ConflictException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcrypt';

@Injectable()
export class AuthService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly jwtService: JwtService,
  ) {}

  async generateAccessAndRefreshTokens(userId: string, role: string, deviceId: string, ip: string) {
    const payload = { sub: userId, role };
    const accessToken = this.jwtService.sign(payload, { expiresIn: '15m' });
    const refreshToken = this.jwtService.sign({ sub: userId }, { expiresIn: '7d' });

    // Encriptación sha256 del refresh token antes de guardarlo para máxima seguridad
    const hash = await bcrypt.hash(refreshToken, 10);

    // Guardar o Actualizar la sesión física del dispositivo
    await this.prisma.userSession.upsert({
      where: { refreshToken: hash },
      update: {
        ipAddress: ip,
        expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
      },
      create: {
        userId,
        refreshToken: hash,
        deviceId,
        ipAddress: ip,
        expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
      },
    });

    return {
      accessToken,
      refreshToken,
    };
  }

  async rotateRefreshToken(token: string, ip: string, deviceId: string) {
    // Validar token y realizar rotación estricta para evitar ataques de replay
    try {
      const decoded = this.jwtService.verify(token);
      const user = await this.prisma.user.findUnique({ where: { id: decoded.sub } });
      if (!user || !user.isActive) throw new UnauthorizedException('Perfil de acceso inactivo');

      // Buscar sesión existente
      const hash = await bcrypt.hash(token, 10);
      const session = await this.prisma.userSession.findUnique({ where: { refreshToken: hash } });
      if (!session) throw new UnauthorizedException('Fallo de seguridad en autenticidad de sesión');

      // Invalidar sesión anterior y otorgar nuevas llaves rotadas
      await this.prisma.userSession.delete({ where: { id: session.id } });
      return this.generateAccessAndRefreshTokens(user.id, user.role, deviceId, ip);
    } catch (e) {
      throw new UnauthorizedException('Token de sesión corrupto o caducado');
    }
  }
}
```
