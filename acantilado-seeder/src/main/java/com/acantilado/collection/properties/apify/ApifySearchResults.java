package com.acantilado.collection.properties.apify;

import com.acantilado.collection.properties.idealista.IdealistaSearchRequest;

import java.util.Set;

public record ApifySearchResults(
    Set<IdealistaSearchRequest> requestsSucceeded,
    Set<IdealistaSearchRequest> requestsToFragment,
    Set<IdealistaSearchRequest> requestsToRetryDueToFailure,
    Set<IdealistaSearchRequest> requestsToRetryDueToProxy) {

    @Override
    public String toString() {
        return "requestsSucceeded " + requestsSucceeded.size() +
                "; requestsToFragment " + requestsToFragment.size() +
                "; requestsToRetryDueToFailure " + requestsToRetryDueToFailure.size() +
                "; requestsToRetryDueToProxy " + requestsToRetryDueToProxy.size();
    }
}
