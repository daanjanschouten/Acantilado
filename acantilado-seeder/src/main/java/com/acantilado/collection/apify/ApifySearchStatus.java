package com.acantilado.collection.apify;

// https://docs.apify.com/platform/actors/running/runs-and-builds
public enum ApifySearchStatus {
  TO_BE_SUBMITTED(false, false), // custom
  STARTED(false, false),
  READY(false, false),
  RUNNING(false, false),
  SUCCEEDED(true, false),
  FAILED(true, true),
  ABORTED(true, true),
  TIMED_OUT(true, true);

  private final boolean finished;
  private final boolean failed;

  ApifySearchStatus(boolean finished, boolean failed) {
    this.finished = finished;
    this.failed = failed;
  }

  public boolean hasFinished() {
    return finished;
  }

  public boolean hasFailed() {
    return failed;
  }
}
