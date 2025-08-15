package com.kaiyikang.minitomcat.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DateUtilsTest {
    @Test
    void testParseDateTimeGMT() {
        // Given
        String rfc1123String = "Fri, 13 Oct 2024 10:15:30 GMT";
        ZonedDateTime expectedZdt = ZonedDateTime.of(2023, 10, 13, 10, 15, 30, 0, ZoneOffset.UTC);
        long expectedEpochMilli = expectedZdt.toInstant().toEpochMilli();

        // When
        long actualEpochMilli = DateUtils.parseDateTimeGMT(rfc1123String);

        // Then
        assertEquals(actualEpochMilli, expectedEpochMilli);

    }

    @Test
    void testFormatDateTimeGMT() {

        // Given
        long epochMilli = 1697192130000L;
        String expectedRfc1123String = "Fri, 13 Oct 2023 10:15:30 GMT";

        // When
        String actualFormattedString = DateUtils.formatDateTimeGMT(epochMilli);

        // Then
        assertEquals(expectedRfc1123String, actualFormattedString);

    }
}
