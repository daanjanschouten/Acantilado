package com.acantilado.collection.properties.apify;

import com.acantilado.collection.properties.idealista.IdealistaSearchRequest;

public record ApifyRunningSearch(
        IdealistaSearchRequest request,
        ApifySearchStatus pendingSearchStatus,
        String runId,
        String datasetId) {

    public ApifyRunningSearch withStatus(ApifySearchStatus searchStatus) {
        return new ApifyRunningSearch(
                this.request,
                searchStatus,
                this.runId,
                this.datasetId);
    }
}
