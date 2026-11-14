> **Nota (Entrega Q10):** ahora existe un `docker-compose.yml` en la carpeta raiz del ZIP que levanta este servicio junto con el otro microservicio y PostgreSQL, todo con un solo comando: `docker compose up --build` desde la raiz. Lo que sigue abajo es la guia para correr ESTE servicio de forma aislada (util para desarrollo), pero para la entrega usa el compose de la raiz.

---

# ticket-service (Gradle) — implementación estricta del contrato OpenAPI

Microservicio REST construido en Java 21 + Spring Boot 3 + Gradle, **basado
únicamente en `ticket-service-openapi.yaml`**. No incluye Kafka ni ningún
endpoint/campo que no esté en el contrato — el propio contrato aclara que
la publicación a Kafka "no se documenta aquí porque no es un contrato REST".

## ⚠️ Antes de correrlo: falta el JAR del wrapper de Gradle

Este entorno no tuvo acceso a los servidores de Gradle para descargar
`gradle/wrapper/gradle-wrapper.jar` (es un binario). Sin él, `./gradlew` no
va a funcionar todavía. Tienes 3 opciones, elige la que te resulte más fácil:

**Opción A — la más simple si usas IntelliJ IDEA:**
Abre la carpeta del proyecto directamente en IntelliJ (`File > Open`,
selecciona `build.gradle`). IntelliJ detecta que falta el wrapper y se
ofrece a descargar Gradle automáticamente — no necesitas hacer nada más.

**Opción B — si ya tienes Gradle instalado en tu equipo:**
```bash
gradle wrapper --gradle-version 8.10
```
Esto genera el `gradle-wrapper.jar` que falta, dentro de la misma carpeta.
Después ya puedes usar `./gradlew` normalmente.

**Opción C — sin wrapper, usando tu Gradle global:**
```bash
gradle bootRun
```

## Cómo correrlo

**1. Levanta PostgreSQL** (solo la base de datos, no hace falta Docker Compose completo):
```bash
docker run -d --name ticket-postgres \
  -e POSTGRES_USER=agilti \
  -e POSTGRES_PASSWORD=agilti_pass \
  -e POSTGRES_DB=ticket_db \
  -p 5432:5432 \
  postgres:15
```

**2. Corre la aplicación:**
```bash
./gradlew bootRun
```
(o `gradle bootRun` / el botón "Run" de tu IDE sobre `TicketServiceApplication`)

**3. Confirma que arrancó:**
```
http://localhost:8080/swagger-ui.html   -> documentacion interactiva
http://localhost:8080/actuator/health   -> {"status":"UP"}
```

## Probar con Postman

1. Abre Postman → **Import** → arrastra los dos archivos de la carpeta `postman/`:
   - `AgilTI-ticket-service.postman_collection.json`
   - `AgilTI-local.postman_environment.json`
2. Selecciona el entorno **"AgilTI - Local"** (arriba a la derecha).
3. Corre las peticiones **en orden** (la petición 1 guarda automáticamente
   el `id` del ticket creado en la variable `{{ticketId}}`, que usan las
   peticiones 4, 6, 7 y 8 — por eso el orden importa).

| # | Petición | Qué valida |
|---|---|---|
| 1 | Crear ticket | 201, técnico asignado automáticamente, estado ABIERTO |
| 2 | Crear ticket sin título | 400, `titulo: El título es obligatorio.` |
| 2.1 – 2.12 | Validación de título y descripción | 9 casos inválidos (400 con el mensaje exacto de cada regla y sin crear ningún ticket) y 3 válidos (201): ver tabla de reglas abajo |
| 3 | Listar tickets paginados | 200, incluye los 7 campos de paginación (`content`, `page`, `size`, `totalElements`, `totalPages`, `nextPage`, `previousPage`); `content` es un arreglo y `nextPage`/`previousPage` son booleanos |
| 4 | Consultar por id | 200, el id coincide |
| 5 | Consultar id inexistente | 404, mensaje con el id |
| 6 | Cambiar a EN_ATENCION | 200, transición válida |
| 7 | Intentar volver a ABIERTO | 400, retroceso no permitido |
| 8 | Cambiar a CERRADO | 200, transición válida |

También puedes correr toda la colección de una vez con el botón **Run**
(Collection Runner) de Postman.

## Reglas de validación al crear (`POST /api/tickets`)

Bean Validation en `TicketCreateRequest` (`@NotBlank`, `@Size`, `@Pattern`) más
`NormalizadorTicket`, que se aplica en los setters del DTO (para validar el valor
ya normalizado) y otra vez en `TicketService` antes de guardar. Actualizar el estado
no cambia.

| Campo | Regla | Respuesta 400 (`mensaje`) |
|---|---|---|
| titulo | ausente, vacío o solo espacios | `titulo: El título es obligatorio.` |
| titulo | algún carácter fuera de la lista blanca, o sin ninguna letra | `titulo: El título contiene caracteres no permitidos. Usa solo letras, números y signos básicos como . , - ( ).` |
| titulo | menos de 5 caracteres (normalizado) | `titulo: Escribe un título de al menos 5 caracteres.` |
| titulo | más de 80 caracteres (normalizado) | `titulo: El título es muy largo. Resúmelo en pocas palabras (máximo 80 caracteres) y deja los detalles en la descripción.` |
| descripcion | ausente, vacía o solo espacios | `descripcion: La descripción es obligatoria.` |
| descripcion | menos de 10 caracteres tras recortar | `descripcion: La descripción debe tener al menos 10 caracteres. Cuéntanos un poco más sobre el problema.` |

- **Normalización del título**: forma Unicode NFC, se recorta y los espacios repetidos se
  reducen a uno (`"  Red   caída "` se guarda como `"Red caída"`). La descripción solo se recorta.
- **Lista blanca del título**: letras `A-Z a-z` con tildes, `Ü/ü` y `Ñ/ñ`, dígitos, espacio y
  `. , : ; - _ ( ) / # ¿ ? ¡ !`. Regex:
  `^(?=.*[A-Za-zÁÉÍÓÚÜáéíóúüÑñ])[A-Za-zÁÉÍÓÚÜáéíóúüÑñ0-9 .,:;\-_()/#¿?¡!]+$`
- **Un solo mensaje por respuesta**, con orden fijo: título antes que descripción y, dentro de
  cada campo, obligatorio > caracteres no permitidos > longitud.
- El frontend replica exactamente estas reglas y mensajes en `frontend/src/validation/ticketRules.js`.
- Pruebas: `TicketValidacionIntegrationTest` (47 casos, incluido que ningún rechazo persiste un ticket).

## Qué se implementó y qué se dejó fuera a propósito

**Incluido (está en el contrato):**
- Los 4 endpoints exactos: `POST /api/tickets`, `GET /api/tickets`, `GET /api/tickets/{id}`, `PATCH /api/tickets/{id}/estado`
- Los 4 schemas exactos: `TicketCreateRequest`, `EstadoUpdateRequest`, `Ticket`, `ErrorResponse`
- Validaciones: `titulo`/`descripcion` obligatorios con reglas de contenido (ver *Reglas de validación al crear*), `prioridad`/`estado` como enum cerrado
- Persistencia real en PostgreSQL (JPA/Hibernate), id como UUID
- 400 y 404 con el formato exacto de `ErrorResponse` documentado
- Transiciones de estado válidas según la descripción del schema `Estado` (sin retrocesos)
- Documentación Swagger UI generada en vivo desde el código

**Dejado fuera a propósito (no está en el contrato REST):**
- El endpoint HTTP de publicación a Kafka del evento `ticket-creado` — no existe tal endpoint, es un efecto secundario de `POST /api/tickets`. El propio YAML documenta la estructura del evento (schema `TicketCreadoEvent`) solo como referencia para quien implemente `notification-service`, aclarando que no es parte del contrato REST (OpenAPI no describe transporte Kafka).
- Cualquier endpoint de `notification-service` o `graphql-gateway`
- Autenticación/autorización (no aparece en el contrato)

Si quieres que integre Kafka de vuelta (como en la versión completa que
armamos antes en Maven), dímelo y lo agrego como una capa aparte sin tocar
el contrato REST.
