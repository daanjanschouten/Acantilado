package com.schouten.core.seeding.administration;

import com.fasterxml.jackson.databind.JsonNode;
import com.schouten.core.administrative.Ayuntamiento;
import com.schouten.core.administrative.ComunidadAutonoma;
import com.schouten.core.administrative.Provincia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AyuntamientoCollector extends OpenDataSoftCollector<Ayuntamiento> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AyuntamientoCollector.class);
    private static final String DATASET_NAME = "georef-spain-municipio";

    @Override
    public String getDatasetName() {
        return DATASET_NAME;
    }

    @Override
    protected Optional<Ayuntamiento> constructObject(JsonNode jsonNode) {
        try {
            Provincia provincia = new Provincia(
                    jsonNode.get("prov_code").asLong(),
                    jsonNode.get("prov_name").textValue()
            );

            ComunidadAutonoma comunidadAutonoma = new ComunidadAutonoma(
                    jsonNode.get("acom_code").asLong(),
                    jsonNode.get("acom_name").textValue()
            );

            Ayuntamiento ayuntamiento = new Ayuntamiento(
                    jsonNode.get("mun_code").asLong(),
                    jsonNode.get("mun_name").textValue(),
                    provincia,
                    comunidadAutonoma);
            // LOGGER.info("Successfully constructed Ayuntamiento: {}", ayuntamiento);

            return Optional.of(ayuntamiento);

        } catch (Exception e) {
            LOGGER.info("Failed to construct Ayuntamiento: {}", jsonNode);
            return Optional.empty();
        }
    }
}