package com.agilti.graphqlgateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifica que el contexto arranca (con los dos RestClient y clientes) y que
 * schema.graphqls coincide con los @QueryMapping/@MutationMapping. No necesita
 * ningun servicio levantado: el gateway solo los llama al resolver un campo.
 */
@SpringBootTest
class GraphqlGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
