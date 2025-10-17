package com.acantilado.gathering.properties.idealista;

public enum IdealistaPropertyType {
    LANDS("lands"),
    HOMES("homes");

    final String propertyType;

    public String getName() {
        return this.propertyType;
    }

    IdealistaPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }
}