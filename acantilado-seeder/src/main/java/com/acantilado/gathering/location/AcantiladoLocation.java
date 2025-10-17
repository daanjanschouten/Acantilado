package com.acantilado.gathering.location;

import com.acantilado.core.administrative.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

public class AcantiladoLocation {
    private static final Logger LOGGER = LoggerFactory.getLogger(AcantiladoLocation.class);
    private static final String JOIN = "-";
    private static final String ABSENT_BARRIO = "XXX";

    private final Ayuntamiento ayuntamiento;
    private final CodigoPostal codigoPostal;
    private final Optional<Barrio> maybeBarrio;

    public AcantiladoLocation(Ayuntamiento ayuntamiento, CodigoPostal codigoPostal) {
        this.ayuntamiento = ayuntamiento;
        this.codigoPostal = codigoPostal;
        this.maybeBarrio = Optional.empty();
    }

    public AcantiladoLocation(Ayuntamiento ayuntamiento, CodigoPostal codigoPostal, Barrio barrio) {
        this.ayuntamiento = ayuntamiento;
        this.codigoPostal = codigoPostal;
        this.maybeBarrio = Optional.of(barrio);
    }

    public static AcantiladoLocation fromLocationIdentifier(
            String identifier,
            AyuntamientoDAO ayuntamientoDAO,
            CodigoPostalDAO codigoPostalDAO,
            BarrioDAO barrioDAO) {

        String[] parts =  StringUtils.split(identifier, JOIN);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid identifier " + identifier);
        }

        Ayuntamiento ayuntamiento = getMandatoryAyuntamiento(parts[0], ayuntamientoDAO);
        CodigoPostal codigoPostal = getMandatoryCodigoPostal(parts[1], codigoPostalDAO);

        return Objects.equals(parts[2], ABSENT_BARRIO)
                ? new AcantiladoLocation(ayuntamiento, codigoPostal)
                : new AcantiladoLocation(ayuntamiento, codigoPostal, getBarrio(parts[2], barrioDAO));
    }

    public String getIdentifier() {
        long ayuntamientoId = ayuntamiento.getId();
        String codigoPostalId = codigoPostal.getCodigoIne();
        String barrioId = maybeBarrio.map(value -> value.getId().toString()).orElse(ABSENT_BARRIO);

        return StringUtils.joinWith(JOIN, ayuntamientoId, codigoPostalId, barrioId);
    }

    private static Barrio getBarrio(String identifierSlice, BarrioDAO barrioDAO) {
        long barrioId = Long.parseLong(identifierSlice);
        Optional<Barrio> maybeBarrio = barrioDAO.findById(barrioId);
        if (maybeBarrio.isEmpty()) {
            throw new IllegalStateException("Provided identifier for ayuntamiento that doesn't exist");
        }
        return maybeBarrio.get();
    }

    private static Ayuntamiento getMandatoryAyuntamiento(String identifierSlice, AyuntamientoDAO ayuntamientoDAO) {
        try {
            long ayuntamientoId = Long.parseLong(identifierSlice);
            Optional<Ayuntamiento> maybeAyuntamiento = ayuntamientoDAO.findById(ayuntamientoId);
            if (maybeAyuntamiento.isEmpty()) {
                throw new IllegalStateException("Provided identifier for ayuntamiento that doesn't exist " + identifierSlice);
            }
            return maybeAyuntamiento.get();
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException("Unable to parse ayuntamiento long from String " + identifierSlice);
        }
    }

    private static CodigoPostal getMandatoryCodigoPostal(String identifierSlice, CodigoPostalDAO codigoPostalDAO) {
        try {
            Optional<CodigoPostal> maybeCodigoPostal = codigoPostalDAO.findById(identifierSlice);
            if (maybeCodigoPostal.isEmpty()) {
                throw new IllegalStateException("Provided identifier for codigo postal that doesn't exist " + identifierSlice);
            }
            return maybeCodigoPostal.get();
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException("Unable to parse codigo postal long from String " + identifierSlice);
        }
    }

    @Override
    public String toString() {
        return getIdentifier();
    }
}
