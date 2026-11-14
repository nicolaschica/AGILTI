package com.agilti.ticketservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica que el contexto de Spring arranca sin errores de configuracion
 * (beans mal cableados, propiedades faltantes, etc.). Usa el perfil "test"
 * con H2 en memoria para no depender de PostgreSQL al correr `./gradlew test`.
 */
@SpringBootTest
@ActiveProfiles("test")
class TicketServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
