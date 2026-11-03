package com.camp.app.attendance;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AttendanceCalculatorTest {

    @Test
    void countsExceptionsWithoutUsingCourse() {
        AttendanceCalculator.Counts counts = AttendanceCalculator.fromTypes(
                List.of("ABSENT", "LATE", "SICK_LEAVE", "PERSONAL_LEAVE", "EARLY_LEAVE"));
        assertEquals(1, counts.absent());
        assertEquals(1, counts.late());
        assertEquals(2, counts.leave());
        assertEquals(1, counts.earlyLeave());
    }

    @Test
    void mapsLegacyStatus() {
        assertEquals(1, AttendanceCalculator.legacyStatus("ABSENT"));
        assertEquals(2, AttendanceCalculator.legacyStatus("LATE"));
        assertEquals("EARLY_LEAVE", AttendanceCalculator.exceptionTypeOf(4));
        assertNull(AttendanceCalculator.legacyStatus("UNKNOWN"));
    }
}
