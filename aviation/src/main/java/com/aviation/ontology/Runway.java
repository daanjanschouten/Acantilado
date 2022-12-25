package com.aviation.ontology;

import javax.persistence.*;

@Entity
@Table(name = "RUNWAY")
@NamedQueries(
        {
                @NamedQuery(
                        name = "com.flightdelays.core.Runway.findAll",
                        query = "SELECT r FROM Runway r"
                )
        }
)
public class Runway {
    @Id
    @Column(name = "runway_id")
    private String runwayId;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "airport_id")
    private Airport airport;

    public Runway() {
    }

    public Runway(String runwayId, Airport airport) {
        this.runwayId = runwayId;
        this.airport = airport;
    }

    public String getRunwayId() {
        return runwayId;
    }

    public void setRunwayId(String runwayId) {
        this.runwayId = runwayId;
    }

    public Airport getAirport() {
        return airport;
    }

    public void setAirport(Airport airport) {
        this.airport = airport;
    }
}
