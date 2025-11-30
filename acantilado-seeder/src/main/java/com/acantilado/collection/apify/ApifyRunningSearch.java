package com.acantilado.collection.apify;

public final class ApifyRunningSearch<T> {
    private final T request;
    private final ApifySearchStatus pendingSearchStatus;
    private final String runId;
    private final String datasetId;

    public ApifyRunningSearch(T request, ApifySearchStatus pendingSearchStatus, String runId, String datasetId) {
        this.request = request;
        this.pendingSearchStatus = pendingSearchStatus;
        this.runId = runId;
        this.datasetId = datasetId;
    }

    public ApifyRunningSearch(T request, String runId, String datasetId) {
        this.request = request;
        this.datasetId = datasetId;
        this.runId = runId;

        this.pendingSearchStatus = ApifySearchStatus.TO_BE_SUBMITTED;
    }

    public ApifyRunningSearch<T> withStatus(ApifySearchStatus searchStatus) {
        return new ApifyRunningSearch<>(
                this.request,
                searchStatus,
                this.runId,
                this.datasetId);
    }

    public T getRequest() { return request; }
    public ApifySearchStatus getStatus() { return pendingSearchStatus; }
    public String getRunId() { return runId; }
    public String getDatasetId() { return datasetId; }
}
