package ru.gang.logdoc.flaps.impl;

import ru.gang.logdoc.flaps.Cleaner;
import ru.gang.logdoc.model.DynamicPosFields;
import ru.gang.logdoc.model.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 13:55
 * logback-adapter ☭ sweat and blood
 */
public class DynCleaner implements Cleaner {
    private final List<Function<String, String>> cleaners;

    public DynCleaner(final DynamicPosFields dynamicFields) {
        cleaners = new ArrayList<>(0);

        for (final Field f : dynamicFields.getFields())
            if (dynamicFields.isClearMarks() && dynamicFields.isClearValues())
                cleaners.add(event -> {
                    final int b = event.indexOf(f.getOpenMark());
                    final int e = b < 0 ? -1 : event.indexOf(f.getCloseMark(), b + f.getOpenMark().length());

                    if (b >= 0 && e > b + 1)
                        return event.substring(b, e + f.getCloseMark().length());

                    return event;
                });
            else if (dynamicFields.isClearMarks())
                cleaners.add(event -> {
                    final int b = event.indexOf(f.getOpenMark());
                    final int e = b < 0 ? -1 : event.indexOf(f.getCloseMark(), b + f.getOpenMark().length());

                    if (b >= 0 && e > b + 1)
                        return event.substring(0, b) + event.substring(b + f.getOpenMark().length(), e) + event.substring(e + f.getCloseMark().length());

                    return event;
                });
            else if (dynamicFields.isClearValues())
                cleaners.add(event -> {
                    final int b = event.indexOf(f.getOpenMark());
                    final int e = b < 0 ? -1 : event.indexOf(f.getCloseMark(), b + f.getOpenMark().length());

                    if (b >= 0 && e > b + 1)
                        return event.substring(0, b + f.getOpenMark().length()) + event.substring(e);

                    return event;
                });
    }

    @Override
    public String apply(final String event) {
        String e = event;
        for (final Function<String, String> c : cleaners)
            e = c.apply(e);

        return e;
    }
}
