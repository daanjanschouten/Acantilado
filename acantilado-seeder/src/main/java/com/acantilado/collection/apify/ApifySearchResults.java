package com.acantilado.collection.apify;

import java.util.Set;

public record ApifySearchResults<T>(
    Set<T> requestsSucceeded,
    Set<T> requestsToFragment,
    Set<T> requestsToRetryDueToFailure,
    Set<T> requestsToRetryDueToEmptyResults) {

  @Override
  public String toString() {
    return "requestsSucceeded "
        + requestsSucceeded.size()
        + "; requestsToFragment "
        + requestsToFragment.size()
        + "; requestsToRetryDueToFailure "
        + requestsToRetryDueToFailure.size()
        + "; requestsToRetryDueToEmptyResults "
        + requestsToRetryDueToEmptyResults.size();
  }
}
