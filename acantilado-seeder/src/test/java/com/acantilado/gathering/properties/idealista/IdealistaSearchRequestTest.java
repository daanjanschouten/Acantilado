package com.acantilado.gathering.properties.idealista;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class IdealistaSearchRequestTest {
    private final  IdealistaSearchRequest baseSearch = IdealistaSearchRequest.fromSearch(
        new IdealistaSearchRequest.BaseIdealistaSearch(
                IdealistaOperation.SALE,
                IdealistaPropertyType.HOMES,
                "someLocation"));

//    private final Set<IdealistaSearchRequest> unfragmentableIdealistaSearchRequests =
//            IdealistaSearchRequest.fragment(fragmentableIdealistaSearchRequests);

    @Test
    void fragmentableRequestsAreFragmented() {
        Set<IdealistaSearchRequest> fragmentableIdealistaSearchRequests =
                IdealistaSearchRequest.fragment(Set.of(baseSearch));

        int numberOfResults = fragmentableIdealistaSearchRequests.size();
        Assertions.assertEquals(13, numberOfResults);
    }

    @Test
    void unfragmentableRequestsAreNotFragmented() {
        // At this point there's 13
        Set<IdealistaSearchRequest> fragmentableIdealistaSearchRequests =
                IdealistaSearchRequest.fragment(Set.of(baseSearch));

        Set<IdealistaSearchRequest> unfragmentableIdealistaSearchRequests =
                IdealistaSearchRequest.fragment(fragmentableIdealistaSearchRequests);

        int numberOfResults = unfragmentableIdealistaSearchRequests.size();
        Assertions.assertEquals(0, numberOfResults);
    }
}