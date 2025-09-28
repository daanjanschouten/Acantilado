package com.acantilado.collection.properties.idealistaTypes;

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