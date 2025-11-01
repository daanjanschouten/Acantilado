package com.acantilado.core.idealista.realEstate;

public interface IdealistaListingVisitor<T extends IdealistaListing> {

    T visit();
}
