package com.acantilado.export;

import com.acantilado.core.administrative.IdealistaLocationMapping;
import com.acantilado.core.administrative.IdealistaLocationMappingDAO;
import com.acantilado.core.administrative.Provincia;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.acantilado.utils.RetryableBatchedExecutor.executeCallableInSessionWithoutTransaction;
import static com.acantilado.utils.RetryableBatchedExecutor.executeRunnableInSessionWithTransaction;

public class LocationMappingMerchant {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocationMappingMerchant.class);

    private final IdealistaLocationMappingDAO mappingDAO;
    private final SessionFactory sessionFactory;

    private final ObjectMapper mapper;
    private static final String OUTPUT_DIRECTORY = "./acantilado-seeder/src/main/resources/mappings";

    public LocationMappingMerchant(IdealistaLocationMappingDAO mappingDAO, SessionFactory sessionFactory) {
        this.mappingDAO = mappingDAO;
        this.sessionFactory = sessionFactory;

        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void exportMappings(Provincia provincia) {
        Set<IdealistaLocationMapping> provinceMappings = executeCallableInSessionWithoutTransaction(
                sessionFactory, mappingDAO::findAll)
                .stream()
                .filter(mapping ->
                        mapping.getIdealistaLocationId().startsWith(provincia.getIdealistaLocationId()))
                .collect(Collectors.toSet());

        if (provinceMappings.isEmpty()) {
            LOGGER.error("No mappings found for province: {}", provincia.getName());
            return;
        }

        String filename = provincia.getIdealistaLocationId() + "_mappings.json";
        File outputFile = new File(OUTPUT_DIRECTORY, filename);

        try {
            mapper.writeValue(outputFile, provinceMappings);
            LOGGER.info("Exported {} mappings for province {} to {}",
                    provinceMappings.size(), provincia.getName(), outputFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write mappings to file: " + outputFile, e);
        }
    }

    /**
     * Import mappings for a province from disk.
     * @return true if mappings were imported, false if file doesn't exist
     */
    public boolean importMappings(Provincia provincia) {
        String filename = provincia.getIdealistaLocationId() + "_mappings.json";
        File inputFile = new File(OUTPUT_DIRECTORY, filename);

        if (!inputFile.exists()) {
            LOGGER.info("No mapping file found: {}", inputFile.getAbsolutePath());
            return false;
        }

        try {
            List<IdealistaLocationMapping> mappings = mapper.readValue(
                    inputFile,
                    new TypeReference<List<IdealistaLocationMapping>>() {}
            );

            if (mappings.isEmpty()) {
                LOGGER.warn("Mapping file is empty for province: {}", provincia.getName());
                return false;
            }

            // Import each mapping, skipping duplicates
            for (IdealistaLocationMapping mapping : mappings) {
                executeRunnableInSessionWithTransaction(sessionFactory, () -> {
                    List<IdealistaLocationMapping> existing = mappingDAO.findByIdealistaLocationId(
                            mapping.getIdealistaLocationId());

                    if (existing.isEmpty()) {
                        mappingDAO.create(mapping);
                        LOGGER.debug("Imported mapping: {}", mapping);
                    } else {
                        LOGGER.debug("Skipping existing mapping: {}", mapping);
                    }
                });
            }

            LOGGER.info("Imported {} mappings for province {} from {}",
                    mappings.size(), provincia.getName(), inputFile.getAbsolutePath());
            return true;

        } catch (IOException e) {
            LOGGER.error("Failed to read mappings from file: {}", inputFile, e);
            return false;
        }
    }
}