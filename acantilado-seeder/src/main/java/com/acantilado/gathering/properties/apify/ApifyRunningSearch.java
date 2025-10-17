package com.acantilado.gathering.properties.apify;

import com.acantilado.gathering.properties.idealista.IdealistaSearchRequest;

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
