package com.schouten.core.collection.properties.queries;

import com.schouten.core.collection.properties.idealistaTypes.IdealistaOperation;
import com.schouten.core.collection.properties.idealistaTypes.IdealistaPropertyType;

import java.util.Set;

public final class DefaultIdealistaSearchQueries {
    public record IdealistaSearch(IdealistaOperation operation, IdealistaPropertyType type, String location) {}

    public static Set<IdealistaSearch> getSearchesForLocation(String location) {
        return Set.of(
                new IdealistaSearch(IdealistaOperation.SALE, IdealistaPropertyType.LAND, location),
                new IdealistaSearch(IdealistaOperation.SALE, IdealistaPropertyType.HOME, location));
    }
}
