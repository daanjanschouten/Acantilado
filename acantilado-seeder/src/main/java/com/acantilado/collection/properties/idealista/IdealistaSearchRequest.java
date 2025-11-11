package com.acantilado.collection.properties.idealista;

import com.acantilado.collection.utils.RequestBodyData;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpRequest;
import java.util.*;
import java.util.stream.Collectors;

public class IdealistaSearchRequest implements RequestBodyData {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdealistaSearchRequest.class);
    private static final int MAX_SURFACE_AREA = 300;
    private static final ProxyConfiguration DEFAULT_PROXY_CONFIGURATION = ProxyConfiguration.datacenter();

    public record BaseIdealistaSearch(IdealistaOperation operation, IdealistaPropertyType type, String location) {}

    private static List<Pair<Integer, Integer>> PERMITTED_SIZE_VALUES() {
        List<Integer> values = Arrays.asList(0, 60, 80, 100, 140, 160, 180, 200, 220, 240, 260, 280, 300);
        List<Pair<Integer, Integer>> pairs = new ArrayList<>();
        for (int i = 0; i < values.size() - 1; i++) {
            pairs.add(Pair.of(values.get(i), values.get(i + 1)));
        }
        return pairs;
    }

    @JsonProperty("country")
    private final String country;

    @JsonProperty("location")
    private final String location;

    @JsonProperty("operation")
    private final String operation;

    @JsonProperty("propertyType")
    private final String propertyType;

    @JsonProperty("maxItems")
    private final int maxItems;

    @JsonProperty("sortBy")
    private final String sortBy;

    @JsonProperty("minSize")
    private final String minSize;

    @JsonProperty("maxSize")
    private final String maxSize;

    @JsonProperty("proxyConfiguration")
    private final ProxyConfiguration proxyConfiguration;

    public IdealistaSearchRequest(
            IdealistaCountry country,
            IdealistaOperation operation,
            IdealistaPropertyType propertyType,
            IdealistaSortBy sortBy,
            String location,
            int maxItems,
            String minSize,
            String maxSize,
            ProxyConfiguration proxyConfiguration) {
        this.country = country.getName();
        this.operation = operation.getName();
        this.propertyType = propertyType.getName();
        this.sortBy = sortBy.getName();

        this.maxItems = maxItems;
        this.location = location;

        this.minSize = minSize;
        this.maxSize = maxSize;

        this.proxyConfiguration = proxyConfiguration;
    }

    public String getLocation() {
        return this.location;
    }

    @Override
    public HttpRequest.BodyPublisher toRequestBodyString() {
        ObjectMapper mapper = new ObjectMapper();

        try {
            return HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(this));
        } catch (JsonProcessingException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static Set<IdealistaSearchRequest> withResidentialProxy(Set<IdealistaSearchRequest> requests) {
        return requests
                .stream()
                .map(IdealistaSearchRequest::withResidentialProxy)
                .collect(Collectors.toSet());
    }

    private static IdealistaSearchRequest withResidentialProxy(IdealistaSearchRequest request) {
        return new IdealistaSearchRequest(
                IdealistaCountry.fromCountryCode(request.country),
                IdealistaOperation.fromOperationCode(request.operation),
                IdealistaPropertyType.fromTypeCode(request.propertyType),
                IdealistaSortBy.fromSortBy(request.sortBy),
                request.location,
                request.maxItems,
                request.minSize,
                request.maxSize,
                ProxyConfiguration.residential()
        );
    }

    public static IdealistaSearchRequest fromSearch(BaseIdealistaSearch search) {
        return new IdealistaSearchRequest(
                IdealistaCountry.SPAIN,
                search.operation(),
                search.type(),
                IdealistaSortBy.PROXIMITY,
                search.location,
                2400,
                String.valueOf(0),
                String.valueOf(0),
                ProxyConfiguration.residential());
    }

    public static Set<IdealistaSearchRequest> fragment(Set<IdealistaSearchRequest> requests) {
        Set<IdealistaSearchRequest> fragmentedRequests = new HashSet<>();
        Set<IdealistaSearchRequest> cannotFragment = new HashSet<>();

        requests.forEach(request -> {
            if (isFragmentable(request)) {
                PERMITTED_SIZE_VALUES().forEach(pair -> {
                    fragmentedRequests.add(IdealistaSearchRequest.withSurfaceAreaBounds(request, pair.getLeft(), pair.getRight()));
                });
                fragmentedRequests.add(IdealistaSearchRequest.withSurfaceAreaBounds(request, MAX_SURFACE_AREA, 0));
            } else {
                cannotFragment.add(request);
            }
        });

        if (!cannotFragment.isEmpty()) {
            LOGGER.error("Cannot fragment {} requests further: {}", cannotFragment.size(), cannotFragment);
        }

        return fragmentedRequests;
    }

    private static IdealistaSearchRequest withSurfaceAreaBounds(IdealistaSearchRequest request, int minSize, int maxSize) {
        return new IdealistaSearchRequest(
                IdealistaCountry.SPAIN,
                IdealistaOperation.valueOf(request.operation.toUpperCase(Locale.ROOT)),
                IdealistaPropertyType.valueOf(request.propertyType.toUpperCase(Locale.ROOT)),
                IdealistaSortBy.PROXIMITY,
                request.getLocation(),
                2400,
                String.valueOf(minSize),
                String.valueOf(maxSize),
                request.proxyConfiguration);
    }

    @Override
    public String toString() {
        return "IdealistaSearchRequest{" +
                "country='" + country + '\'' +
                ", location='" + location + '\'' +
                ", operation='" + operation + '\'' +
                ", propertyType='" + propertyType + '\'' +
                ", maxItems=" + maxItems +
                ", sortBy='" + sortBy + '\'' +
                ", minSize='" + minSize + '\'' +
                ", maxSize='" + maxSize + '\'' +
                ", proxyConfiguration=" + proxyConfiguration +
                '}';
    }

    public static IdealistaSearchRequest saleSearch(String location, IdealistaPropertyType propertyType) {
        BaseIdealistaSearch search = new BaseIdealistaSearch(
                IdealistaOperation.SALE,
                propertyType,
                location);
        return IdealistaSearchRequest.fromSearch(search);
    }

    public static boolean isFragmentable(IdealistaSearchRequest searchRequest) {
        boolean minIsZero = Objects.equals(searchRequest.minSize, "0");
        boolean maxIsZero = Objects.equals(searchRequest.maxSize, "0");

        return minIsZero && maxIsZero;
    }

    public static IdealistaSearchRequest locationBasedSaleSearch(String location, IdealistaPropertyType propertyType) {
        return new IdealistaSearchRequest(
                IdealistaCountry.SPAIN,
                IdealistaOperation.SALE,
                propertyType,
                IdealistaSortBy.RECENCY,
                location,
                2400,
                String.valueOf(0),
                String.valueOf(0),
                ProxyConfiguration.residential());
    }
}