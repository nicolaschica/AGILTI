package com.agilti.graphqlgateway.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica que /graphiql-local y todos sus archivos (GraphiQL, React,
 * ReactDOM) se sirven desde el propio servicio, sin referencias a CDN.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GraphiqlLocalConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void graphiqlLocalSirvePaginaSinCdn() throws Exception {
        mockMvc.perform(get("/graphiql-local"))
                .andExpect(forwardedUrl("/graphiql-local.html"));

        mockMvc.perform(get("/graphiql-local.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/webjars/graphiql/3.9.0/graphiql.min.js")))
                .andExpect(content().string(containsString("url: '/graphql'")))
                .andExpect(content().string(not(containsString("unpkg"))))
                .andExpect(content().string(not(containsString("https://"))));
    }

    @Test
    void archivosDeGraphiqlYReactEstanEnElServicio() throws Exception {
        for (String ruta : new String[] {
                "/webjars/graphiql/3.9.0/graphiql.min.js",
                "/webjars/graphiql/3.9.0/graphiql.min.css",
                "/webjars/react/18.3.1/umd/react.production.min.js",
                "/webjars/react-dom/18.3.1/umd/react-dom.production.min.js"}) {
            mockMvc.perform(get(ruta)).andExpect(status().isOk());
        }
    }

    @Test
    void graphiqlOriginalRedirigeALaVersionLocal() throws Exception {
        mockMvc.perform(get("/graphiql"))
                .andExpect(redirectedUrl("/graphiql-local"));
    }
}
