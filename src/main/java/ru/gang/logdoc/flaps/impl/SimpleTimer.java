package ru.gang.logdoc.flaps.impl;

import ru.gang.logdoc.flaps.Timer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;

import static ru.gang.logdoc.structs.utils.Tools.logTimeFormat;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 12:59
 * logback-adapter ☭ sweat and blood
 */
public class SimpleTimer implements Timer {
    private static final ZoneId zone = ZoneId.systemDefault();

    @Override
    public String apply(final Long timestamp) {
        final LocalDateTime ldt = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDateTime();

        return String.valueOf(ldt.getYear()).substring(2)
                + pad(ldt.getMonthValue(), 10)
                + pad(ldt.getDayOfMonth(), 10)
                + pad(ldt.getHour(), 10)
                + pad(ldt.getMinute(), 10)
                + pad(ldt.getSecond(), 10)
                + pad(ldt.get(ChronoField.MILLI_OF_SECOND), 1000)
                + pad(ldt.get(ChronoField.NANO_OF_SECOND), 1000);
    }

    private String pad(final int i, final int rad) {
        if (i >= rad)
            return String.valueOf(i);

        if (i == 0)
            return "0" + String.valueOf(rad).substring(1);

        if (rad == 10)
            return "0" + i;

        return (i < 10 ? "00" : i < 100 ? "0" : "0") + i;
    }
}
