package ru.gang.logdoc.flaps.impl;

import ru.gang.logdoc.flaps.Cleaner;
import ru.gang.logdoc.model.StaticPosFields;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 13:54
 * logback-adapter ☭ sweat and blood
 */
public class PostCleaner implements Cleaner {
    private final StaticPosFields staticSuffix;

    public PostCleaner(final StaticPosFields staticSuffix) {
        this.staticSuffix = staticSuffix;
    }

    @Override
    public String apply(final String event) {
        if (!event.endsWith(staticSuffix.getCloseMark()))
            return event;

        final int closeIdx = event.lastIndexOf(staticSuffix.getOpenMark());

        if (closeIdx <= 0 || closeIdx == event.length() - 1)
            return event;

        return event.substring(0, closeIdx);
    }
}
