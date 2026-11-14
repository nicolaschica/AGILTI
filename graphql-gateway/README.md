> **Nota:** para la entrega usa el `docker-compose.yml` de la raíz (`docker compose up --build`), que levanta este gateway junto con ticket-service, notification-service y el resto del sistema. Lo que sigue es la guía para correrlo de forma aislada.

---

# graphql-gateway (Gradle) — única puerta de entrada del frontend

Servicio Java 21 + Spring Boot 3.3.4 + Gradle que expone un endpoint
**GraphQL** con queries y mutations. Es el **único backend con el que habla
el frontend**: el navegador solo hace `POST http://localhost:8082/graphql`.
El gateway no tiene base de datos, ni Kafka, ni lógica de negocio: cada
campo se resuelve llamando por REST al microservicio dueño del dato.

| Operación GraphQL | Llamada REST |
|---|---|
| `query tickets(estado, prioridad, page, size)` | ticket-service `GET /api/tickets?estado=&prioridad=&page=&size=` (los filtros nulos no se envían) |
| `query ticket(id)` | ticket-service `GET /api/tickets/{id}` |
| `mutation crearTicket(titulo, descripcion, prioridad)` | ticket-service `POST /api/tickets` |
| `mutation actualizarEstadoTicket(id, estado)` | ticket-service `PATCH /api/tickets/{id}/estado` |
| `query notificaciones(page = 0, size = 20)` | notification-service `GET /api/notificaciones?page=&size=` |
| `query notificacionesPorTicket(ticketId)` | notification-service `GET /api/notificaciones/ticket/{ticketId}` |

- Las validaciones, la asignación de técnico, la publicación del evento Kafka
  `ticket-creado` y las reglas de transición de estado siguen en ticket-service;
  el gateway solo delega y traduce errores.
- No define `Subscription`.
- Esquema (contrato real): [`src/main/resources/graphql/schema.graphqls`](src/main/resources/graphql/schema.graphqls).
  Tipos: `Ticket`, `TicketPage`, `Notificacion` (`id`, `ticketId`, `mensaje`, `fechaEnvio`),
  `NotificacionPage`, enums `Estado` y `Prioridad`.
- OpenAPI: [`openapi/graphql-gateway-openapi.yaml`](../openapi/graphql-gateway-openapi.yaml) documenta
  únicamente el transporte HTTP (`POST /graphql`, siempre 200, `errors[].extensions.code`) para
  herramientas basadas en OpenAPI (Postman, mocks); no reemplaza el esquema GraphQL como contrato.

## URLs (puerto 8082)

| URL | Qué es |
|---|---|
| `POST http://localhost:8082/graphql` | Endpoint GraphQL (body JSON `{"query": "...", "variables": {...}}`) |
| http://localhost:8082/graphiql-local | GraphiQL, explorador interactivo en el navegador (funciona sin internet) |
| http://localhost:8082/graphiql | Redirige a `/graphiql-local` (el GraphiQL integrado de Spring está desactivado) |
| http://localhost:8082/actuator/health | Health check |

CORS habilitado en `/graphql` para `http://localhost:5173` y `http://localhost:3000` (el frontend).

## Configuración

| Propiedad (`application.yml`) | Variable de entorno | Por defecto |
|---|---|---|
| `agilti.ticket-service.url` | `TICKET_SERVICE_URL` | `http://localhost:8080` |
| `agilti.ticket-service.timeout` | `TICKET_SERVICE_TIMEOUT` | `4s` (conexión y lectura) |
| `agilti.notification-service.url` | `NOTIFICATION_SERVICE_URL` | `http://localhost:8081` |
| `agilti.notification-service.timeout` | `NOTIFICATION_SERVICE_TIMEOUT` | `4s` (conexión y lectura) |

En Docker Compose: `TICKET_SERVICE_URL=http://ticket-service:8080` y
`NOTIFICATION_SERVICE_URL=http://notification-service:8081` (nombres de servicio en la red `agilti-net`).

## Cómo correrlo sin Docker

Requiere ticket-service en `localhost:8080` y notification-service en `localhost:8081`.
Igual que en los otros servicios, falta `gradle-wrapper.jar` (ver README de ticket-service):

```bash
gradle bootRun      # o ./gradlew bootRun si ya generaste el wrapper
gradle test         # pruebas: no necesitan ningún servicio levantado
```

## Ejemplos

```graphql
# Primera página (page=0, size=10 por defecto)
{
  tickets {
    content { id titulo prioridad estado tecnicoAsignado fechaCreacion }
    page size totalElements totalPages nextPage previousPage
  }
}

# Filtros + paginación
{
  tickets(estado: ABIERTO, prioridad: ALTA, page: 0, size: 5) {
    content { id titulo }
    totalElements
  }
}

# Detalle por id
query ($id: ID!) {
  ticket(id: $id) { id titulo descripcion prioridad estado tecnicoAsignado fechaCreacion }
}

# Crear ticket (ticket-service asigna el técnico y publica ticket-creado en Kafka)
mutation {
  crearTicket(titulo: "No enciende el computador", descripcion: "Área contable", prioridad: ALTA) {
    id estado tecnicoAsignado fechaCreacion
  }
}

# Cambiar estado (solo ABIERTO -> EN_ATENCION -> CERRADO)
mutation ($id: ID!) {
  actualizarEstadoTicket(id: $id, estado: EN_ATENCION) { id estado }
}

# Notificaciones de un ticket (llegan unos segundos después de crearlo, vía Kafka)
query ($id: ID!) {
  notificacionesPorTicket(ticketId: $id) { id ticketId mensaje fechaEnvio }
}

# Todas las notificaciones, paginadas (más reciente primero)
{
  notificaciones(page: 0, size: 5) {
    content { id ticketId mensaje fechaEnvio }
    page size totalElements totalPages nextPage previousPage
  }
}
```

Con curl:

```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"mutation { crearTicket(titulo: \"Impresora\", descripcion: \"No imprime\", prioridad: MEDIA) { id tecnicoAsignado } }"}'
```

Con PowerShell:

```powershell
$body = @{ query = '{ notificaciones(size: 3) { totalElements content { ticketId mensaje } } }' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8082/graphql -ContentType 'application/json' -Body $body
```

## Errores

Los errores llegan con HTTP 200 en el arreglo `errors` (convención GraphQL),
con un mensaje legible y **sin stack trace**, tanto en queries como en mutations.
El código está en `extensions.code`:

| Situación | `extensions.code` | Mensaje |
|---|---|---|
| el servicio responde 404 (ticket inexistente) | `NOT_FOUND` | el `mensaje` del servicio, p. ej. `No se encontro el ticket con id ...` |
| el servicio responde 400 (id no UUID, título/descripción que no cumplen las reglas de ticket-service, transición inválida, page/size inválidos) | `BAD_REQUEST` | el `mensaje` del servicio, p. ej. `titulo: Escribe un título de al menos 5 caracteres.` |
| servicio caído o sin respuesta dentro del timeout | `SERVICE_UNAVAILABLE` | `ticket-service no esta disponible o no respondio a tiempo` (o `notification-service ...`) |
| otro error HTTP del servicio (5xx) | `INTERNAL_ERROR` | mensaje genérico |

Ejemplo:

```json
{
  "errors": [{
    "message": "No se encontro el ticket con id 00000000-0000-0000-0000-000000000000",
    "path": ["ticket"],
    "extensions": { "code": "NOT_FOUND", "classification": "NOT_FOUND" }
  }],
  "data": { "ticket": null }
}
```

Si falta un argumento obligatorio o un enum es inválido, GraphQL rechaza la
operación antes de llamar a ningún servicio (error de validación).

## Pruebas

- `TicketQueryControllerTest` (`@GraphQlTest` + `GraphQlTester`, cliente simulado): mapeo de `TicketPage`,
  filtros, ticket por id, `NOT_FOUND` y `SERVICE_UNAVAILABLE`.
- `TicketMutationControllerTest`: `crearTicket` y `actualizarEstadoTicket` delegan los argumentos, y
  `BAD_REQUEST` (datos inválidos, transición inválida), `NOT_FOUND`, `SERVICE_UNAVAILABLE` y argumento faltante.
- `NotificacionQueryControllerTest`: `notificaciones` (valores por defecto y paginación),
  `notificacionesPorTicket`, `BAD_REQUEST`, `NOT_FOUND` y `SERVICE_UNAVAILABLE`.
- `TicketServiceClientTest` / `NotificationServiceClientTest` (`MockRestServiceServer`): método, URL,
  query params y cuerpo JSON enviados a cada servicio, mapeo de las respuestas y error envuelto con el nombre del servicio.
- `GraphqlGatewayApplicationTests`: el contexto arranca y el esquema cuadra con los `@QueryMapping`/`@MutationMapping`.
- `GraphiqlLocalConfigTest` (`MockMvc`): `/graphiql-local` y sus archivos JS/CSS se sirven desde el servicio, sin CDN.

El `Dockerfile` construye con `-x test`, así que las pruebas nunca rompen el build de Docker.

Postman: `postman/AgilTI-graphql-gateway.postman_collection.json` (carpeta **GraphQL Gateway**, 15 peticiones:
crear ticket, listar/filtrar/paginar, detalle, notificaciones, transiciones de estado y cada código de error).

## Estructura

```
graphql-gateway/
├── build.gradle / settings.gradle / Dockerfile
└── src/main/java/com/agilti/graphqlgateway/
    ├── client/TicketServiceClient.java         # REST a ticket-service (GET, POST, PATCH)
    ├── client/NotificationServiceClient.java   # REST a notification-service (GET)
    ├── client/ServicioRemotoException.java     # error de RestClient + nombre del servicio
    ├── config/RestClientConfig.java            # un RestClient por servicio, con URL base y timeout
    ├── config/TicketServiceProperties.java     # agilti.ticket-service.*
    ├── config/NotificationServiceProperties.java  # agilti.notification-service.*
    ├── controller/TicketQueryController.java   # @QueryMapping tickets / ticket
    ├── controller/TicketMutationController.java   # @MutationMapping crearTicket / actualizarEstadoTicket
    ├── controller/NotificacionQueryController.java # @QueryMapping notificaciones / notificacionesPorTicket
    ├── controller/GraphQlExceptionResolver.java   # errores -> extensions.code
    ├── dto/ (TicketDto, TicketPageDto, NotificacionDto, NotificacionPageDto, ErrorResponse)
    └── model/ (Estado, Prioridad)
```
