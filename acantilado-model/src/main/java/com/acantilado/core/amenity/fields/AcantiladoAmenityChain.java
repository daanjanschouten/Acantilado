package com.acantilado.core.amenity.fields;

public enum AcantiladoAmenityChain {
    // Supermarkets
    CARREFOUR(AcantiladoAmenityType.SUPERMARKET),
    ALCAMPO(AcantiladoAmenityType.SUPERMARKET),
    DIA(AcantiladoAmenityType.SUPERMARKET),
    MERCADONA(AcantiladoAmenityType.SUPERMARKET),
    LIDL(AcantiladoAmenityType.SUPERMARKET),
    ALDI(AcantiladoAmenityType.SUPERMARKET),

    // Banks (major Spanish ones)
    BBVA(AcantiladoAmenityType.BANK),
    SANTANDER(AcantiladoAmenityType.BANK),
    CAIXABANK(AcantiladoAmenityType.BANK),
    SABADELL(AcantiladoAmenityType.BANK),

    // Gas Stations
    CEPSA(AcantiladoAmenityType.GAS_STATION),
    REPSOL(AcantiladoAmenityType.GAS_STATION);

    private final AcantiladoAmenityType amenityType;

    AcantiladoAmenityChain(AcantiladoAmenityType amenityType) {
        this.amenityType = amenityType;
    }

    public AcantiladoAmenityType getAmenityType() {
        return amenityType;
    }
}
