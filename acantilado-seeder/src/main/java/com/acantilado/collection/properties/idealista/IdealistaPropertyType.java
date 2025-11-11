package com.acantilado.collection.properties.idealista;

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

    public static IdealistaPropertyType fromTypeCode(String typeCode) {
        for (IdealistaPropertyType t : values()) {
            if (t.propertyType.equals(typeCode)) {
                return t;
            }
        }
        throw new IllegalArgumentException();
    }
}