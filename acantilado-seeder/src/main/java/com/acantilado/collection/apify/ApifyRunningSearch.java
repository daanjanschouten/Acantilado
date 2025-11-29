package com.acantilado.collection.apify;

public record ApifyRunningSearch<T> (
        T request,
        ApifySearchStatus pendingSearchStatus,
        String runId,
        String datasetId) {

    public ApifyRunningSearch<T> withStatus(ApifySearchStatus searchStatus) {
        return new ApifyRunningSearch<>(
                this.request,
                searchStatus,
                this.runId,
                this.datasetId);
    }
}
