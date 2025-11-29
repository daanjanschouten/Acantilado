package com.acantilado.core.amenity;

import com.acantilado.core.amenity.fields.OpeningHour;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OpeningHourTest {
    private OpeningHour openingHour;

    @Test
    public void throwsForBadHours() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                new OpeningHour(24, 1, 5, 40));
    }

    @Test
    public void throwsForBadMinutes() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                new OpeningHour(3, 70, 5, 40));
    }

    @Test
    public void returnsCorrectDayTime() {
        openingHour = new OpeningHour(3, 30, 14, 50);
        Assertions.assertEquals((11 * 60) + 20, openingHour.duration().toMinutes());
    }

    @Test
    public void returnsCorrectNightTime() {
        openingHour = new OpeningHour(20, 30, 3, 10);
        Assertions.assertEquals((6 * 60) + 40, openingHour.duration().toMinutes());
    }

    @Test
    public void handles24HourStore() {
        openingHour = new OpeningHour(0, 0, 0, 0);
        Assertions.assertEquals(1, openingHour.duration().toDays());
    }

    @Test
    public void handlesSameTimeNonMidnight() {
        openingHour = new OpeningHour(9, 0, 9, 0);
        Assertions.assertEquals(1, openingHour.duration().toDays());
    }
}