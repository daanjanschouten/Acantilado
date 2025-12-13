package com.acantilado.core.amenity.fields;

public enum AcantiladoAmenityChain {
  CARREFOUR(GoogleAmenityCategory.SUPERMARKET),
  ALCAMPO(GoogleAmenityCategory.SUPERMARKET),
  DIA(GoogleAmenityCategory.SUPERMARKET),
  MERCADONA(GoogleAmenityCategory.SUPERMARKET),
  LIDL(GoogleAmenityCategory.SUPERMARKET),
  ALDI(GoogleAmenityCategory.SUPERMARKET),
  AHORRAMAS(GoogleAmenityCategory.SUPERMARKET),
  COVIRAN(GoogleAmenityCategory.SUPERMARKET),
  SUPERCOR(GoogleAmenityCategory.SUPERMARKET),

  BBVA(GoogleAmenityCategory.BANK),
  SANTANDER(GoogleAmenityCategory.BANK),
  CAIXABANK(GoogleAmenityCategory.BANK),
  SABADELL(GoogleAmenityCategory.BANK);

  private final GoogleAmenityCategory amenityType;

  AcantiladoAmenityChain(GoogleAmenityCategory amenityType) {
    this.amenityType = amenityType;
  }

  public GoogleAmenityCategory getAmenityType() {
    return amenityType;
  }
}
