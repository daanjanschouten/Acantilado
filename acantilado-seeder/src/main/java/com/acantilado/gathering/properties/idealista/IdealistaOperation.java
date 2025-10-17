package com.acantilado.gathering.properties.idealista;

public enum IdealistaOperation {
    SALE("sale"),
    RENT("rent");

    final String operation;

    public String getName() {
        return this.operation;
    };

    IdealistaOperation(String operation) {
        this.operation = operation;
    }
}