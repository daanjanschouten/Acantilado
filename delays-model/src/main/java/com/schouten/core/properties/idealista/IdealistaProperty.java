package com.schouten.core.properties.idealista;

import javax.persistence.*;
import java.math.BigDecimal;
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
    private String size;

    @Column(name = "sub_typology")
    private String subTypology;

    @Column(name = "address")
    private String address;

    @Column(name = "municipality", nullable = false)
    private String municipality;

    @Column(name = "location_id")
    private String locationId;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("recordedAt DESC")
    private List<IdealistaPriceRecord> priceRecords = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_phone_number", referencedColumnName = "phone_number")
    private IdealistaContactInformation contactInfo;

    public IdealistaProperty() {}

    public IdealistaProperty(Long propertyCode, String operation, String description,
                             String size, String propertyType, String subTypology,
                             String address, String municipality, String locationId,
                             String latitude, String longitude) {
        this.propertyCode = propertyCode;
        this.operation = operation;
        this.description = description;
        this.size = size;
        this.propertyType = propertyType;
        this.subTypology = subTypology;
        this.address = address;
        this.municipality = municipality;
        this.locationId = locationId;
        this.latitude = latitude != null ? new BigDecimal(latitude) : null;
        this.longitude = longitude != null ? new BigDecimal(longitude) : null;
    }

    public Long getPropertyCode() { return propertyCode; }
    public void setPropertyCode(Long propertyCode) { this.propertyCode = propertyCode; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

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

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public List<IdealistaPriceRecord> getPriceRecords() { return priceRecords; }
    public void setPriceRecords(List<IdealistaPriceRecord> priceRecords) { this.priceRecords = priceRecords; }

    public IdealistaContactInformation getContactInfo() { return contactInfo; }
    public void setContactInfo(IdealistaContactInformation contactInfo) { this.contactInfo = contactInfo; }
}