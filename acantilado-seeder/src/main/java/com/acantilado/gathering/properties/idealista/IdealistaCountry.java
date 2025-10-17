package com.acantilado.gathering.properties.idealista;

public enum IdealistaCountry {
    SPAIN("es"),
    PORTUGAL("pt"),
    ITALY("it");

    final String country;

    public String getName() {
        return this.country;
    };

    IdealistaCountry(String country) {
        this.country = country;
    }
}