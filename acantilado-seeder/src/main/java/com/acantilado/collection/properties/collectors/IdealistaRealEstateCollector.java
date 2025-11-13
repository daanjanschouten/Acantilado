package com.acantilado.collection.properties.collectors;

import com.acantilado.core.idealista.IdealistaContactInformation;
import com.acantilado.core.idealista.IdealistaContactInformationDAO;
import com.acantilado.core.idealista.IdealistaRealEstateDAO;
import com.acantilado.core.idealista.priceRecords.IdealistaPriceRecordBase;
import com.acantilado.core.idealista.priceRecords.IdealistaPropertyPriceRecord;
import com.acantilado.core.idealista.priceRecords.IdealistaTerrainPriceRecord;
import com.acantilado.core.idealista.realEstate.IdealistaProperty;
import com.acantilado.core.idealista.realEstate.IdealistaRealEstate;
import com.acantilado.core.idealista.realEstate.IdealistaTerrain;
import com.acantilado.collection.location.AcantiladoLocation;
import com.acantilado.collection.location.AcantiladoLocationEstablisher;
import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class IdealistaRealEstateCollector<T extends IdealistaRealEstate<? extends IdealistaPriceRecordBase>> extends ApifyCollector<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaRealEstateCollector.class);
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();


    private final AcantiladoLocationEstablisher locationEstablisher;
    private final IdealistaContactInformationDAO contactInformationDAO;
    private final IdealistaRealEstateDAO<T> realEstateDAO;
    private final Function<JsonNode, T> constructObjectFunction;

    public IdealistaRealEstateCollector(
            AcantiladoLocationEstablisher locationEstablisher,
            IdealistaContactInformationDAO contactInformationDAO,
            IdealistaRealEstateDAO<T> realEstateDAO,
            Function<JsonNode, T> constructObjectFunction) {
        this.locationEstablisher = locationEstablisher;
        this.contactInformationDAO = contactInformationDAO;
        this.realEstateDAO = realEstateDAO;
        this.constructObjectFunction = constructObjectFunction;
    }

    @Override
    protected T constructObject(JsonNode jsonNode) {
        try {
            return constructObjectFunction.apply(jsonNode);
        } catch (Exception e) {
            LOGGER.error("Failed to construct JSON object: {}", jsonNode, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void storeResult(T realEstate) {
        Coordinate coordinate = new Coordinate(realEstate.getLongitude(), realEstate.getLatitude());
        AcantiladoLocation location = locationEstablisher.establish(
                realEstate.getMunicipality(),
                AcantiladoLocation.normalizeIdealistaLocationId(realEstate.getLocationId()),
                GEOMETRY_FACTORY.createPoint(coordinate));

        realEstate.setAcantiladoLocationId(location.getIdentifier());

        IdealistaContactInformation definitiveContactInformation =
                establishContactInformation(realEstate.getContactInfo(), contactInformationDAO);
        IdealistaRealEstateResult<T> idealistaRealEstateResult = establishProperty(realEstate, realEstateDAO);

        T definitiveIdealistaRealEstate = idealistaRealEstateResult.idealistaRealEstate();
        definitiveIdealistaRealEstate.setContactInfo(definitiveContactInformation);
        realEstateDAO.saveOrUpdate(definitiveIdealistaRealEstate);
    }

    public record IdealistaRealEstateResult<T extends IdealistaRealEstate<?>>(T idealistaRealEstate, Result result) {
        public enum Result { EXISTING_IDENTICAL, PRICE_CHANGE, NEW }
    }

    private static <T extends IdealistaRealEstate<? extends IdealistaPriceRecordBase>> IdealistaRealEstateResult<T> establishProperty(
            T newRealEstate,
            IdealistaRealEstateDAO<T> realEstateDAO) {
        final long code = newRealEstate.getPropertyCode();
        Optional<T> maybeRealEstate = realEstateDAO.findByPropertyCode(code);

        return maybeRealEstate.map(existingRealEstate -> mergeRealEstate(newRealEstate, existingRealEstate, code))
                .orElseGet(() -> new IdealistaRealEstateResult<>(newRealEstate, IdealistaRealEstateResult.Result.NEW));
    }

    private static <T extends IdealistaRealEstate<? extends IdealistaPriceRecordBase>> IdealistaRealEstateResult<T> mergeRealEstate(
            T newRealEstate,
            T existingRealEstate,
            long code) {
        final long currentTimestamp = Instant.now().toEpochMilli();
        existingRealEstate.setLastSeen(currentTimestamp);

        boolean priceHasChanged = false;
        List<? extends IdealistaPriceRecordBase> priceRecords = existingRealEstate.getPriceRecords();

        if (priceRecords.isEmpty()) {
            LOGGER.error("Found existing real estate but no existing price record {}", existingRealEstate);
        } else {
            final long newPrice = newRealEstate.getPriceRecords().get(0).getPrice();
            if (priceRecords.get(0).getPrice() != newPrice) {
                LOGGER.debug("Price has changed! {}", code);
                priceHasChanged = true;

                if (existingRealEstate instanceof IdealistaProperty && newRealEstate instanceof IdealistaProperty) {
                    IdealistaProperty existingProp = (IdealistaProperty) existingRealEstate;
                    IdealistaProperty newProp = (IdealistaProperty) newRealEstate;
                    IdealistaPropertyPriceRecord newPriceRecord = newProp.getPriceRecords().get(0);
                    newPriceRecord.setProperty(existingProp);
                    existingProp.getPriceRecords().add(newPriceRecord);
                } else if (existingRealEstate instanceof IdealistaTerrain && newRealEstate instanceof IdealistaTerrain) {
                    IdealistaTerrain existingTerr = (IdealistaTerrain) existingRealEstate;
                    IdealistaTerrain newTerr = (IdealistaTerrain) newRealEstate;
                    IdealistaTerrainPriceRecord newPriceRecord = newTerr.getPriceRecords().get(0);
                    newPriceRecord.setTerrain(existingTerr);
                    existingTerr.getPriceRecords().add(newPriceRecord);
                }
            }
        }
        return new IdealistaRealEstateResult<>(existingRealEstate, priceHasChanged
                ? IdealistaRealEstateResult.Result.PRICE_CHANGE
                : IdealistaRealEstateResult.Result.EXISTING_IDENTICAL);
    }

    private static IdealistaContactInformation establishContactInformation(IdealistaContactInformation newContactInformation, IdealistaContactInformationDAO contactInformationDAO) {
        IdealistaContactInformation definitiveContactInformation;

        if (newContactInformation.getPhoneNumber() == 0) {
            definitiveContactInformation = contactInformationDAO.create(newContactInformation);
        } else {
            definitiveContactInformation = contactInformationDAO
                    .findByPhoneNumber(newContactInformation.getPhoneNumber())
                    .orElse(contactInformationDAO.create(newContactInformation));
        }
        return definitiveContactInformation;
    }
}
