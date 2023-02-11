package com.schouten.core.aviation.db;

import com.schouten.core.aviation.Aircraft;
import com.schouten.core.external.AircraftSeeder;
import io.dropwizard.hibernate.AbstractDAO;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AircraftDao extends AbstractDAO<Aircraft> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AircraftDao.class);

    public AircraftDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Optional<Aircraft> findById(String aircraftId) {
        return Optional.ofNullable(get(aircraftId));
    }

    public Aircraft create(Aircraft aircraft) {
        return persist(aircraft);
    }

    public List<Aircraft> findAll() {
        return list(
                namedTypedQuery("com.flightdelays.core.Aircraft.findAll")
        );
    }

    public long seed(boolean complete) {
        Set<Aircraft> allAircraft = new AircraftSeeder().seed();
        if (!complete) {
            List<String> existingAircraft = findAll().stream()
                    .map(Aircraft::getHexIcaoId)
                    .collect(Collectors.toList());
            List<Aircraft> newAircraft = allAircraft.stream()
                    .filter(a -> ! existingAircraft.contains(a.getHexIcaoId()))
                    .collect(Collectors.toList());
            newAircraft.forEach(this::create);
            LOGGER.info(StringUtils.join(
                    "Retrieved ", allAircraft.size(), " carriers, of which ", existingAircraft.size(),
                    " already exist. ", newAircraft.size(), " were added: ", newAircraft.toString()));
            return newAircraft.size();
        }
        allAircraft.forEach(this::create);
        LOGGER.info(StringUtils.join("Completely reseeded and added ", allAircraft.size(), " new aircraft."));
        return allAircraft.size();
    }
}
