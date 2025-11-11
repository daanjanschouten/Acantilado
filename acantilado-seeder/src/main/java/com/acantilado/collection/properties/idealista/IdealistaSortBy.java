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

    public static IdealistaSortBy fromSortBy(String sortBy) {
        for (IdealistaSortBy s : values()) {
            if (s.sortBy.equals(sortBy)) {
                return s;
            }
        }
        throw new IllegalArgumentException();
    }
}