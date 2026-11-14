# AgilTI — Sistema de Helpdesk con microservicios

Proyecto de aula de **Programación Avanzada** · Ingeniería de Software, 8.º semestre · Fundación de Estudios Superiores FET, Neiva
**Entrega final** · Grupo G1 – Equipo 4 · Docente tutor: Diego Fernando Vargas Ramírez

AgilTI permite registrar tickets de soporte, asignarlos automáticamente a un técnico, seguir su estado y consultar las notificaciones generadas. Está construido con microservicios que se comunican por eventos (Kafka) y por un gateway GraphQL, y **se levanta completo con un solo comando**.

## Integrantes

| Integrante | Rol |
|---|---|
| Nicolás Andrés Chica Córdoba | Desarrollo |
| Juan Carlos Bastidas Bernal | Desarrollo |
| Juan Pablo Cubides Santos | Documentación |
| Kevin Stick Artunduaga Narváez | Diseño |
| Nycolas Jamevit Cabrera Perdomo | Pruebas (tester) |

---

## Contenido

1. [Arranque rápido](#1-arranque-rápido)
2. [Tecnologías y versiones](#2-tecnologías-y-versiones)
3. [Arquitectura](#3-arquitectura)
4. [URLs y puertos](#4-urls-y-puertos)
5. [Estado de implementación](#5-estado-de-implementación)
6. [Reglas de negocio](#6-reglas-de-negocio)
7. [Pruebas](#7-pruebas)
8. [Frontend](#8-frontend)
9. [GraphQL Gateway](#9-graphql-gateway)
10. [Estructura del repositorio](#10-estructura-del-repositorio)
11. [Documentación adicional](#11-documentación-adicional)

---

## 1. Arranque rápido

**Requisito único:** Docker Desktop instalado y en ejecución. No hace falta tener Java, Gradle ni Node instalados.

```bash
git clone https://github.com/nicolaschica/AGILTI.git
cd AGILTI
docker compose up --build
```

Este comando:

1. Levanta **PostgreSQL** con dos bases de datos independientes (`ticket_db` y `notification_db`), creadas por `docker/init-db.sql`.
2. Levanta **ZooKeeper, Kafka y Kafka UI**.
3. Construye `ticket-service`, `notification-service` y `graphql-gateway` **dentro de sus contenedores** con la imagen oficial de Gradle (no depende de que tu máquina tenga Gradle ni del archivo `gradle-wrapper.jar`).
4. Construye el frontend y lo sirve con nginx.

> La primera vez puede tardar varios minutos porque Gradle y npm descargan dependencias dentro de los contenedores.

**Cómo comprobar que todo arrancó:** `docker compose ps` debe mostrar **8 contenedores** en ejecución (postgres, zookeeper, kafka, kafka-ui, ticket-service, notification-service, graphql-gateway y frontend). Luego abre http://localhost:3000.

**Cómo apagar:**

```bash
docker compose down        # detiene y elimina los contenedores
docker compose down -v     # además borra los datos de la base
```

> Si algún puerto de la sección 4 está ocupado en tu equipo, libéralo antes de arrancar.

---

## 2. Tecnologías y versiones

| Componente | Tecnología | Versión |
|---|---|---|
| Lenguaje backend | Java | 21 |
| Framework backend | Spring Boot (Spring Data JPA, Bean Validation, Spring for GraphQL) | 3.3.4 |
| Construcción | Gradle | 8.10 |
| Documentación REST | springdoc-openapi (Swagger UI) | — |
| Base de datos | PostgreSQL | 15 |
| Mensajería | Apache Kafka y ZooKeeper (imágenes Confluent) | 7.6.0 |
| Inspección de Kafka | Kafka UI | — |
| Frontend | React + Vite (JavaScript) | — |
| Frontend en modo desarrollo | Node.js | 18 o superior |
| Contenedores | Docker Compose | — |
| Pruebas de API | Postman (3 colecciones) | — |

Versiones de los contratos: `ticket-service` **v3.4.1**, `notification-service` **v1.2.0**, `graphql-gateway` **v1.0.0**.

---

## 3. Arquitectura

```text
            Navegador
                |
                v
   +--------------------------+
   |  Frontend React  :3000   |
   +------------+-------------+
                | POST /graphql
                v
   +--------------------------+
   |  graphql-gateway  :8082  |
   +-----+-------------+------+
         | REST        | REST
         v             v
+----------------+  +------------------------+
| ticket-service |  | notification-service   |
|     :8080      |  |        :8081           |
+---+--------+---+  +-----+-----------+------+
    |        |            ^           |
    |        | publica    | consume   |
    |        v            |           |
    |   +------------------------+    |
    |   | Kafka: ticket-creado   |    |
    |   +------------------------+    |
    v                                 v
+-----------+                 +-----------------+
| ticket_db |                 | notification_db |
+-----------+                 +-----------------+
 \_____ PostgreSQL 15: una instancia, dos bases _____/
```

**Flujo de creación de un ticket**

1. El front envía la mutation `crearTicket` **solo al gateway**.
2. El gateway la reenvía a `ticket-service`, que valida los datos, genera el UUID, deja el ticket en `ABIERTO`, asigna el técnico por round-robin y lo guarda en `ticket_db`.
3. De forma asíncrona, `ticket-service` publica el evento `ticket-creado` en Kafka.
4. `notification-service` consume el evento y guarda la notificación en `notification_db`.
5. El front consulta tickets y notificaciones únicamente a través del gateway.

| Componente | Responsabilidad |
|---|---|
| `frontend` | Interfaz web. Solo se conecta al gateway. |
| `graphql-gateway` | Única puerta de entrada. Expone queries y mutations GraphQL, delega por REST y traduce errores a códigos. |
| `ticket-service` | Crea y consulta tickets, asigna técnico, aplica reglas de estado, valida y publica el evento. Dueño de `ticket_db`. |
| `notification-service` | Consume el evento, guarda la notificación y expone el historial por REST. Dueño de `notification_db`. |
| Kafka + ZooKeeper + Kafka UI | Bus de eventos (tópico `ticket-creado`) e inspección. |
| PostgreSQL | Una instancia con una base de datos por servicio. |

---

## 4. URLs y puertos

| Componente | URL |
|---|---|
| **Frontend** | http://localhost:3000 |
| Frontend (modo desarrollo, Vite) | http://localhost:5173 |
| **graphql-gateway** (lo único que usa el frontend) | http://localhost:8082/graphql |
| GraphiQL (servido localmente, sin CDN) | http://localhost:8082/graphiql-local |
| ticket-service — Swagger UI | http://localhost:8080/swagger-ui.html |
| notification-service — Swagger UI | http://localhost:8081/swagger-ui.html |
| Kafka UI | http://localhost:8090 |

`ticket-service` y `notification-service` quedan expuestos en 8080 y 8081 para Postman, Swagger y el propio gateway; el frontend **no** los usa.

---

## 5. Estado de implementación

Distinguimos lo implementado, lo parcial y lo pendiente. No se registran como implementadas funcionalidades que no lo están.

### Requisitos funcionales

| Requisito | Estado | Observación |
|---|---|---|
| RF-01 Crear ticket con título, descripción y prioridad | Implementado | Validación en front y backend. |
| RF-02 Listar (paginación y filtros por estado y prioridad) y consultar detalle | Implementado | |
| RF-03 Actualizar estado (ABIERTO → EN_ATENCION → CERRADO, sin retroceso) | Implementado | La transición inválida responde 400. |
| RF-04 Notificar al técnico asignado al crear el ticket | **Parcial** | La notificación se genera y se guarda en base de datos (**simulada**). No se envía por correo ni mensajería. |
| RF-05 Consultar y modificar tickets y notificaciones por GraphQL mediante el gateway | Implementado | |
| RF-06 Historial de notificaciones por ticket | Implementado | |
| RF-07 Asignación automática de técnico (round-robin) | Implementado | Conjunto fijo de tres técnicos. |
| RF-08 Interfaz web conectada únicamente al gateway | Implementado | |

### Requisitos no funcionales

| Requisito | Estado | Observación |
|---|---|---|
| RNF-01 Tolerancia a fallas con mensajes claros | **Parcial** | Verificado con `notification-service` detenido y con falla en el listado. No se probaron todas las combinaciones de caída. |
| RNF-02 Creación rápida (meta: menos de 500 ms) | **No medido formalmente** | Postman registró tiempos medios de 118 ms (ticket-service) y 94 ms (gateway) sobre toda la colección; no hay prueba de rendimiento específica. |
| RNF-03 No perder notificaciones si `notification-service` cae | **No verificado** | Depende de la retención de eventos de Kafka; la prueba (TP-17 del informe) no se ejecutó. |
| RNF-04 Despliegue con un solo comando | Implementado | `docker compose up --build`. |
| RNF-05 Documentación de endpoints | Implementado | Contratos OpenAPI y esquema GraphQL. |
| RNF-06 Bajo acoplamiento | Implementado | Base de datos por servicio; comunicación por eventos o por el gateway. |

### Resultados medidos, simulados y trabajo futuro

- **Medidos:** 157 pruebas automatizadas de Postman sin errores; consumer group estable con lag 0; tiempos medios de respuesta de Postman.
- **Simulados:** la notificación al técnico (solo se registra en base de datos) y los datos de prueba (técnicos y tickets ficticios).
- **Trabajo futuro (pendiente):** autenticación y autorización; envío real de notificaciones; evento de cambio de estado; métricas para el coordinador; pruebas de carga; Kafka con más particiones y replicación; bases de datos en instancias separadas.

### Limitaciones conocidas

- Al separar las bases de datos, la consistencia se resuelve con eventos y no con llaves foráneas; por eso pueden quedar notificaciones huérfanas si se borran tickets directamente en la base de datos.
- Kafka corre con una partición y replicación 1 (entorno académico).

---

## 6. Reglas de negocio

- **Título:** de 5 a 80 caracteres, con lista blanca de caracteres y al menos una letra.
- **Descripción:** mínimo 10 caracteres.
- **Prioridad:** valores definidos por el esquema (por ejemplo, `ALTA`).
- **Asignación de técnico:** round-robin entre tres técnicos de prueba (Carlos Perez, Ana Gomez, Luis Rodriguez), todos datos ficticios.
- **Estados:** `ABIERTO → EN_ATENCION → CERRADO`, sin retroceso ni saltos. Una transición inválida devuelve `BAD_REQUEST`.
- **Fuente de verdad de las validaciones:** `ticket-service` (Bean Validation). El frontend las replica en `frontend/src/validation/ticketRules.js` solo para avisar al instante.

---

## 7. Pruebas

### Colecciones Postman (`postman/`)

Importar en Postman, con el sistema levantado:

| Colección | Pruebas |
|---|---|
| `ticket-service` | 50 |
| `notification-service` | 15 |
| `graphql-gateway` | 92 |
| **Total** | **157, sin errores** |

La colección del gateway crea un ticket, comprueba que los títulos y descripciones inválidos se rechazan con `BAD_REQUEST` y el mensaje exacto sin crear nada, espera la notificación vía Kafka y recorre las transiciones de estado.

### Pruebas unitarias y de integración (Gradle)

Dentro de cada carpeta de servicio (`ticket-service/`, `notification-service/`, `graphql-gateway/`):

```bash
gradle test
```

Requiere JDK 21 y Gradle 8.10 instalados en tu máquina (el wrapper `gradle-wrapper.jar` no viaja en el ZIP). Usan H2 en memoria (perfil `test`), así que **no necesitan** Docker ni PostgreSQL.

| Prueba | Qué cubre |
|---|---|
| `TicketServiceTest` | Asignación round-robin y transiciones de estado válidas e inválidas. |
| `TicketControllerIntegrationTest` | Los 4 endpoints de tickets contra H2 (201, 400, 404). |
| `NotificacionControllerIntegrationTest` | Endpoints de notificaciones, incluido arreglo vacío y 400 por formato inválido. |

### Evidencias

La matriz de pruebas (TP-01 a TP-17) y las capturas de funcionamiento están en el **Informe final** (Anexos A y B). El caso TP-17 figura como **no ejecutado**.

---

## 8. Frontend

Interfaz en React + Vite dentro de `frontend/`. Todas sus peticiones son `POST http://localhost:8082/graphql` hechas con `fetch` (sin librerías GraphQL). No conoce las URLs de `ticket-service` ni de `notification-service` y no se conecta a Kafka.

| Pantalla | Ruta | Qué hace |
|---|---|---|
| Tickets | `/` | Tabla paginada con filtros por estado y prioridad, tamaño de página y botones Anterior/Siguiente. |
| Crear ticket | `/tickets/nuevo` | Formulario con validación al escribir y al salir de cada campo; el botón se deshabilita mientras haya errores; conserva lo escrito si el envío falla; muestra el técnico asignado. |
| Detalle | `/tickets/:id` | Todos los campos, botón con **solo** la siguiente transición válida y las notificaciones del ticket. |
| Notificaciones | `/notificaciones` | Listado paginado de las notificaciones generadas por el evento `ticket-creado`, con enlace al ticket y botón "Actualizar". |

**Errores amigables:** el frontend nunca muestra el texto crudo del gateway ni de los servicios. `frontend/src/api/errores.js` elige el mensaje según la pantalla y el código del error (`SERVICE_UNAVAILABLE`, `INTERNAL_ERROR`, red caída o timeout de 15 s, `NOT_FOUND`, `BAD_REQUEST`). Con `ticket-service` detenido, el listado muestra: *"No pudimos mostrar los tickets — Estamos teniendo un problema para cargar la información. Inténtalo de nuevo en unos segundos."*

**Textos largos:** títulos, descripciones y notificaciones parten palabras largas (`overflow-wrap: anywhere`); en el listado el título se recorta con puntos suspensivos y el texto completo queda en el tooltip.

**Con Docker (recomendado):** `docker compose up --build` levanta el contenedor `agilti-frontend` en http://localhost:3000. La URL del gateway se pasa como *build arg* (`VITE_GATEWAY_URL`) en `docker-compose.yml` y apunta a `localhost:8082`, porque la llama el navegador.

**Modo desarrollo (opcional):** requiere Node 18+ y el backend corriendo.

```bash
cd frontend
cp .env.example .env      # VITE_GATEWAY_URL=http://localhost:8082
npm install
npm run dev               # http://localhost:5173
```

`npm run build` genera la versión de producción en `frontend/dist/`.

**CORS:** solo el `graphql-gateway` permite `/graphql` desde `http://localhost:5173` y `http://localhost:3000`.

---

## 9. GraphQL Gateway

Requisito del docente: *"El Gateway debe hacer todas las peticiones, el front solo debe conectarse con el Gateway"*. El gateway expone queries y mutations GraphQL y delega por REST, sin duplicar lógica de negocio. No usa base de datos ni Kafka y no tiene Subscriptions.

| GraphQL | REST |
|---|---|
| `tickets`, `ticket` | ticket-service `GET /api/tickets`, `GET /api/tickets/{id}` |
| `crearTicket` (mutation) | ticket-service `POST /api/tickets` (asigna técnico y publica `ticket-creado`) |
| `actualizarEstadoTicket` (mutation) | ticket-service `PATCH /api/tickets/{id}/estado` |
| `notificaciones`, `notificacionesPorTicket` | notification-service `GET /api/notificaciones`, `GET /api/notificaciones/ticket/{ticketId}` |

- **Endpoint:** `POST http://localhost:8082/graphql`
- **Esquema:** `graphql-gateway/src/main/resources/graphql/schema.graphqls`
- **Errores** en `errors[].extensions.code`: `NOT_FOUND`, `BAD_REQUEST` (datos inválidos o transición no permitida), `SERVICE_UNAVAILABLE` (servicio caído o timeout de 4 s, configurable con `TICKET_SERVICE_TIMEOUT` y `NOTIFICATION_SERVICE_TIMEOUT`) e `INTERNAL_ERROR`.

Ejemplo:

```bash
curl -X POST http://localhost:8082/graphql -H "Content-Type: application/json" \
  -d '{"query":"mutation { crearTicket(titulo: \"Impresora\", descripcion: \"No imprime\", prioridad: ALTA) { id estado tecnicoAsignado } }"}'
```

Más ejemplos en [`graphql-gateway/README.md`](graphql-gateway/README.md).

### Endpoints REST de los servicios

| Servicio | Endpoint |
|---|---|
| ticket-service | `GET /api/tickets?estado=&prioridad=&page=&size=` (paginado: `content`, `page`, `size`, `totalElements`, `totalPages`) |
| ticket-service | `GET /api/tickets/{id}` · `POST /api/tickets` · `PATCH /api/tickets/{id}/estado` |
| notification-service | `GET /api/notificaciones?page=&size=` (más reciente primero) |
| notification-service | `GET /api/notificaciones/ticket/{ticketId}` |

Las notificaciones **nunca** se crean por REST: solo las crea el consumidor Kafka.

### Evento Kafka

Tópico `ticket-creado` (1 partición, replicación 1), con el UUID del ticket como clave y consumer group `notification-service-group`. Campos del evento: `eventoId`, `tipoEvento` (`TICKET_CREADO`), `fechaEvento` (epoch en segundos), `ticketId`, `titulo`, `prioridad`, `tecnicoAsignado`.

---

## 10. Estructura del repositorio

```
.
├── docker-compose.yml          # levanta TODO con un solo comando
├── docker/init-db.sql          # crea ticket_db y notification_db
├── openapi/
│   ├── ticket-service-openapi.yaml        # v3.4.1: filtros, paginación, validaciones, evento Kafka
│   ├── notification-service-openapi.yaml  # v1.2.0: solo lectura
│   └── graphql-gateway-openapi.yaml       # v1.0.0: transporte POST /graphql (el contrato real es schema.graphqls)
├── postman/                    # 3 colecciones listas para importar
├── ticket-service/             # proyecto Gradle (puerto 8080)
├── notification-service/       # proyecto Gradle (puerto 8081)
├── graphql-gateway/            # proyecto Gradle (puerto 8082)
└── frontend/                   # React + Vite (3000 en Docker, 5173 en desarrollo)
```

---

## 11. Documentación adicional

- **Informe final** (formato FO-GI-10): objetivos, metodología, resultados, matriz de pruebas, presupuesto y referencias. Anexo A: evidencias de funcionamiento.
- **Contratos OpenAPI:** carpeta [`openapi/`](openapi/).
- **Esquema GraphQL:** `graphql-gateway/src/main/resources/graphql/schema.graphqls`.
- **README por servicio:** dentro de cada carpeta.

**Versión entregada:** commit «entrega final» de este repositorio, idéntico al contenido del archivo ZIP de la entrega.#