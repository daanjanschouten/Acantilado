package com.acantilado.gathering.administration;

import com.fasterxml.jackson.databind.JsonNode;
import com.acantilado.core.administrative.Ayuntamiento;
import com.acantilado.core.administrative.ComunidadAutonoma;
import com.acantilado.core.administrative.Provincia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public final class AyuntamientoCollector extends OpenDataSoftCollector<Ayuntamiento> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AyuntamientoCollector.class);
    private static final String DATASET_NAME = "georef-spain-municipio";

    public AyuntamientoCollector() {
        super(DATASET_NAME);
    }

    @Override
    protected Optional<Ayuntamiento> constructObject(JsonNode jsonNode) {
        try {
            final long provinciaId = jsonNode.get("prov_code").asLong();
            Provincia provincia = new Provincia(
                    provinciaId,
                    jsonNode.get("prov_name").textValue()
            );

            ComunidadAutonoma comunidadAutonoma = new ComunidadAutonoma(
                    jsonNode.get("acom_code").asLong(),
                    jsonNode.get("acom_name").textValue()
            );

            Ayuntamiento ayuntamiento = new Ayuntamiento(
                    jsonNode.get("mun_code").asLong(),
                    jsonNode.get("mun_name").textValue(),
                    provinciaId,
                    provincia,
                    comunidadAutonoma);

            return Optional.of(ayuntamiento);
        } catch (Exception e) {
            LOGGER.info("Failed to construct Ayuntamiento: {}", jsonNode);
            return Optional.empty();
        }
    }
}