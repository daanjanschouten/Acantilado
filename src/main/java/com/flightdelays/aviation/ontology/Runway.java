package com.flightdelays.aviation.ontology;

import javax.persistence.*;

@Entity
@Table(name = "RUNWAY")
public class Runway {
    @Id
    @Column(name = "runway_id")
    private long runwayId;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "airport_id")
    private Airport airport;

    public Runway() {
    }

    public Runway(long runwayId, Airport airport) {
        this.runwayId = runwayId;
        this.airport = airport;
    }

    public long getRunwayId() {
        return runwayId;
    }

    public void setRunwayId(long runwayId) {
        this.runwayId = runwayId;
    }

    public Airport getAirport() {
        return airport;
    }

    public void setAirport(Airport airport) {
        this.airport = airport;
    }
}
