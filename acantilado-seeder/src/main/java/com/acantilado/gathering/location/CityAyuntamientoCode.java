package com.acantilado.gathering.location;

public enum CityAyuntamientoCode {
    MADRID(28, 79),
    BARCELONA(8, 19),
    VALENCIA(46, 250),
    ZARAGOZA(50, 297),
    SEVILLA(41, 91),
    MALAGA(29, 67),
    SAN_SEBASTIAN(20, 69),
    ALICANTE(3, 14),
    TENERIFE(38, 38),
    LORCA(30, 24);

    private final long provinceCode;
    private final long municipalityCode;

    CityAyuntamientoCode(long provinceCode, long municipalityCode) {
        this.provinceCode = provinceCode;
        this.municipalityCode = municipalityCode;
    }

    public long getProvinceCode() {
        return provinceCode;
    }

    public long getCityCode() {
        return provinceCode * 1000 + municipalityCode;
    }

    public long getMunicipalityCode() {
        return municipalityCode;
    }
}