package com.acantilado.core.amenity.fields;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.DayOfWeek;
import java.time.Duration;
import java.util.*;

@JsonSerialize
@JsonDeserialize
public class OpeningHours {

  @JsonProperty("schedule")
  private final EnumMap<DayOfWeek, List<OpeningHour>> schedule;

  @JsonCreator
  public OpeningHours(@JsonProperty("schedule") Map<DayOfWeek, List<OpeningHour>> schedule) {
    this.schedule = new EnumMap<>(DayOfWeek.class);
    for (DayOfWeek day : DayOfWeek.values()) {
      this.schedule.put(day, new ArrayList<>(schedule.getOrDefault(day, List.of())));
    }
  }

  private OpeningHours(EnumMap<DayOfWeek, List<OpeningHour>> schedule) {
    this.schedule = schedule;
  }

  public List<OpeningHour> getHours(DayOfWeek day) {
    return schedule.get(day);
  }

  public boolean isOpen(DayOfWeek day) {
    return !schedule.get(day).isEmpty();
  }

  public Duration totalWeeklyHours() {
    return schedule.values().stream()
        .flatMap(List::stream)
        .map(OpeningHour::duration)
        .reduce(Duration.ZERO, Duration::plus);
  }

  public Duration weekdayHours() {
    return schedule.entrySet().stream()
        .filter(e -> e.getKey().getValue() <= 5)
        .flatMap(e -> e.getValue().stream())
        .map(OpeningHour::duration)
        .reduce(Duration.ZERO, Duration::plus);
  }

  public Duration weekendHours() {
    return totalWeeklyHours().minus(weekdayHours());
  }

  public double weekendRatio() {
    Duration total = totalWeeklyHours();
    if (total.isZero()) return 0.0;
    return weekendHours().toMinutes() / (double) total.toMinutes();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof OpeningHours that && Objects.equals(schedule, that.schedule);
  }

  @Override
  public int hashCode() {
    return Objects.hash(schedule);
  }

  @Override
  public String toString() {
    return "OpeningHours" + schedule;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final EnumMap<DayOfWeek, List<OpeningHour>> schedule = new EnumMap<>(DayOfWeek.class);

    public Builder() {
      for (DayOfWeek day : DayOfWeek.values()) {
        schedule.put(day, new ArrayList<>());
      }
    }

    public Builder add(DayOfWeek day, OpeningHour hours) {
      schedule.get(day).add(hours);
      return this;
    }

    public Builder closed(DayOfWeek day) {
      schedule.get(day).clear();
      return this;
    }

    public OpeningHours build() {
      return new OpeningHours(schedule);
    }
  }
}
