package com.acantilado.core.amenity.fields;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@JsonSerialize
@JsonDeserialize
public class OpeningHours {
    @JsonProperty("schedule")
    private final EnumMap<DayOfWeek, Optional<OpeningHour>> schedule;

    // Constructor for Jackson
    @JsonCreator
    public OpeningHours(@JsonProperty("schedule") Map<DayOfWeek, Optional<OpeningHour>> schedule) {
        this.schedule = new EnumMap<>(schedule);
        validateAllDaysPresent(this.schedule);
    }

    private OpeningHours(EnumMap<DayOfWeek, Optional<OpeningHour>> schedule) {
        validateAllDaysPresent(schedule);
        this.schedule = new EnumMap<>(schedule);
    }

    private void validateAllDaysPresent(EnumMap<DayOfWeek, Optional<OpeningHour>> schedule) {
        for (DayOfWeek day : DayOfWeek.values()) {
            if (!schedule.containsKey(day)) {
                throw new IllegalStateException("Schedule must contain all days of the week. Missing: " + day);
            }
        }
    }

    public Optional<OpeningHour> getHours(DayOfWeek day) {
        return schedule.get(day);
    }

    public boolean isOpen(DayOfWeek day) {
        return schedule.get(day).isPresent();
    }

    public Duration totalWeeklyHours() {
        return schedule.values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(OpeningHour::duration)
                .reduce(Duration.ZERO, Duration::plus);
    }

    public Duration weekdayHours() {
        return schedule.entrySet().stream()
                .filter(entry -> isWeekday(entry.getKey()))
                .map(Map.Entry::getValue)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(OpeningHour::duration)
                .reduce(Duration.ZERO, Duration::plus);
    }

    public Duration weekendHours() {
        return totalWeeklyHours().minus(weekdayHours());
    }

    public double weekendRatio() {
        Duration total = totalWeeklyHours();
        if (total.isZero()) {
            return 0.0;
        }
        return weekendHours().toMinutes() / (double) total.toMinutes();
    }

    private boolean isWeekday(DayOfWeek day) {
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpeningHours that = (OpeningHours) o;
        return Objects.equals(schedule, that.schedule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schedule);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("OpeningHours{");
        for (DayOfWeek day : DayOfWeek.values()) {
            sb.append(day).append("=");
            schedule.get(day).ifPresentOrElse(
                    hour -> sb.append(hour),
                    () -> sb.append("CLOSED")
            );
            sb.append(", ");
        }
        if (sb.length() > 14) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("}");
        return sb.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final EnumMap<DayOfWeek, Optional<OpeningHour>> schedule = new EnumMap<>(DayOfWeek.class);

        public Builder() {
            for (DayOfWeek day : DayOfWeek.values()) {
                schedule.put(day, Optional.empty());
            }
        }

        public Builder monday(OpeningHour hours) {
            return day(DayOfWeek.MONDAY, hours);
        }

        public Builder tuesday(OpeningHour hours) {
            return day(DayOfWeek.TUESDAY, hours);
        }

        public Builder wednesday(OpeningHour hours) {
            return day(DayOfWeek.WEDNESDAY, hours);
        }

        public Builder thursday(OpeningHour hours) {
            return day(DayOfWeek.THURSDAY, hours);
        }

        public Builder friday(OpeningHour hours) {
            return day(DayOfWeek.FRIDAY, hours);
        }

        public Builder saturday(OpeningHour hours) {
            return day(DayOfWeek.SATURDAY, hours);
        }

        public Builder sunday(OpeningHour hours) {
            return day(DayOfWeek.SUNDAY, hours);
        }

        public Builder mondayClosed() {
            return dayClosed(DayOfWeek.MONDAY);
        }

        public Builder tuesdayClosed() {
            return dayClosed(DayOfWeek.TUESDAY);
        }

        public Builder wednesdayClosed() {
            return dayClosed(DayOfWeek.WEDNESDAY);
        }

        public Builder thursdayClosed() {
            return dayClosed(DayOfWeek.THURSDAY);
        }

        public Builder fridayClosed() {
            return dayClosed(DayOfWeek.FRIDAY);
        }

        public Builder saturdayClosed() {
            return dayClosed(DayOfWeek.SATURDAY);
        }

        public Builder sundayClosed() {
            return dayClosed(DayOfWeek.SUNDAY);
        }

        public Builder day(DayOfWeek day, OpeningHour hours) {
            schedule.put(day, Optional.of(hours));
            return this;
        }

        public Builder dayClosed(DayOfWeek day) {
            schedule.put(day, Optional.empty());
            return this;
        }

        public Builder weekdays(OpeningHour hours) {
            return monday(hours)
                    .tuesday(hours)
                    .wednesday(hours)
                    .thursday(hours)
                    .friday(hours);
        }

        public Builder weekend(OpeningHour hours) {
            return saturday(hours).sunday(hours);
        }

        public Builder allDays(OpeningHour hours) {
            return weekdays(hours).weekend(hours);
        }

        public OpeningHours build() {
            return new OpeningHours(schedule);
        }
    }
}