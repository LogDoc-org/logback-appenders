package ru.gang.logdoc.flaps.impl.multiplexers;

import ru.gang.logdoc.flaps.Multiplexer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 13:16
 * logback-adapter ☭ sweat and blood
 */
//  <multiline>false</multiline> <!-- Should we keep multilined logs as is -->
public class SplitByLinebreaks implements Multiplexer {
    @Override
    public List<String> apply(final String s) {
        final String[] array = s.split("\\r?\\n");
        final List<String> list = new ArrayList<>(array.length);
        Collections.addAll(list, array);
        return list;
    }
}
