package com.acantilado.collection.properties.idealista;

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

    public static IdealistaCountry fromCountryCode(String countryCode) {
        for (IdealistaCountry c : values()) {
            if (c.country.equals(countryCode)) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown country code: " + countryCode);
    }
}