package com.acantilado.core.resources.openapi;

import jakarta.servlet.annotation.WebServlet;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;


@WebServlet(urlPatterns = "/openapi.json")
public class SwaggerServlet extends OpenApiResource {
    // In most cases you don’t need to override anything
}

