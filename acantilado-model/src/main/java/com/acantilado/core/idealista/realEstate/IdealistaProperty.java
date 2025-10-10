package com.acantilado.core.idealista.realEstate;

import com.acantilado.core.idealista.IdealistaContactInformation;
import com.acantilado.core.idealista.priceRecords.IdealistaPropertyPriceRecord;

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
public class IdealistaProperty implements IdealistaRealEstate<IdealistaPropertyPriceRecord> {

    @Id
    @Column(name = "property_code")
    private Long propertyCode;

    @Column(name = "operation", nullable = false)
    private String operation;

    @Column(name = "property_type")
    private String propertyType;

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

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "status")
    private String status;

    @Column(name = "new_development")
    private Boolean newDevelopment;

    @Column(name = "new_property")
    private Boolean newProperty;

    // Property-specific fields
    @Column(name = "rooms")
    private Integer rooms;

    @Column(name = "bathrooms")
    private Integer bathrooms;

    @Column(name = "floor")
    private String floor;

    @Column(name = "has_lift")
    private Boolean hasLift;

    @Column(name = "has_parking_space")
    private Boolean hasParkingSpace;

    @Column(name = "parking_included_in_price")
    private Boolean parkingIncludedInPrice;

    @Column(name = "has_terrace")
    private Boolean hasTerrace;

    @Column(name = "has_garden")
    private Boolean hasGarden;

    @Column(name = "has_pool")
    private Boolean hasPool;

    @Column(name = "has_air_conditioning")
    private Boolean hasAirConditioning;

    @Column(name = "has_box_room")
    private Boolean hasBoxRoom;

    @Column(name = "energy_certificate")
    private String energyCertificate;

    @Column(name = "first_seen", nullable = false)
    private Long firstSeen;

    @Column(name = "last_seen", nullable = false)
    private Long lastSeen;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("recordedAt DESC")
    private List<IdealistaPropertyPriceRecord> priceRecords = new ArrayList<>();

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

    // Getters and Setters
    public Long getPropertyCode() { return propertyCode; }
    public void setPropertyCode(Long propertyCode) { this.propertyCode = propertyCode; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }

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

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getNewDevelopment() { return newDevelopment; }
    public void setNewDevelopment(Boolean newDevelopment) { this.newDevelopment = newDevelopment; }

    public Boolean getNewProperty() { return newProperty; }
    public void setNewProperty(Boolean newProperty) { this.newProperty = newProperty; }

    public Integer getRooms() { return rooms; }
    public void setRooms(Integer rooms) { this.rooms = rooms; }

    public Integer getBathrooms() { return bathrooms; }
    public void setBathrooms(Integer bathrooms) { this.bathrooms = bathrooms; }

    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }

    public Boolean getHasLift() { return hasLift; }
    public void setHasLift(Boolean hasLift) { this.hasLift = hasLift; }

    public Boolean getHasParkingSpace() { return hasParkingSpace; }
    public void setHasParkingSpace(Boolean hasParkingSpace) { this.hasParkingSpace = hasParkingSpace; }

    public Boolean getParkingIncludedInPrice() { return parkingIncludedInPrice; }
    public void setParkingIncludedInPrice(Boolean parkingIncludedInPrice) { this.parkingIncludedInPrice = parkingIncludedInPrice; }

    public Boolean getHasTerrace() { return hasTerrace; }
    public void setHasTerrace(Boolean hasTerrace) { this.hasTerrace = hasTerrace; }

    public Boolean getHasGarden() { return hasGarden; }
    public void setHasGarden(Boolean hasGarden) { this.hasGarden = hasGarden; }

    public Boolean getHasPool() { return hasPool; }
    public void setHasPool(Boolean hasPool) { this.hasPool = hasPool; }

    public Boolean getHasAirConditioning() { return hasAirConditioning; }
    public void setHasAirConditioning(Boolean hasAirConditioning) { this.hasAirConditioning = hasAirConditioning; }

    public Boolean getHasBoxRoom() { return hasBoxRoom; }
    public void setHasBoxRoom(Boolean hasBoxRoom) { this.hasBoxRoom = hasBoxRoom; }

    public String getEnergyCertificate() { return energyCertificate; }
    public void setEnergyCertificate(String energyCertificate) { this.energyCertificate = energyCertificate; }

    public Long getFirstSeen() { return firstSeen; }
    public void setFirstSeen(Long firstSeen) { this.firstSeen = firstSeen; }

    public Long getLastSeen() { return lastSeen; }
    public void setLastSeen(Long lastSeen) { this.lastSeen = lastSeen; }

    public List<IdealistaPropertyPriceRecord> getPriceRecords() { return priceRecords; }
    public void setPriceRecords(List<IdealistaPropertyPriceRecord> priceRecords) { this.priceRecords = priceRecords; }

    public IdealistaContactInformation getContactInfo() { return contactInfo; }
    public void setContactInfo(IdealistaContactInformation contactInfo) { this.contactInfo = contactInfo; }

    @Override
    public String toString() {
        return "IdealistaProperty{" +
                "propertyCode=" + propertyCode +
                ", operation='" + operation + '\'' +
                ", propertyType='" + propertyType + '\'' +
                ", size=" + size +
                ", municipality='" + municipality + '\'' +
                ", rooms=" + rooms +
                ", bathrooms=" + bathrooms +
                '}';
    }
}