package com.acantilado.core.amenity.fields;

public enum ExpectWebsite {
  TRUE("withWebsite"),
  FALSE("allPlaces");

  private final String searchTerm;

  ExpectWebsite(String searchTerm) {
    this.searchTerm = searchTerm;
  }

  public String getSearchTerm() {
    return searchTerm;
  }
}
