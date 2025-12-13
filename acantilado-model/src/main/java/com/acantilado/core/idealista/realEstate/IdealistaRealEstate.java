package com.acantilado.core.idealista.realEstate;

import com.acantilado.core.idealista.IdealistaContactInformation;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class IdealistaRealEstate<P> {
  private static final String SUB_TYPOLOGY_FALLBACK = "Indeterminate";

  public abstract Long getPropertyCode();

  public abstract void setPropertyCode(Long propertyCode);

  public abstract String getOperation();

  public abstract void setOperation(String operation);

  public abstract String getDescription();

  public abstract void setDescription(String description);

  public abstract Long getSize();

  public abstract void setSize(Long size);

  public abstract String getSubTypology();

  public abstract void setSubTypology(String subTypology);

  public abstract String getAddress();

  public abstract void setAddress(String address);

  public abstract String getMunicipality();

  public abstract void setMunicipality(String municipality);

  public abstract String getLocationId();

  public abstract void setLocationId(String locationId);

  public abstract String getAcantiladoLocationId();

  public abstract void setAcantiladoLocationId(String locationId);

  public abstract Double getLatitude();

  public abstract void setLatitude(Double latitude);

  public abstract Double getLongitude();

  public abstract void setLongitude(Double longitude);

  public abstract Long getFirstSeen();

  public abstract void setFirstSeen(Long firstSeen);

  public abstract Long getLastSeen();

  public abstract void setLastSeen(Long lastSeen);

  public abstract List<P> getPriceRecords();

  public abstract void setPriceRecords(List<P> priceRecords);

  public abstract IdealistaContactInformation getContactInfo();

  public abstract void setContactInfo(IdealistaContactInformation contactInfo);

  public record IdealistaRealEstateBase(
      Long price,
      Long propertyCode,
      String operation,
      String description,
      Long size,
      String subTypology,
      String address,
      String municipality,
      String locationId,
      Double latitude,
      Double longitude,
      Long firstSeen,
      Long lastSeen,
      IdealistaContactInformation contactInformation) {}

  /*
      AcantiladoLocation acantiladoLocation = locationEstablisher.establishAndRecordMapping(
              ayuntamiento,
              idealistaLocationId,
              locationPoint,
              propertyCode);

  */

  public static IdealistaRealEstateBase construct(JsonNode jsonNode) {
    final long propertyCode = jsonNode.get("propertyCode").asLong();
    final long currentTimestamp = Instant.now().toEpochMilli();

    String subTypology =
        Objects.isNull(jsonNode.get("detailedType").get("subTypology"))
            ? SUB_TYPOLOGY_FALLBACK
            : jsonNode.get("detailedType").get("subTypology").textValue();
    String description =
        Objects.isNull(jsonNode.get("description")) ? "" : jsonNode.get("description").textValue();
    String contactName =
        Objects.isNull(jsonNode.get("contactInfo").get("contactName"))
            ? ""
            : jsonNode.get("contactInfo").get("contactName").textValue();

    double latitude = jsonNode.get("latitude").asDouble();
    double longitude = jsonNode.get("longitude").asDouble();
    String ayuntamiento = jsonNode.get("municipality").textValue();
    String idealistaLocationId = jsonNode.get("locationId").textValue();

    Optional<IdealistaContactInformation.PhoneContact> phoneContact =
        constructPhone(jsonNode.get("contactInfo"));
    IdealistaContactInformation contactInfo =
        new IdealistaContactInformation(
            phoneContact, contactName, jsonNode.get("contactInfo").get("userType").textValue());

    return new IdealistaRealEstateBase(
        jsonNode.get("price").longValue(),
        propertyCode,
        jsonNode.get("operation").textValue(),
        description,
        jsonNode.get("size").longValue(),
        subTypology,
        jsonNode.get("address").textValue(),
        ayuntamiento,
        idealistaLocationId,
        latitude,
        longitude,
        currentTimestamp,
        currentTimestamp,
        contactInfo);
  }

  private static Optional<IdealistaContactInformation.PhoneContact> constructPhone(
      JsonNode contactJson) {
    if (!Objects.isNull(contactJson.get("phone1"))) {
      return Optional.of(
          new IdealistaContactInformation.PhoneContact(
              contactJson.get("phone1").get("prefix").longValue(),
              contactJson.get("phone1").get("phoneNumber").longValue()));
    }
    return Optional.empty();
  }
}
