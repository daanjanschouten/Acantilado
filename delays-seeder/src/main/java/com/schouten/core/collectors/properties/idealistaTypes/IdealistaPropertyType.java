package com.schouten.core.collectors.properties.idealistaTypes;

public enum IdealistaPropertyType {
    LAND("lands"),
    HOME("homes");

    final String propertyType;

    public String getName() {
        return this.propertyType;
    }

    IdealistaPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }
}