package com.acantilado.gathering.properties.collectors;

import java.util.Optional;

public class PendingSearchOrError {
    private final Optional<ApifyCollector.ApifyPendingSearch> pendingSearch;
    private final Optional<String> error;
    private final boolean succeeded;

    public PendingSearchOrError(Optional<ApifyCollector.ApifyPendingSearch> pendingSearch, Optional<String> error) {
        this.pendingSearch = pendingSearch;
        this.error = error;
        this.succeeded = error.isEmpty();
    }

    public Optional<ApifyCollector.ApifyPendingSearch> getPendingSearch() {
        return pendingSearch;
    }

    public Optional<String> getError() {
        return error;
    }

    public boolean isSucceeded() {
        return succeeded;
    }
}