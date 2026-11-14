package com.agilti.ticketservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agilti.ticketservice.messaging.TicketEventProducer;
import com.agilti.ticketservice.repository.TicketRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Reglas de validacion de POST /api/tickets (titulo y descripcion).
 * Cada caso invalido debe responder 400 con {"mensaje": "campo: texto"} y
 * NO debe persistir ningun ticket.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketValidacionIntegrationTest {

    private static final String DESCRIPCION_OK = "El equipo no enciende desde esta mañana.";

    private static final String MSG_CARACTERES = "titulo: El título contiene caracteres no permitidos. "
            + "Usa solo letras, números y signos básicos como . , - ( ).";
    private static final String MSG_LARGO = "titulo: El título es muy largo. Resúmelo en pocas palabras "
            + "(máximo 80 caracteres) y deja los detalles en la descripción.";
    private static final String MSG_CORTO = "titulo: Escribe un título de al menos 5 caracteres.";
    private static final String MSG_TITULO_VACIO = "titulo: El título es obligatorio.";
    private static final String MSG_DESC_CORTA = "descripcion: La descripción debe tener al menos 10 caracteres. "
            + "Cuéntanos un poco más sobre el problema.";
    private static final String MSG_DESC_VACIA = "descripcion: La descripción es obligatoria.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @MockBean
    private TicketEventProducer ticketEventProducer;

    // ── Titulo invalido ─────────────────────────────────────────────

    @Test
    void titulo_conMenorQue_rechazado() throws Exception {
        assertRechazado("Error <script> en login", DESCRIPCION_OK, MSG_CARACTERES);
    }

    @Test
    void titulo_soloMenorQueRepetido_rechazado() throws Exception {
        assertRechazado("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<", DESCRIPCION_OK, MSG_CARACTERES);
    }

    @Test
    void titulo_conAmpersand_rechazado() throws Exception {
        assertRechazado("Red & internet caidas", DESCRIPCION_OK, MSG_CARACTERES);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Precio > 100", "Llaves {abc}", "Arreglo [1]", "Ruta C:\\temp", "Tubo | aqui", "Potencia ^2",
            "Tilde ~ suelta", "Comilla `grave`", "Costo $100", "Avance 50%", "Producto * dos", "Igual = algo",
            "Suma + algo", "Correo a@b.co", "Comilla 'simple'", "Comilla \"doble\"",
            "Salto\nde linea", "Tab\taqui dentro", "Invisible\u200Baqui", "Control\u0007aqui"
    })
    void titulo_conCaracteresFueraDeLaListaBlanca_rechazado(String titulo) throws Exception {
        assertRechazado(titulo, DESCRIPCION_OK, MSG_CARACTERES);
    }

    @Test
    void titulo_de81Caracteres_rechazado() throws Exception {
        assertRechazado("a".repeat(81), DESCRIPCION_OK, MSG_LARGO);
    }

    @Test
    void titulo_largoYConCaracteresInvalidos_priorizaCaracteres() throws Exception {
        assertRechazado("<".repeat(200), DESCRIPCION_OK, MSG_CARACTERES);
    }

    @Test
    void titulo_de4Caracteres_rechazado() throws Exception {
        assertRechazado("Wifi", DESCRIPCION_OK, MSG_CORTO);
    }

    @Test
    void titulo_queQuedaCortoTrasNormalizarEspacios_rechazado() throws Exception {
        // 13 caracteres en bruto, pero normalizado queda "ab c" (4)
        assertRechazado("   ab    c   ", DESCRIPCION_OK, MSG_CORTO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "      "})
    void titulo_vacioOSoloEspacios_rechazado(String titulo) throws Exception {
        assertRechazado(titulo, DESCRIPCION_OK, MSG_TITULO_VACIO);
    }

    @Test
    void titulo_ausente_rechazado() throws Exception {
        assertRechazado(null, DESCRIPCION_OK, MSG_TITULO_VACIO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567", "2024 - 12", "#123 (45)", "...!!!???", "¿¡-_/#!?"})
    void titulo_soloNumerosOSimbolos_rechazado(String titulo) throws Exception {
        assertRechazado(titulo, DESCRIPCION_OK, MSG_CARACTERES);
    }

    // ── Titulo valido ───────────────────────────────────────────────

    @Test
    void titulo_conTildesYEnie_aceptado() throws Exception {
        crear("Configuración de cañón y pingüino ÁÉÍÓÚÜÑ", DESCRIPCION_OK)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Configuración de cañón y pingüino ÁÉÍÓÚÜÑ"));
    }

    @Test
    void titulo_conTodosLosSignosPermitidos_aceptado() throws Exception {
        String titulo = "¿Error #12? ¡Sí!: red, piso 3; (VPN) A-B_C/D.";
        crear(titulo, DESCRIPCION_OK)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value(titulo));
    }

    @Test
    void titulo_de80Caracteres_aceptado() throws Exception {
        crear("a".repeat(80), DESCRIPCION_OK).andExpect(status().isCreated());
    }

    @Test
    void titulo_seGuardaRecortadoYConEspaciosUnicos() throws Exception {
        crear("   Impresora    del   piso 3   ", DESCRIPCION_OK)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Impresora del piso 3"));
    }

    // ── Descripcion ─────────────────────────────────────────────────

    @Test
    void descripcion_de9Caracteres_rechazada() throws Exception {
        assertRechazado("Equipo sin red", "123456789", MSG_DESC_CORTA);
    }

    @Test
    void descripcion_de9CaracteresTrasRecortar_rechazada() throws Exception {
        assertRechazado("Equipo sin red", "   123456789   ", MSG_DESC_CORTA);
    }

    @Test
    void descripcion_de1Caracter_rechazada() throws Exception {
        assertRechazado("Equipo sin red", "a", MSG_DESC_CORTA);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "     "})
    void descripcion_vacia_rechazada(String descripcion) throws Exception {
        assertRechazado("Equipo sin red", descripcion, MSG_DESC_VACIA);
    }

    @Test
    void descripcion_de10Caracteres_aceptada() throws Exception {
        crear("Equipo sin red", "1234567890")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descripcion").value("1234567890"));
    }

    @Test
    void descripcion_sinListaDeCaracteres_aceptaCualquierSimbolo() throws Exception {
        crear("Equipo sin red", "Error <500> & {json}: 100% caido")
                .andExpect(status().isCreated());
    }

    // ── Prioridad entre campos ──────────────────────────────────────

    @Test
    void tituloYDescripcionInvalidos_reportaPrimeroElTitulo() throws Exception {
        assertRechazado("<<<<<", "a", MSG_CARACTERES);
    }

    // ── Utilidades ──────────────────────────────────────────────────

    private ResultActions crear(String titulo, String descripcion) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        if (titulo != null) {
            body.put("titulo", titulo);
        }
        body.put("descripcion", descripcion);
        body.put("prioridad", "MEDIA");
        return mockMvc.perform(post("/api/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private void assertRechazado(String titulo, String descripcion, String mensajeEsperado) throws Exception {
        long antes = ticketRepository.count();
        crear(titulo, descripcion)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value(mensajeEsperado));
        assertThat(ticketRepository.count()).as("no debe persistir ningun ticket").isEqualTo(antes);
    }
}
