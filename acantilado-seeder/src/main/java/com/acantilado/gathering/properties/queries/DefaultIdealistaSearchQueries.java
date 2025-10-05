package com.acantilado.gathering.properties.queries;

import com.acantilado.gathering.properties.idealistaTypes.IdealistaOperation;
import com.acantilado.gathering.properties.idealistaTypes.IdealistaPropertyType;

import java.util.Set;

public final class DefaultIdealistaSearchQueries {
    public record IdealistaSearch(IdealistaOperation operation, IdealistaPropertyType type, String location) {}

    public static Set<IdealistaSearch> getSearchesForLocation(String location) {
        return Set.of(
                new IdealistaSearch(IdealistaOperation.SALE, IdealistaPropertyType.LAND, location),
                new IdealistaSearch(IdealistaOperation.SALE, IdealistaPropertyType.HOME, location));
    }
}
