package com.schouten.core.aviation;

import javax.persistence.*;

@Entity
@Table(name = "AIRCRAFT")
@NamedQueries(
        {
                @NamedQuery(
                        name = "com.flightdelays.core.Aircraft.findAll",
                        query = "SELECT a FROM Aircraft a"
                )
        }
)
public class Aircraft {
    @Id
    @Column(name="hex_icao_id")
    private String hexIcaoId;

    @Column(name = "iata_type", nullable = false)
    private String iataType;

    @Column(name = "registration_date", nullable = false)
    private String registrationDate;

    @Column(name = "owner_iata_id", nullable = false)
    private String ownerIataId;

    public Aircraft(String hexIcaoId, String iataType, String registrationDate, String ownerIataId) {
        this.hexIcaoId = hexIcaoId;
        this.iataType = iataType;
        this.registrationDate = registrationDate;
        this.ownerIataId = ownerIataId;
    }

    public Aircraft() {
    }

    public String getHexIcaoId() {
        return hexIcaoId;
    }

    public void setHexIcaoId(String hexIcaoId) {
        this.hexIcaoId = hexIcaoId;
    }

    public String getIataType() {
        return iataType;
    }

    public void setIataType(String iataType) {
        this.iataType = iataType;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getOwnerIataId() {
        return ownerIataId;
    }

    public void setOwnerIataId(String ownerIataId) {
        this.ownerIataId = ownerIataId;
    }

    @Override
    public String toString() {
        return "Aircraft{" +
                "hexIcaoId='" + hexIcaoId + '\'' +
                ", iataType='" + iataType + '\'' +
                ", registrationDate='" + registrationDate + '\'' +
                ", ownerIataId='" + ownerIataId + '\'' +
                '}';
    }
}
