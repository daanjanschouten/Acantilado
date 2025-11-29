package com.acantilado.core.amenity.fields;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;

@Converter(autoApply = false)
public class OpeningHoursJsonConverter implements AttributeConverter<OpeningHours, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpeningHoursJsonConverter.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new Jdk8Module())  // For Optional support
            .registerModule(new JavaTimeModule());  // For DayOfWeek support

    @Override
    public String convertToDatabaseColumn(OpeningHours openingHours) {
        if (openingHours == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(openingHours);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to convert OpeningHours to JSON", e);
            throw new RuntimeException("Failed to convert OpeningHours to JSON", e);
        }
    }

    @Override
    public OpeningHours convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(dbData, OpeningHours.class);
        } catch (IOException e) {
            LOGGER.error("Failed to convert JSON to OpeningHours: {}", dbData, e);
            throw new RuntimeException("Failed to convert JSON to OpeningHours", e);
        }
    }
}