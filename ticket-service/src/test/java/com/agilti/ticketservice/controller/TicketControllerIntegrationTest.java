package com.agilti.ticketservice.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agilti.ticketservice.messaging.TicketEventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba de integracion real: levanta el contexto completo de Spring
 * (con H2 en memoria, perfil "test") y hace peticiones HTTP de verdad
 * contra los endpoints, verificando persistencia, validaciones y codigos
 * HTTP de extremo a extremo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Sin broker Kafka en las pruebas: se simula el productor para que
    // POST /api/tickets no quede esperando metadatos del broker.
    @MockBean
    private TicketEventProducer ticketEventProducer;

    @Test
    void crearTicket_devuelve201ConTecnicoAsignado() throws Exception {
        String body = """
                {
                  "titulo": "No enciende el computador",
                  "descripcion": "Equipo de contabilidad",
                  "prioridad": "ALTA"
                }
                """;

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.estado").value("ABIERTO"))
                .andExpect(jsonPath("$.tecnicoAsignado", notNullValue()));
    }

    @Test
    void crearTicket_sinTitulo_devuelve400() throws Exception {
        String body = """
                {
                  "descripcion": "Falta el titulo a proposito",
                  "prioridad": "MEDIA"
                }
                """;

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje", notNullValue()));
    }

    @Test
    void crearTicket_jsonMalFormado_devuelve400ConMensaje() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"sin cerrar\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("JSON mal formado en el cuerpo de la peticion"));
    }

    @Test
    void crearTicket_prioridadInexistente_devuelve400ConMensaje() throws Exception {
        String body = """
                {
                  "titulo": "Prioridad fuera del enum",
                  "descripcion": "Prueba",
                  "prioridad": "URGENTE"
                }
                """;

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Valor invalido para 'prioridad': URGENTE"));
    }

    @Test
    void obtenerTicket_inexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/api/tickets/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje", notNullValue()));
    }

    @Test
    void listarTickets_devuelve200ConEstructuraPaginada() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements", notNullValue()));
    }

    @Test
    void flujoCompleto_crearYCambiarEstado() throws Exception {
        String body = """
                {
                  "titulo": "Impresora sin conexion",
                  "descripcion": "No imprime en el piso 3",
                  "prioridad": "MEDIA"
                }
                """;

        String responseJson = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(responseJson).get("id").asText();

        // Transicion valida: ABIERTO -> EN_ATENCION
        mockMvc.perform(patch("/api/tickets/" + id + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"EN_ATENCION\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_ATENCION"));

        // Transicion invalida: EN_ATENCION -> ABIERTO (retroceso, no permitido)
        mockMvc.perform(patch("/api/tickets/" + id + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"ABIERTO\"}"))
                .andExpect(status().isBadRequest());
    }
}
