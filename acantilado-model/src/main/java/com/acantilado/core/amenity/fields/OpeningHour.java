package com.acantilado.core.amenity.fields;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Duration;
import java.util.Objects;

@JsonSerialize
@JsonDeserialize
public class OpeningHour {
  private static final int MINS_PER_HOUR = 60;
  private static final int HOURS_PER_DAY = 24;
  private static final int MAX_MINS = 59;
  private static final int MAX_HOURS = 23;

  @JsonProperty("openingHour")
  private final int openingHour;

  @JsonProperty("openingMinute")
  private final int openingMinute;

  @JsonProperty("closingHour")
  private final int closingHour;

  @JsonProperty("closingMinute")
  private final int closingMinute;

  @JsonCreator
  public OpeningHour(
      @JsonProperty("openingHour") int openingHour,
      @JsonProperty("openingMinute") int openingMinute,
      @JsonProperty("closingHour") int closingHour,
      @JsonProperty("closingMinute") int closingMinute) {

    validateHour(openingHour, "opening");
    validateHour(closingHour, "closing");
    validateMinute(openingMinute, "opening");
    validateMinute(closingMinute, "closing");

    this.openingHour = openingHour;
    this.openingMinute = openingMinute;
    this.closingHour = closingHour;
    this.closingMinute = closingMinute;
  }

  private void validateHour(int hour, String type) {
    if (hour < 0 || hour > MAX_HOURS) {
      throw new IllegalArgumentException(
          String.format("%s hour must be between 0 and %d, got %d", type, MAX_HOURS, hour));
    }
  }

  private void validateMinute(int minute, String type) {
    if (minute < 0 || minute > MAX_MINS) {
      throw new IllegalArgumentException(
          String.format("%s minute must be between 0 and %d, got %d", type, MAX_MINS, minute));
    }
  }

  public Duration duration() {
    int openingTime = this.openingHour * MINS_PER_HOUR + openingMinute;
    int closingTime = this.closingHour * MINS_PER_HOUR + closingMinute;

    if (closingTime == openingTime) {
      return Duration.ofDays(1);
    }

    if (closingTime > openingTime) {
      return Duration.ofMinutes(closingTime - openingTime);
    }

    int minsUntilMidnight = (MINS_PER_HOUR * HOURS_PER_DAY) - openingTime;
    return Duration.ofMinutes(minsUntilMidnight + closingTime);
  }

  public boolean crossesMidnight() {
    int openingTimeInMinutes = this.openingHour * MINS_PER_HOUR + openingMinute;
    int closingTimeInMinutes = this.closingHour * MINS_PER_HOUR + closingMinute;
    return closingTimeInMinutes < openingTimeInMinutes;
  }

  // Getters
  public int getOpeningHour() {
    return openingHour;
  }

  public int getOpeningMinute() {
    return openingMinute;
  }

  public int getClosingHour() {
    return closingHour;
  }

  public int getClosingMinute() {
    return closingMinute;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OpeningHour that = (OpeningHour) o;
    return openingHour == that.openingHour
        && openingMinute == that.openingMinute
        && closingHour == that.closingHour
        && closingMinute == that.closingMinute;
  }

  @Override
  public int hashCode() {
    return Objects.hash(openingHour, openingMinute, closingHour, closingMinute);
  }

  @Override
  public String toString() {
    return String.format(
        "%02d:%02d-%02d:%02d", openingHour, openingMinute, closingHour, closingMinute);
  }
}
