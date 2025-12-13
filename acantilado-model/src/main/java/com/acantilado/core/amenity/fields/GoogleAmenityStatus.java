package com.acantilado.core.amenity.fields;

public enum GoogleAmenityStatus {
  OPERATIONAL(true),
  CLOSED_TEMPORARILY(false),
  CLOSED_PERMANENTLY(false);

  private final boolean isOpen;

  GoogleAmenityStatus(boolean isOpen) {
    this.isOpen = isOpen;
  }

  public boolean isOpen() {
    return isOpen;
  }
}
