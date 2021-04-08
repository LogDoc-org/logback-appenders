package ru.gang.logdoc.flaps.impl;

import ru.gang.logdoc.flaps.Timer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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
        return Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDateTime().format(logTimeFormat);
    }
}
