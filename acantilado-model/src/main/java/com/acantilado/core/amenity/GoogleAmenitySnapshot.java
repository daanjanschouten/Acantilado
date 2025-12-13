package com.acantilado.core.amenity;

import com.acantilado.core.amenity.fields.GoogleAmenityStatus;
import com.acantilado.core.amenity.fields.OpeningHours;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "google_amenity_snapshot")
@NamedQueries({
  @NamedQuery(
      name = "com.acantilado.core.amenity.GoogleAmenitySnapshot.findAll",
      query = "SELECT s FROM GoogleAmenitySnapshot s ORDER BY s.lastSeen DESC")
})
public class GoogleAmenitySnapshot {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "place_id", length = 255, nullable = false)
  private String placeId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 50, nullable = false)
  private GoogleAmenityStatus status;

  @JdbcTypeCode(SqlTypes.JSON) // ← ADD THIS
  @Column(name = "opening_hours", nullable = false, columnDefinition = "jsonb")
  private OpeningHours openingHours;

  @Column(name = "rating")
  private Double rating;

  @Column(name = "user_rating_count")
  private Integer userRatingCount;

  @Column(name = "first_seen", nullable = false, updatable = false)
  @CreationTimestamp // Hibernate generates this
  private Instant firstSeen;

  @Column(name = "last_seen", nullable = false)
  @UpdateTimestamp // Hibernate updates this automatically
  private Instant lastSeen;

  // No-arg constructor for JPA
  public GoogleAmenitySnapshot() {}

  private GoogleAmenitySnapshot(Builder builder) {
    this.placeId = requireNonNull(builder.placeId, "placeId");
    this.status = requireNonNull(builder.status, "status");
    this.openingHours = requireNonNull(builder.openingHours, "openingHours");
    this.rating = builder.rating;
    this.userRatingCount = builder.userRatingCount;
    this.firstSeen = requireNonNull(builder.firstSeen, "firstSeen");
    this.lastSeen = requireNonNull(builder.lastSeen, "lastSeen");

    validateTimeRange();
  }

  private void validateTimeRange() {
    if (lastSeen.isBefore(firstSeen)) {
      throw new IllegalArgumentException(
          "lastSeen cannot be before firstSeen: " + lastSeen + " < " + firstSeen);
    }
  }

  private static <T> T requireNonNull(T obj, String fieldName) {
    if (obj == null) {
      throw new IllegalArgumentException(fieldName + " cannot be null");
    }
    return obj;
  }

  // Getters
  public Long getId() {
    return id;
  }

  public String getPlaceId() {
    return placeId;
  }

  public GoogleAmenityStatus getStatus() {
    return status;
  }

  public OpeningHours getOpeningHours() {
    return openingHours;
  }

  public Optional<Double> getRating() {
    return Optional.ofNullable(rating);
  }

  public Optional<Integer> getUserRatingCount() {
    return Optional.ofNullable(userRatingCount);
  }

  public Instant getFirstSeen() {
    return firstSeen;
  }

  public Instant getLastSeen() {
    return lastSeen;
  }

  // Setters for JPA
  public void setId(Long id) {
    this.id = id;
  }

  public void setPlaceId(String placeId) {
    this.placeId = placeId;
  }

  public void setStatus(GoogleAmenityStatus status) {
    this.status = status;
  }

  public void setOpeningHours(OpeningHours openingHours) {
    this.openingHours = openingHours;
  }

  public void setRating(Double rating) {
    this.rating = rating;
  }

  public void setUserRatingCount(Integer userRatingCount) {
    this.userRatingCount = userRatingCount;
  }

  public void setFirstSeen(Instant firstSeen) {
    this.firstSeen = firstSeen;
  }

  public void setLastSeen(Instant lastSeen) {
    this.lastSeen = lastSeen;
  }

  public boolean dataMatches(GoogleAmenitySnapshot other) {
    if (!this.placeId.equals(other.placeId)) {
      return false;
    }

    return this.status == other.status
        && Objects.equals(this.openingHours, other.openingHours)
        && Objects.equals(this.rating, other.rating);
  }

  public GoogleAmenitySnapshot withUpdatedMetadata(
      Integer newUserRatingCount, Instant newLastSeen) {
    return GoogleAmenitySnapshot.builder()
        .placeId(this.placeId)
        .status(this.status)
        .openingHours(this.openingHours)
        .rating(this.rating)
        .userRatingCount(newUserRatingCount)
        .firstSeen(this.firstSeen)
        .lastSeen(newLastSeen)
        .build();
  }

  public boolean hasChangedFrom(GoogleAmenitySnapshot previous) {
    return !dataMatches(previous);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GoogleAmenitySnapshot that = (GoogleAmenitySnapshot) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return String.format(
        "GoogleAmenitySnapshot{id=%d, placeId='%s', status=%s, rating=%s (%d reviews), firstSeen=%s, lastSeen=%s}",
        id, placeId, status, rating, userRatingCount, firstSeen, lastSeen);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String placeId;
    private GoogleAmenityStatus status;
    private OpeningHours openingHours;
    private Double rating;
    private Integer userRatingCount;
    private Instant firstSeen;
    private Instant lastSeen;

    public Builder placeId(String placeId) {
      this.placeId = placeId;
      return this;
    }

    public Builder status(GoogleAmenityStatus status) {
      this.status = status;
      return this;
    }

    public Builder openingHours(OpeningHours openingHours) {
      this.openingHours = openingHours;
      return this;
    }

    public Builder rating(Double rating) {
      this.rating = rating;
      return this;
    }

    public Builder userRatingCount(Integer userRatingCount) {
      this.userRatingCount = userRatingCount;
      return this;
    }

    public Builder firstSeen(Instant firstSeen) {
      this.firstSeen = firstSeen;
      return this;
    }

    public Builder lastSeen(Instant lastSeen) {
      this.lastSeen = lastSeen;
      return this;
    }

    public Builder seenNow() {
      Instant now = Instant.now();
      this.firstSeen = now;
      this.lastSeen = now;
      return this;
    }

    public GoogleAmenitySnapshot build() {
      return new GoogleAmenitySnapshot(this);
    }
  }
}
