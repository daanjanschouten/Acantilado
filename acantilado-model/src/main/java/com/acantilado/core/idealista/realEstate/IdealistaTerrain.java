package com.acantilado.core.idealista.realEstate;

import com.acantilado.core.idealista.IdealistaContactInformation;
import com.acantilado.core.idealista.priceRecords.IdealistaTerrainPriceRecord;
import com.fasterxml.jackson.databind.JsonNode;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "IDEALISTA_TERRAIN")
@NamedQueries(
        {
                @NamedQuery(
                        name = "com.schouten.core.properties.idealista.IdealistaTerrain.findAll",
                        query = "SELECT t FROM IdealistaTerrain t"
                ),
                @NamedQuery(
                        name = "com.schouten.core.properties.idealista.IdealistaTerrain.findByMunicipality",
                        query = "SELECT t FROM IdealistaTerrain t WHERE t.municipality = :municipality"
                ),
                @NamedQuery(
                        name = "com.schouten.core.properties.idealista.IdealistaTerrain.findByPropertyCode",
                        query = "SELECT t FROM IdealistaTerrain t WHERE t.propertyCode = :propertyCode"
                )
        }
)
public class IdealistaTerrain extends IdealistaRealEstate<IdealistaTerrainPriceRecord, IdealistaTerrain> {
    @Id
    @Column(name = "property_code")
    private Long propertyCode;

    @Column(name = "operation", nullable = false)
    private String operation;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "size")
    private Long size;

    @Column(name = "sub_typology")
    private String subTypology;

    @Column(name = "address")
    private String address;

    @Column(name = "municipality", nullable = false)
    private String municipality;

    @Column(name = "location_id")
    private String locationId;

    @Column(name = "acantilado_location_id", nullable = false)
    private String acantiladoLocationId;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "first_seen", nullable = false)
    private Long firstSeen;

    @Column(name = "last_seen", nullable = false)
    private Long lastSeen;

    @OneToMany(mappedBy = "terrain", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("recordedAt DESC")
    private List<IdealistaTerrainPriceRecord> priceRecords = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_phone_number", referencedColumnName = "id")
    private IdealistaContactInformation contactInfo;

    public IdealistaTerrain() {}

    public IdealistaTerrain(long propertyCode, String operation, String description, long size, String subTypology,
                            String address, IdealistaContactInformation contactInformation, String municipality, String locationId, String acantiladoLocationId,
                            Double latitude, Double longitude, long firstSeen, long lastSeen) {
        this.propertyCode = propertyCode;
        this.operation = operation;
        this.description = description;
        this.size = size;
        this.subTypology = subTypology;
        this.address = address;
        this.contactInfo = contactInformation;
        this.municipality = municipality;
        this.locationId = locationId;
        this.acantiladoLocationId = acantiladoLocationId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
    }

    public static IdealistaTerrain constructFromJson(JsonNode jsonNode) {
        IdealistaRealEstateBase base = IdealistaRealEstate.construct(jsonNode);

        IdealistaTerrain terrain = new IdealistaTerrain(
                base.propertyCode(),
                base.operation(),
                base.description(),
                base.size(),
                base.subTypology(),
                base.address(),
                base.contactInformation(),
                base.municipality(),
                base.locationId(),
                base.acantiladoLocationId(),
                base.latitude(),
                base.longitude(),
                base.firstSeen(),
                base.lastSeen());

        terrain.setContactInfo(base.contactInformation());

        IdealistaTerrainPriceRecord priceRecord = new IdealistaTerrainPriceRecord(
                base.propertyCode(),
                base.price(),
                base.firstSeen());
        priceRecord.setTerrain(terrain);
        terrain.getPriceRecords().add(priceRecord);

        return terrain;
    }


    // Getters and Setters
    public Long getPropertyCode() { return propertyCode; }
    public void setPropertyCode(Long propertyCode) { this.propertyCode = propertyCode; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public String getSubTypology() { return subTypology; }
    public void setSubTypology(String subTypology) { this.subTypology = subTypology; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getMunicipality() { return municipality; }
    public void setMunicipality(String municipality) { this.municipality = municipality; }

    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }

    public String getAcantiladoLocationId() { return acantiladoLocationId; }
    public void setAcantiladoLocationId(String locationId) { this.acantiladoLocationId = locationId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Long getFirstSeen() { return firstSeen; }
    public void setFirstSeen(Long firstSeen) { this.firstSeen = firstSeen; }

    public Long getLastSeen() { return lastSeen; }
    public void setLastSeen(Long lastSeen) { this.lastSeen = lastSeen; }

    public List<IdealistaTerrainPriceRecord> getPriceRecords() { return priceRecords; }
    public void setPriceRecords(List<IdealistaTerrainPriceRecord> priceRecords) { this.priceRecords = priceRecords; }

    public IdealistaContactInformation getContactInfo() { return contactInfo; }
    public void setContactInfo(IdealistaContactInformation contactInfo) { this.contactInfo = contactInfo; }

    @Override
    public String toString() {
        return "IdealistaTerrain{" +
                "propertyCode=" + propertyCode +
                ", operation='" + operation + '\'' +
                ", description='" + description + '\'' +
                ", size=" + size +
                ", subTypology='" + subTypology + '\'' +
                ", address='" + address + '\'' +
                ", municipality='" + municipality + '\'' +
                ", locationId='" + locationId + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", firstSeen=" + firstSeen +
                ", lastSeen=" + lastSeen +
                ", priceRecords=" + priceRecords +
                ", contactInfo=" + contactInfo +
                '}';
    }
}