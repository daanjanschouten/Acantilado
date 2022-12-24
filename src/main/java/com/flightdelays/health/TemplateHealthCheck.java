package com.flightdelays.health;

import com.codahale.metrics.health.HealthCheck;
import com.flightdelays.core.Template;

import java.util.Optional;

public class TemplateHealthCheck extends HealthCheck {
    private final Template template;

    public TemplateHealthCheck(Template template) {
        this.template = template;
    }

    @Override
    protected Result check() {
        template.render(Optional.of("woo"));
        template.render(Optional.empty());
        return Result.healthy();
    }
}
