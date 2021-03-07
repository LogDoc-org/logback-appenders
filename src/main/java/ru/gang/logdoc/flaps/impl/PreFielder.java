package ru.gang.logdoc.flaps.impl;

import ru.gang.logdoc.flaps.Fielder;
import ru.gang.logdoc.model.Field;
import ru.gang.logdoc.model.StaticPosFields;

import java.util.*;
import java.util.function.BiFunction;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 13:28
 * logback-adapter ☭ sweat and blood
 */
public class PreFielder implements Fielder {
    private final StaticPosFields staticPrefix;

    private final BiFunction<String, Integer, Boolean> matcher;

    public PreFielder(final StaticPosFields staticPrefix) {
        this.staticPrefix = staticPrefix;

        matcher = staticPrefix.getSeparator().length() > 1
                ? (log, pos) -> {
            int e = pos;
            for (int j = 0; j < staticPrefix.getSeparator().length(); j++, e++)
                if (log.charAt(e) != staticPrefix.getSeparator().charAt(j))
                    return false;
            return true;
        }
                : (log, pos) -> staticPrefix.getSeparator().charAt(0) == log.charAt(pos);
    }

    @Override
    public Map<String, String> apply(final String event) {
        if (!event.startsWith(staticPrefix.getOpenMark()))
            return Collections.emptyMap();

        final int closeIdx = event.indexOf(staticPrefix.getCloseMark(), staticPrefix.getOpenMark().length() + 1);

        if (closeIdx <= 0)
            return Collections.emptyMap();

        final String values = event.substring(staticPrefix.getOpenMark().length(), closeIdx);

        return extractFields(new HashMap<>(), values);

    }

    private Map<String, String> extractFields(final Map<String, String> map, final String values) {
        if (values.trim().isEmpty())
            return map;

        int from = 0;
        final Iterator<Field> iterator = staticPrefix.getFields().iterator();

        for (int i = 0; i < values.length(); i++) {
            if (Character.isISOControl(values.charAt(i)))
                return map;

            if ((i + 1) == values.length() || matcher.apply(values, i)) {
                map.put(iterator.next().getFieldName(), values.substring(from, Math.max(from, i - 1)));

                i += staticPrefix.getSeparator().length() - 1;
                from = i + 1;
            }
        }

        return map;
    }
}
