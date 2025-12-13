package com.acantilado.collection.location;

public enum CityAyuntamientoCode {
  MADRID("28", "079"),
  BARCELONA("08", "019"),
  VALENCIA("46", "250"),
  ZARAGOZA("50", "297"),
  SEVILLA("41", "091"),
  MALAGA("29", "067"),
  SAN_SEBASTIAN("20", "069"),
  ALICANTE("03", "014"),
  // TENERIFE("38", "038"),
  LORCA("30", "024");

  private final String provinceCode;
  private final String municipalityCode;

  CityAyuntamientoCode(String provinceCode, String municipalityCode) {
    this.provinceCode = provinceCode;
    this.municipalityCode = municipalityCode;
  }

  public String getProvinceCode() {
    return provinceCode;
  }

  public String getCityCode() {
    return provinceCode + municipalityCode;
  }

  public String getMunicipalityCode() {
    return municipalityCode;
  }
}
