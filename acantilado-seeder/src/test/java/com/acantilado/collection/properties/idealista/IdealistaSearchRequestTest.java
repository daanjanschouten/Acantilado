package com.acantilado.collection.properties.idealista;

class IdealistaSearchRequestTest {
    private final  IdealistaSearchRequest baseSearch = IdealistaSearchRequest.fromSearch(
        new IdealistaSearchRequest.BaseIdealistaSearch(
                IdealistaOperation.SALE,
                IdealistaPropertyType.HOMES,
                "someLocation"));

//    @Test
//    void fragmentableRequestsAreFragmented() {
//        Set<IdealistaSearchRequest> fragmentableIdealistaSearchRequests =
//                IdealistaSearchRequest.fragment(Set.of(baseSearch));
//
//        int numberOfResults = fragmentableIdealistaSearchRequests.size();
//        Assertions.assertEquals(13, numberOfResults);
//    }
//
//    @Test
//    void unfragmentableRequestsAreNotFragmented() {
//        // At this point there's 13
//        Set<IdealistaSearchRequest> fragmentableIdealistaSearchRequests =
//                IdealistaSearchRequest.fragment(Set.of(baseSearch));
//
//        // None of these can be fragmented further
//        Set<IdealistaSearchRequest> unfragmentableIdealistaSearchRequests =
//                IdealistaSearchRequest.fragment(fragmentableIdealistaSearchRequests);
//
//        int numberOfResults = unfragmentableIdealistaSearchRequests.size();
//        Assertions.assertEquals(0, numberOfResults);
//    }
}