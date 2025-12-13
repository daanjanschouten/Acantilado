package com.acantilado.core.resources.openapi;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import jakarta.servlet.annotation.WebServlet;

@WebServlet(urlPatterns = "/openapi.json")
public class SwaggerServlet extends OpenApiResource {
  // In most cases you don’t need to override anything
}
