> **Nota (Entrega Q10):** ahora existe un `docker-compose.yml` en la carpeta raiz del ZIP que levanta este servicio junto con el otro microservicio y PostgreSQL, todo con un solo comando: `docker compose up --build` desde la raiz. Lo que sigue abajo es la guia para correr ESTE servicio de forma aislada (util para desarrollo), pero para la entrega usa el compose de la raiz.

---

# notification-service (Gradle) — implementación estricta del contrato OpenAPI

Microservicio REST construido en Java 21 + Spring Boot 3 + Gradle, **basado
únicamente en `notification-service-openapi.yaml`**. Solo implementa el
único endpoint de lectura que define el contrato — no incluye el
consumidor de Kafka (el propio contrato aclara que este servicio "es
principalmente un CONSUMIDOR de eventos Kafka... por eso este contrato
solo tiene un endpoint de lectura").

## ⚠️ Falta el JAR del wrapper de Gradle (igual que en ticket-service)

Este entorno no tuvo acceso a los servidores de Gradle para descargar el
binario `gradle/wrapper/gradle-wrapper.jar`. Opciones:

- **IntelliJ IDEA**: abre la carpeta directamente (`File > Open` sobre
  `build.gradle`) — IntelliJ resuelve el wrapper automáticamente.
- **Si ya tienes Gradle instalado**: `gradle wrapper --gradle-version 8.10`
  dentro de esta carpeta, y ya queda `./gradlew` funcionando.
- **Sin wrapper**: `gradle bootRun` directamente.

## Cómo correrlo

**1. Levanta PostgreSQL:**
```bash
docker run -d --name notification-postgres \
  -e POSTGRES_USER=agilti \
  -e POSTGRES_PASSWORD=agilti_pass \
  -e POSTGRES_DB=notification_db \
  -p 5432:5432 \
  postgres:15
```

**2. Corre la aplicación:**
```bash
./gradlew bootRun
```

**3. Confirma que arrancó:**
```
http://localhost:8081/swagger-ui.html
http://localhost:8081/actuator/health
```

## Datos de prueba

No hay datos precargados (ya no existe `data.sql`). Las notificaciones se
crean **solo consumiendo el evento Kafka `ticket-creado`** que publica
ticket-service al crear un ticket. Para tener datos basta con crear un
ticket (`POST http://localhost:8080/api/tickets`); la colección Postman lo
hace sola.

## Probar con Postman

Requiere el sistema completo levantado (`docker compose up --build` en la
raíz), porque la colección usa ticket-service, Kafka y este servicio.

1. Abre Postman → **Import** → arrastra los dos archivos de `postman/`:
   - `AgilTI-notification-service.postman_collection.json`
   - `AgilTI-notification-local.postman_environment.json`
2. Selecciona el entorno **"AgilTI - Local (notification-service)"**.
3. Corre la colección **en orden** con el botón **Run**: la petición 1 crea
   un ticket real, guarda su id en `{{ticketId}}` y espera a que el
   consumidor Kafka genere la notificación (consulta el historial hasta
   15 veces, con 1 s entre intentos). Las demás usan ese `ticketId`, así
   que funciona igual con la base vacía o con datos.

| # | Petición | Qué valida |
|---|---|---|
| 1 | Crear ticket en ticket-service y esperar su notificación | 201 y que la notificación aparece antes de agotar los reintentos |
| 2 | Historial del ticket creado | 200, al menos una notificación, todas de ese `ticketId`, mensaje con el título del ticket, 4 campos del contrato, orden más reciente primero |
| 3 | Listar notificaciones paginadas | 200, estructura de paginación, incluye la notificación del ticket creado, orden más reciente primero |
| 4 | Historial de un ticket inexistente (UUID aleatorio) | 200 (no 404) con arreglo vacío — así lo define el contrato |
| 5 | `ticketId` con formato inválido | 400 con `mensaje` |

## Qué se implementó y qué se dejó fuera a propósito

**Incluido (está en el contrato):**
- El único endpoint: `GET /api/notificaciones/ticket/{ticketId}`
- El schema `Notificacion` exacto (id, ticketId, mensaje, fechaEnvio)
- Orden de más reciente a más antigua (como pide la descripción del endpoint)
- `ticketId` como columna UUID normal, **sin FK real** hacia ticket-service — tal como exige el contrato explícitamente
- Persistencia real en PostgreSQL
- Documentación Swagger UI generada en vivo

**Dejado fuera a propósito (no está en el contrato REST):**
- Cualquier endpoint de escritura (POST/PUT/DELETE) — el contrato solo define lectura
- Respuesta 404 — un `ticketId` sin resultados simplemente da 200 con arreglo vacío (un `ticketId` con formato inválido sí da 400)
