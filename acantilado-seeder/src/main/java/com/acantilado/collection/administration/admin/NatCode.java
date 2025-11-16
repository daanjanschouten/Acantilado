package com.acantilado.collection.administration.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Immutable value object representing a parsed NATCODE from Spanish administrative GeoJSON files.
 *
 * NATCODE Format: CC AA PP PPMMMM (11 digits)
 * - CC (positions 0-1): Country code = "34" (Spain)
 * - AA (positions 2-3): Autonomous Community code
 * - PP (positions 4-5): Province code
 * - PPMMMM (positions 6-10): Full 5-digit INE municipality code
 *
 * Examples:
 * - Municipality: 34160101017 -> Country="34", CCAA="16", Province="01", INE="01017"
 * - Province: 34160100000 -> Country="34", CCAA="16", Province="01", INE="00000"
 * - CCAA: 34160000000 -> Country="34", CCAA="16", Province="00", INE="00000"
 *
 * All IDs are returned as Strings to preserve leading zeros.
 */
public final class NatCode {
    private static final Logger LOGGER = LoggerFactory.getLogger(NatCode.class);

    // NATCODE structure lengths
    private static final int COUNTRY_CODE_LENGTH = 2;
    private static final int CCAA_CODE_LENGTH = 2;
    private static final int PROVINCE_CODE_LENGTH = 2;
    private static final int INE_CODE_LENGTH = 5;
    private static final int NATCODE_LENGTH = COUNTRY_CODE_LENGTH + CCAA_CODE_LENGTH + PROVINCE_CODE_LENGTH + INE_CODE_LENGTH;
    private static final String EXPECTED_COUNTRY_CODE = "34";

    private final String countryCode;
    private final String comunidadAutonomaId;
    private final String provinciaId;
    private final String ineCode;
    private final String rawValue;

    /**
     * Private constructor - use parse() factory method to create instances.
     */
    private NatCode(String rawValue, String countryCode, String comunidadAutonomaId, String provinciaId, String ineCode) {
        this.rawValue = rawValue;
        this.countryCode = countryCode;
        this.comunidadAutonomaId = comunidadAutonomaId;
        this.provinciaId = provinciaId;
        this.ineCode = ineCode;
    }

    /**
     * Parses a NATCODE string into a validated NatCode instance.
     *
     * @param natCode The 11-digit NATCODE string
     * @return A valid NatCode instance
     * @throws IllegalArgumentException if NATCODE is invalid or cannot be parsed
     */
    public static NatCode parse(String natCode) {
        if (natCode == null) {
            throw new IllegalArgumentException("NATCODE cannot be null");
        }

        if (natCode.length() != NATCODE_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Invalid NATCODE length: expected %d, got %d for NATCODE '%s'",
                            NATCODE_LENGTH, natCode.length(), natCode));
        }

        int pos = 0;
        String countryCode = natCode.substring(pos, pos + COUNTRY_CODE_LENGTH);
        pos += COUNTRY_CODE_LENGTH;

        String ccaaCode = natCode.substring(pos, pos + CCAA_CODE_LENGTH);
        pos += CCAA_CODE_LENGTH;

        String provinciaCode = natCode.substring(pos, pos + PROVINCE_CODE_LENGTH);
        pos += PROVINCE_CODE_LENGTH;

        String ineCode = natCode.substring(pos, pos + INE_CODE_LENGTH);

        if (!EXPECTED_COUNTRY_CODE.equals(countryCode)) {
            throw new IllegalArgumentException(
                    String.format("Invalid country code in NATCODE '%s': expected %s, got %s",
                            natCode, EXPECTED_COUNTRY_CODE, countryCode));
        }

        LOGGER.debug("Parsed NATCODE {}: Country={}, CCAA={}, Provincia={}, INE={}",
                natCode, countryCode, ccaaCode, provinciaCode, ineCode);

        return new NatCode(natCode, countryCode, ccaaCode, provinciaCode, ineCode);
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getComunidadAutonomaId() {
        return comunidadAutonomaId;
    }

    public String getProvinciaId() {
        return provinciaId;
    }

    public String getIneCode() {
        return ineCode;
    }

    public String getRawValue() {
        return rawValue;
    }

    @Override
    public String toString() {
        return "NatCode{" +
                "rawValue='" + rawValue + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", comunidadAutonomaId='" + comunidadAutonomaId + '\'' +
                ", provinciaId='" + provinciaId + '\'' +
                ", ineCode='" + ineCode + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NatCode natCode = (NatCode) o;
        return rawValue.equals(natCode.rawValue);
    }

    @Override
    public int hashCode() {
        return rawValue.hashCode();
    }
}