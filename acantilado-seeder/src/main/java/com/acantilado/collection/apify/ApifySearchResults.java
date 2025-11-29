package com.acantilado.collection.apify;

import java.util.Set;

public record ApifySearchResults<T> (
    Set<T> requestsSucceeded,
    Set<T> requestsToFragment,
    Set<T> requestsToRetryDueToFailure,
    Set<T> requestsToRetryDueToProxy) {

    @Override
    public String toString() {
        return "requestsSucceeded " + requestsSucceeded.size() +
                "; requestsToFragment " + requestsToFragment.size() +
                "; requestsToRetryDueToFailure " + requestsToRetryDueToFailure.size() +
                "; requestsToRetryDueToProxy " + requestsToRetryDueToProxy.size();
    }
}
