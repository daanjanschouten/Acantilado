package com.flightdelays.aviation;

import javax.persistence.*;

@Entity
@Table(name = "AIRCRAFT")
public class Aircraft {
    @Id
    @Column(name="aircraft_id")
    private long aircraftId;

    private String model;
    public Aircraft() {
    }

    public Aircraft(long aircraftId, String model) {
        this.aircraftId = aircraftId;
        this.model = model;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
    public long getAircraftId() {
        return aircraftId;
    }

    public void setAircraftId(long aircraftId) {
        this.aircraftId = aircraftId;
    }
}
