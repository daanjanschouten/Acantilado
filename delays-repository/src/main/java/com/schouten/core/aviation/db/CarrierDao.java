package com.schouten.core.aviation.db;

import com.schouten.core.aviation.Carrier;
import com.schouten.core.external.CarrierSeeder;
import io.dropwizard.hibernate.AbstractDAO;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class CarrierDao extends AbstractDAO<Carrier> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarrierDao.class);

    public CarrierDao(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public Optional<Carrier> findById(String CarrierId) {
        return Optional.ofNullable(get(CarrierId));
    }

    public Carrier create(Carrier Carrier) {
        return persist(Carrier);
    }

    public List<Carrier> findAll() {
        return list(
                namedTypedQuery("com.flightdelays.core.Carrier.findAll")
        );
    }

    public long seed(boolean complete) throws IOException, InterruptedException {
        Set<Carrier> allCarriers = new CarrierSeeder().seed();
        if (!complete) {
            List<String> existingCarriers = findAll().stream()
                    .map(Carrier::getCarrierId)
                    .collect(Collectors.toList());
            List<Carrier> newCarriers = allCarriers.stream()
                    .filter(c -> ! existingCarriers.contains(c.getCarrierId()))
                    .collect(Collectors.toList());
            newCarriers.forEach(this::create);
            LOGGER.info(StringUtils.join(
                    "Retrieved ", allCarriers.size(), " carriers, of which ", existingCarriers.size(),
                    " already exist. ", newCarriers.size(), " were added: ", newCarriers.toString()));
            return newCarriers.size();
        }
        allCarriers.forEach(this::create);
        LOGGER.info(StringUtils.join("Completely reseeded and added ", allCarriers.size(), " new carriers."));
        return allCarriers.size();
    }
}
