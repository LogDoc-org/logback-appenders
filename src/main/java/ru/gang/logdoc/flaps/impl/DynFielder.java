package ru.gang.logdoc.flaps.impl;

import ru.gang.logdoc.flaps.Fielder;
import ru.gang.logdoc.model.DynamicPosFields;
import ru.gang.logdoc.model.Field;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 13:39
 * logback-adapter ☭ sweat and blood
 */
public class DynFielder implements Fielder {
    private final DynamicPosFields fields;

    public DynFielder(final DynamicPosFields fields) {
        this.fields = fields;
    }

    @Override
    public Map<String, String> apply(final String event) {
        final Map<String, String> map = new HashMap<>(0);

        for (final Field f : fields.getFields()) {
            final int b = event.indexOf(f.getOpenMark());
            final int e = b < 0 ? -1 : event.indexOf(f.getCloseMark(), b + f.getOpenMark().length());

            if (b >= 0 && e > b + 1)
                map.put(f.getFieldName(), event.substring(b + f.getOpenMark().length(), e));
        }

        return map;
    }
}
