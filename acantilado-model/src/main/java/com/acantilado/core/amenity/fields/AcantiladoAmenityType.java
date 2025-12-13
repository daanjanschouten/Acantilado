package com.acantilado.core.amenity.fields;

/**
 *
 * Criticality levels
 * - CRITICAL: Town is essentially non-functional without these
 * - HIGH: Strong decline signal if missing or reduced hours
 * - MODERATE: Indicates reduced services, but town can function
 * - LOW: Nice to have; smaller towns may lack these
 * - URBAN_INDICATOR: Presence suggests larger town, not small town
 */
public enum AcantiladoAmenityType {

    // CRITICAL - Town is dead without these
    PHARMACY("farmacia", Criticality.CRITICAL),                 // Essential medicine access
    SUPERMARKET("supermercado", Criticality.CRITICAL),          // Daily necessities
    FOOD_STORE("tienda de alimentacion", Criticality.CRITICAL), // Alternative to supermarket

    // HIGH - Strong decline signals
    BAKERY("panaderia", Criticality.HIGH),                      // Daily staple in Spain
    BANK("banco", Criticality.HIGH),                            // First to reduce hours in decline
    POST_OFFICE("correos", Criticality.HIGH),                   // Critical rural infrastructure

    // MODERATE - Reduced services indicator
    BUTCHER("carniceria", Criticality.MODERATE),                // Often replaced by supermarket
    CAFE("cafe", Criticality.MODERATE),                         // Social activity
    FRUIT_STORE("fruteria", Criticality.MODERATE),              // Specialized food retail
    HEALTH_CENTER("centro de salud", Criticality.HIGH),         // Public health access
    RESTAURANT("restaurante", Criticality.MODERATE),            // Economic/tourism activity

    // LOW - Smaller towns may not have
    CHILD_CARE("guarderia", Criticality.URBAN_INDICATOR),       // Needs population of young families
    COLLEGE("colegio", Criticality.MODERATE),                   // Primary school - families present
    GAS_STATION("gasolinera", Criticality.MODERATE),            // Mobility dependent
    HAIRDRESSER("peluqueria", Criticality.LOW),                 // Personal services
    MECHANIC("taller mecanico", Criticality.LOW),               // Can travel to nearby town
    SCHOOL("escuela", Criticality.MODERATE),                    // Alternative term for primary

    // URBAN_INDICATOR - Presence indicates larger town
    HOSPITAL("hospital", Criticality.URBAN_INDICATOR),          // Only in towns/cities
    LIBRARY("biblioteca", Criticality.LOW),                     // Often only in larger towns
    POLICE("policia", Criticality.URBAN_INDICATOR),             // Only in larger municipalities
    UNIVERSITY("universidad", Criticality.URBAN_INDICATOR);     // Only in cities

    private final String searchTerm;
    private final Criticality criticality;

    AcantiladoAmenityType(String searchTerm, Criticality criticality) {
        this.searchTerm = searchTerm;
        this.criticality = criticality;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public Criticality getCriticality() {
        return criticality;
    }

    public enum Criticality {
        CRITICAL(1),
        HIGH(2),
        MODERATE(3),
        LOW(4),
        URBAN_INDICATOR(5);

        private final int level;

        Criticality(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }
}