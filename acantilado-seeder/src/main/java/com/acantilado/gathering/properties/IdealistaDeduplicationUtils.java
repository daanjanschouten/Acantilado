package com.acantilado.gathering.properties;

import com.acantilado.core.idealista.IdealistaContactInformation;
import com.acantilado.core.idealista.IdealistaContactInformationDAO;
import com.acantilado.core.idealista.priceRecords.IdealistaPriceRecordBase;
import com.acantilado.core.idealista.priceRecords.IdealistaPropertyPriceRecord;
import com.acantilado.core.idealista.priceRecords.IdealistaTerrainPriceRecord;
import com.acantilado.core.idealista.realEstate.IdealistaProperty;
import com.acantilado.core.idealista.realEstate.IdealistaRealEstate;
import com.acantilado.core.idealista.realEstate.IdealistaTerrain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

public final class IdealistaDeduplicationUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaDeduplicationUtils.class);

    private IdealistaDeduplicationUtils() {}

    public static IdealistaContactInformation establishContactInformation(IdealistaContactInformation newContactInformation, IdealistaContactInformationDAO contactInformationDAO) {
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

    public static <T extends IdealistaRealEstate<?>> T establishLocation(T realEstate) {
        return realEstate;
    }

    public static <T extends IdealistaRealEstate<?>> IdealistaCollectorService.IdealistaRealEstateResult<T> mergeRealEstate(
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
        return new IdealistaCollectorService.IdealistaRealEstateResult<>(existingRealEstate, priceHasChanged
                ? IdealistaCollectorService.IdealistaRealEstateResult.Result.PRICE_CHANGE
                : IdealistaCollectorService.IdealistaRealEstateResult.Result.EXISTING_IDENTICAL);
    }
}
