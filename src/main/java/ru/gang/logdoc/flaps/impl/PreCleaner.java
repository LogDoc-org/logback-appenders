package ru.gang.logdoc.flaps.impl;

import ru.gang.logdoc.flaps.Cleaner;
import ru.gang.logdoc.model.StaticPosFields;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 13:53
 * logback-adapter ☭ sweat and blood
 */
public class PreCleaner implements Cleaner {
    private final StaticPosFields staticPrefix;

    public PreCleaner(final StaticPosFields staticPrefix) {
        this.staticPrefix = staticPrefix;
    }

    @Override
    public String apply(final String event) {
        if (!event.startsWith(staticPrefix.getOpenMark()))
            return event;

        final int closeIdx = event.indexOf(staticPrefix.getCloseMark(), staticPrefix.getOpenMark().length() + 1);

        if (closeIdx <= 0)
            return event;

        return event.substring(closeIdx + staticPrefix.getCloseMark().length());
    }
}
