package com.schouten.core.aviation;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import javax.persistence.*;
import java.util.List;
import java.util.Optional;

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
    @Column(name="aircraft_id")
    private String aircraftId;

    private String model;
    public Aircraft() {
    }

    public Aircraft(String aircraftId, String model) {
        this.aircraftId = aircraftId;
        this.model = model;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getAircraftId() {
        return aircraftId;
    }

    public void setAircraftId(String aircraftId) {
        this.aircraftId = aircraftId;
    }
}
