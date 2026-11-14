package com.agilti.notificationservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica que el contexto de Spring arranca sin errores de configuracion.
 * Usa el perfil "test" con H2 en memoria para no depender de PostgreSQL.
 */
@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
