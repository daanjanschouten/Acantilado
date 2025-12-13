package com.acantilado.core.amenity.fields;

/**
 * This project uses amenity presence and opening-hour dynamics as a proxy for <b>town vitality</b>,
 * not simple amenity availability. As such, amenities are evaluated by how quickly and truthfully
 * they respond to changes in local economic demand.
 *
 * <p>Good indicators are amenities that act as early, market-driven signals of town health. These
 * services are sensitive to population decline, income loss, and reduced foot traffic.
 *
 * <ul>
 *   <li>Serve the general population
 *   <li>Are commercially fragile and margin-sensitive
 *   <li>Adjust opening hours quickly in response to demand
 *   <li>Close early when a town begins to decline
 * </ul>
 *
 * <p>Worse indicators are amenities whose presence and hours are weak indicators of economic
 * vitality. They primarily encode <b>demographic structure</b> or administrative importance, and
 * are often insulated from market forces.
 *
 * <ul>
 *   <li>Serve a narrow age group
 *   <li>Are politically or institutionally protected
 *   <li>Exhibit "sticky" opening hours
 *   <li>Close late, abruptly, or only after prolonged decline
 * </ul>
 *
 * <p>Similarly, amenities such as car mechanics and gas stations are poor indicators because
 * they're not as bound to the towns they're in, and will get traffic from neighboring towns.
 * Additionally, opening hours aren't as affected by traffic, and they tend to close suddenly rather
 * than gradually
 */
public enum GoogleAmenityCategory {
  // HIGH
  PHARMACY("farmacia", Criticality.HIGH, ExpectWebsite.FALSE),
  SUPERMARKET("supermercado", Criticality.HIGH, ExpectWebsite.TRUE),

  // MODERATE
  BANK("banco", Criticality.MODERATE, ExpectWebsite.TRUE),
  CAFE("cafe", Criticality.MODERATE, ExpectWebsite.FALSE),
  POST_OFFICE("correos", Criticality.MODERATE, ExpectWebsite.TRUE),

  // MODERATE
  HAIRDRESSER("peluqueria", Criticality.MODERATE, ExpectWebsite.FALSE),

  // URBAN_INDICATOR
  HOSPITAL("hospital", Criticality.URBAN, ExpectWebsite.TRUE),
  LIBRARY("biblioteca", Criticality.URBAN, ExpectWebsite.TRUE),
  UNIVERSITY("universidad", Criticality.URBAN, ExpectWebsite.TRUE),

  UNIDENTIFIED("unidentified", Criticality.LOW, ExpectWebsite.FALSE);

  private final String searchTerm;
  private final Criticality criticality;
  private final ExpectWebsite expectWebsite;

  GoogleAmenityCategory(String searchTerm, Criticality criticality, ExpectWebsite expectWebsite) {
    this.searchTerm = searchTerm;
    this.criticality = criticality;
    this.expectWebsite = expectWebsite;
  }

  public String getSearchTerm() {
    return searchTerm;
  }

  public Criticality getCriticality() {
    return criticality;
  }

  public ExpectWebsite getExpectWebsite() {
    return expectWebsite;
  }

  public enum Criticality {
    HIGH(1),
    MODERATE(2),
    LOW(3),
    URBAN(4);

    private final int level;

    Criticality(int level) {
      this.level = level;
    }

    public int getLevel() {
      return level;
    }
  }
}
