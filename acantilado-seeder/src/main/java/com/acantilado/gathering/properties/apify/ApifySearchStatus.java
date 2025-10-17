package com.acantilado.gathering.properties.apify;

// https://docs.apify.com/platform/actors/running/runs-and-builds
public enum ApifySearchStatus {
    TO_BE_SUBMITTED(false), // custom
    STARTED(false),
    READY(false),
    RUNNING(false),
    SUCCEEDED(true),
    FAILED(true),
    ABORTED(true);

    private final boolean finished;

    ApifySearchStatus(boolean finished) {
        this.finished = finished;
    }

    public boolean isFinished() {
        return finished;
    }
}
