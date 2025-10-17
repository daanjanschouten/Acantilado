package com.acantilado.gathering.location;

public enum CityAyuntamientoCodes {
        MADRID(28079L),
        BARCELONA(8019L),
        VALENCIA(46250),
        ZARAGOZA(50297),
        SEVILLA(41091),
        MALAGA(29067L),
        SAN_SEBASTIAN(20069L),
        ALICANTE(3014L),
        TENERIFE(38038L),
        LORCA(30024L);

        public final long cityCode;

        CityAyuntamientoCodes(long cityCode) {
            this.cityCode = cityCode;
        }
}
