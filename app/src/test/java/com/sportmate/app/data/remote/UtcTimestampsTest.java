package com.sportmate.app.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class UtcTimestampsTest {
    private static final long INSTANT = 4_094_852_400_000L; // 2099-10-05T03:00:00Z

    @Test
    public void parsesBackendInstantsWithAndWithoutFractions() {
        assertEquals(INSTANT, UtcTimestamps.parse("2099-10-05T03:00:00Z"));
        assertEquals(INSTANT + 123, UtcTimestamps.parse("2099-10-05T03:00:00.1234567Z"));
        assertEquals(INSTANT + 500, UtcTimestamps.parse("2099-10-05T03:00:00.5Z"));
        // The fields are UTC by contract even if the zone designator is missing.
        assertEquals(INSTANT, UtcTimestamps.parse("2099-10-05T03:00:00"));
    }

    @Test
    public void rejectsOtherFormats() {
        assertThrows(IllegalStateException.class, () -> UtcTimestamps.parse(null));
        assertThrows(IllegalStateException.class, () -> UtcTimestamps.parse("2099-10-05T10:00:00+07:00"));
        assertThrows(IllegalStateException.class, () -> UtcTimestamps.parse("2099-13-05T03:00:00Z"));
    }

    @Test
    public void formatsWholeMinuteUtcInstants() {
        assertEquals("2099-10-05T03:00:00Z", UtcTimestamps.format(INSTANT));
        assertEquals("2099-10-05T04:30:00Z", UtcTimestamps.format(INSTANT + 90 * 60_000L));
    }
}
