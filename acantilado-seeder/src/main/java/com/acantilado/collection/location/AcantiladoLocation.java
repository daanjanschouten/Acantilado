package com.acantilado.collection.location;

import com.acantilado.core.administrative.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public class AcantiladoLocation {
  private static final String JOIN = "-";
  private static final String ABSENT_BARRIO = "XXX";

  private final Ayuntamiento ayuntamiento;
  private final CodigoPostal codigoPostal;
  private final Optional<Barrio> maybeBarrio;

  public AcantiladoLocation(Ayuntamiento ayuntamiento, CodigoPostal codigoPostal) {
    this.ayuntamiento = ayuntamiento;
    this.codigoPostal = codigoPostal;
    this.maybeBarrio = Optional.empty();
  }

  public AcantiladoLocation(Ayuntamiento ayuntamiento, CodigoPostal codigoPostal, Barrio barrio) {
    this.ayuntamiento = ayuntamiento;
    this.codigoPostal = codigoPostal;
    this.maybeBarrio = Optional.of(barrio);
  }

  public Ayuntamiento getAyuntamiento() {
    return ayuntamiento;
  }

  public static AcantiladoLocation fromLocationIdentifier(
      String identifier,
      AyuntamientoDAO ayuntamientoDAO,
      CodigoPostalDAO codigoPostalDAO,
      BarrioDAO barrioDAO) {

    String[] parts = StringUtils.split(identifier, JOIN);
    if (parts.length != 3) {
      throw new IllegalArgumentException("Invalid identifier " + identifier);
    }

    Ayuntamiento ayuntamiento = getMandatoryAyuntamiento(parts[0], ayuntamientoDAO);
    CodigoPostal codigoPostal = getMandatoryCodigoPostal(parts[1], codigoPostalDAO);

    return Objects.equals(parts[2], ABSENT_BARRIO)
        ? new AcantiladoLocation(ayuntamiento, codigoPostal)
        : new AcantiladoLocation(ayuntamiento, codigoPostal, getBarrio(parts[2], barrioDAO));
  }

  public String getIdentifier() {
    String ayuntamientoId = ayuntamiento.getId();
    String codigoPostalId = codigoPostal.getCodigoIne();
    String barrioId = maybeBarrio.map(value -> value.getId().toString()).orElse(ABSENT_BARRIO);

    return StringUtils.joinWith(JOIN, ayuntamientoId, codigoPostalId, barrioId);
  }

  public static String normalizeIdealistaLocationId(String idealistaLocationId) {
    // Idealista location ID structure:
    // 0-EU-ES-{province}-{comarca}-{subgroup}-{ayuntamiento} = 7 segments (ayuntamiento level -
    // keep as-is)
    // 0-EU-ES-{province}-{comarca}-{subgroup}-{ayuntamiento}-{district} = 8 segments (truncate to
    // 7)
    // 0-EU-ES-{province}-{comarca}-{subgroup}-{ayuntamiento}-{district}-{subdistrict} = 9 segments
    // (truncate to 7)

    String[] parts = idealistaLocationId.split("-");

    if (parts.length <= 7) {
      return idealistaLocationId;
    }

    // Has 8+ segments (includes district/neighborhood info) - keep only first 7
    return String.join("-", Arrays.copyOfRange(parts, 0, 7));
  }

  public static String getAyuntamientoFromNormalizedLocationId(String idealistaLocationId) {
    String[] parts = idealistaLocationId.split("-");

    if (parts.length != 7) {
      throw new IllegalArgumentException("location ID has incorrect length; normalize it first");
    }

    return String.join("", parts[3], parts[6]);
  }

  private static Barrio getBarrio(String identifierSlice, BarrioDAO barrioDAO) {
    long barrioId = Long.parseLong(identifierSlice);
    Optional<Barrio> maybeBarrio = barrioDAO.findById(barrioId);
    if (maybeBarrio.isEmpty()) {
      throw new IllegalStateException("Provided identifier for ayuntamiento that doesn't exist");
    }
    return maybeBarrio.get();
  }

  private static Ayuntamiento getMandatoryAyuntamiento(
      String identifierSlice, AyuntamientoDAO ayuntamientoDAO) {
    try {
      Optional<Ayuntamiento> maybeAyuntamiento = ayuntamientoDAO.findById(identifierSlice);
      if (maybeAyuntamiento.isEmpty()) {
        throw new IllegalStateException(
            "Provided identifier for ayuntamiento that doesn't exist " + identifierSlice);
      }
      return maybeAyuntamiento.get();
    } catch (NumberFormatException numberFormatException) {
      throw new IllegalArgumentException(
          "Unable to parse ayuntamiento long from String " + identifierSlice);
    }
  }

  private static CodigoPostal getMandatoryCodigoPostal(
      String identifierSlice, CodigoPostalDAO codigoPostalDAO) {
    try {
      Optional<CodigoPostal> maybeCodigoPostal = codigoPostalDAO.findById(identifierSlice);
      if (maybeCodigoPostal.isEmpty()) {
        throw new IllegalStateException(
            "Provided identifier for codigo postal that doesn't exist " + identifierSlice);
      }
      return maybeCodigoPostal.get();
    } catch (NumberFormatException numberFormatException) {
      throw new IllegalArgumentException(
          "Unable to parse codigo postal long from String " + identifierSlice);
    }
  }

  @Override
  public String toString() {
    return getIdentifier();
  }
}
