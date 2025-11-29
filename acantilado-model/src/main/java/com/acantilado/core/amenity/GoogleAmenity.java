package com.acantilado.core.amenity;

import com.acantilado.core.amenity.fields.AcantiladoAmenityChain;
import com.acantilado.core.amenity.fields.AcantiladoAmenityType;

import javax.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "google_amenity")
@NamedQueries({
        @NamedQuery(
                name = "com.acantilado.core.amenity.GoogleAmenity.findAll",
                query = "SELECT a FROM GoogleAmenity a"
        ),
        @NamedQuery(
                name = "com.acantilado.core.amenity.GoogleAmenity.findByChain",
                query = "SELECT a FROM GoogleAmenity a WHERE a.chain = :chain"
        )
})
public class GoogleAmenity {
    @Id
    @Column(name = "place_id", length = 255, nullable = false)
    private String placeId;

    @Column(name = "name", length = 500, nullable = false)
    private String name;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private double latitude;

    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "chain", length = 100, nullable = false)
    private AcantiladoAmenityChain chain;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "previous_place_id", length = 255)
    private String previousPlaceId;

    public GoogleAmenity() {
    }

    private GoogleAmenity(Builder builder) {
        this.placeId = requireNonNull(builder.placeId, "placeId");
        this.name = requireNonNull(builder.name, "name");
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
        this.chain = requireNonNull(builder.chain, "chain");
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.previousPlaceId = builder.previousPlaceId;

        validateCoordinates();
    }

    private void validateCoordinates() {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90, got: " + latitude);
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180, got: " + longitude);
        }
    }

    private static <T> T requireNonNull(T obj, String fieldName) {
        if (obj == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        return obj;
    }

    // Getters
    public String getPlaceId() { return placeId; }
    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public AcantiladoAmenityChain getChain() { return chain; }
    public AcantiladoAmenityType getType() { return chain.getAmenityType(); }
    public Instant getCreatedAt() { return createdAt; }
    public String getPreviousPlaceId() { return previousPlaceId; }

    // Setters for JPA
    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setChain(AcantiladoAmenityChain chain) {
        this.chain = chain;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setPreviousPlaceId(String previousPlaceId) {
        this.previousPlaceId = previousPlaceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GoogleAmenity that = (GoogleAmenity) o;
        return Objects.equals(placeId, that.placeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(placeId);
    }

    @Override
    public String toString() {
        return String.format("GoogleAmenity{placeId='%s', name='%s', chain=%s}",
                placeId, name, chain);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String placeId;
        private String name;
        private double latitude;
        private double longitude;
        private AcantiladoAmenityChain chain;
        private Instant createdAt;
        private String previousPlaceId;

        public Builder placeId(String placeId) {
            this.placeId = placeId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder latitude(double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Builder chain(AcantiladoAmenityChain chain) {
            this.chain = chain;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder previousPlaceId(String previousPlaceId) {
            this.previousPlaceId = previousPlaceId;
            return this;
        }

        public GoogleAmenity build() {
            return new GoogleAmenity(this);
        }
    }
}