package com.agilti.notificationservice.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agilti.notificationservice.model.Notificacion;
import com.agilti.notificationservice.repository.NotificacionRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba de integracion: levanta el contexto completo con H2 en memoria
 * (perfil "test") y verifica el unico endpoint del contrato. Ya no hay
 * data.sql (las notificaciones llegan por Kafka), asi que cada prueba
 * inserta sus propios datos con el repositorio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificacionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Test
    void historialPorTicket_conNotificaciones_devuelve200ConDatos() throws Exception {
        UUID ticketId = UUID.randomUUID();
        notificacionRepository.save(new Notificacion(ticketId, "Primera notificacion"));
        notificacionRepository.save(new Notificacion(ticketId, "Segunda notificacion"));

        mockMvc.perform(get("/api/notificaciones/ticket/" + ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].ticketId").value(ticketId.toString()));
    }

    @Test
    void historialPorTicket_sinNotificaciones_devuelve200ConArregloVacio() throws Exception {
        mockMvc.perform(get("/api/notificaciones/ticket/99999999-9999-9999-9999-999999999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listarNotificaciones_devuelvePaginaConMetadatos() throws Exception {
        notificacionRepository.save(new Notificacion(UUID.randomUUID(), "Notificacion A"));
        notificacionRepository.save(new Notificacion(UUID.randomUUID(), "Notificacion B"));

        mockMvc.perform(get("/api/notificaciones").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.previousPage").value(false))
                .andExpect(jsonPath("$.nextPage").value(true));
    }

    @Test
    void historialPorTicket_idInvalido_devuelve400() throws Exception {
        mockMvc.perform(get("/api/notificaciones/ticket/no-es-un-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists());
    }
}
