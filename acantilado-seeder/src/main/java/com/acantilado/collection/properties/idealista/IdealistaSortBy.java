package com.acantilado.collection.properties.idealista;

public enum IdealistaSortBy {
    RELEVANCE("relevance"),
    PROXIMITY("closest"),
    RECENCY("mostRecent");

    final String sortBy;

    public String getName() {
        return this.sortBy;
    }

    IdealistaSortBy(String sortBy) {
        this.sortBy = sortBy;
    }
}