package com.schouten.core.properties.idealista;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "IDEALISTA_PROPERTY")
@NamedQueries(
        {
                @NamedQuery(
                        name = "com.schouten.core.properties.idealista.IdealistaProperty.findAll",
                        query = "SELECT p FROM IdealistaProperty p"
                ),
                @NamedQuery(
                        name = "com.schouten.core.properties.idealista.IdealistaProperty.findByMunicipality",
                        query = "SELECT p FROM IdealistaProperty p WHERE p.municipality = :municipality"
                ),
                @NamedQuery(
                        name = "com.schouten.core.properties.idealista.IdealistaProperty.findByPropertyCode",
                        query = "SELECT p FROM IdealistaProperty p WHERE p.propertyCode = :propertyCode"
                )
        }
)
public class IdealistaProperty {

    @Id
    @Column(name = "property_code")
    private Long propertyCode;

    @Column(name = "operation", nullable = false)
    private String operation;

    @Column(name = "property_type", nullable = false)
    private String propertyType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "size")
    private long size;

    @Column(name = "sub_typology")
    private String subTypology;

    @Column(name = "address")
    private String address;

    @Column(name = "municipality", nullable = false)
    private String municipality;

    @Column(name = "location_id")
    private String locationId;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "first_seen", nullable = false)
    private long firstSeen;

    @Column(name = "last_seen", nullable = false)
    private long lastSeen;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("recordedAt DESC")
    private List<IdealistaPriceRecord> priceRecords = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_phone_number", referencedColumnName = "id")
    private IdealistaContactInformation contactInfo;

    public IdealistaProperty() {}

    public IdealistaProperty(long propertyCode, String operation, String description,
                             long size, String propertyType, String subTypology,
                             String address, String municipality, String locationId,
                             Double latitude, Double longitude, long firstSeen, long lastSeen) {
        this.propertyCode = propertyCode;
        this.operation = operation;
        this.description = description;
        this.size = size;
        this.propertyType = propertyType;
        this.subTypology = subTypology;
        this.address = address;
        this.municipality = municipality;
        this.locationId = locationId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
    }

    public long getPropertyCode() { return propertyCode; }
    public void setPropertyCode(Long propertyCode) { this.propertyCode = propertyCode; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getSubTypology() { return subTypology; }
    public void setSubTypology(String subTypology) { this.subTypology = subTypology; }

    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getMunicipality() { return municipality; }
    public void setMunicipality(String municipality) { this.municipality = municipality; }

    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public long getFirstSeen() { return firstSeen; }
    public void setFirstSeen(long firstSeen) { this.firstSeen = firstSeen; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }

    public List<IdealistaPriceRecord> getPriceRecords() { return priceRecords; }
    public void setPriceRecords(List<IdealistaPriceRecord> priceRecords) { this.priceRecords = priceRecords; }

    public IdealistaContactInformation getContactInfo() { return contactInfo; }
    public void setContactInfo(IdealistaContactInformation contactInfo) { this.contactInfo = contactInfo; }

    @Override
    public String toString() {
        return "IdealistaProperty{" +
                "propertyCode=" + propertyCode +
                ", operation='" + operation + '\'' +
                ", propertyType='" + propertyType + '\'' +
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