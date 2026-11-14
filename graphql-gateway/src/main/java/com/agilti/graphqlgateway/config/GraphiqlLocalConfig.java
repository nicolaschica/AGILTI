package com.agilti.graphqlgateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Publica GraphiQL en /graphiql-local (static/graphiql-local.html, con JS y
 * CSS servidos desde los webjars del propio servicio). El /graphiql
 * integrado de Spring esta desactivado porque descarga GraphiQL desde
 * unpkg.com; esa ruta redirige aqui para no confundir.
 */
@Configuration
public class GraphiqlLocalConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/graphiql-local").setViewName("forward:/graphiql-local.html");
        registry.addRedirectViewController("/graphiql", "/graphiql-local");
    }
}
