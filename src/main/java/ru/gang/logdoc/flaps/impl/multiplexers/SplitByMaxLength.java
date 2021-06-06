package ru.gang.logdoc.flaps.impl.multiplexers;

import ru.gang.logdoc.flaps.Multiplexer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 13:19
 * logback-adapter ☭ sweat and blood
 */

//  <multiline>true</multiline> <!-- Should we keep multilined logs as is -->
//
public class SplitByMaxLength implements Multiplexer {

    private final int maxSize;

    public SplitByMaxLength(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public List<String> apply(final String s) {
        List<String> list = new ArrayList<>((s.length() + maxSize - 1) / maxSize);
        for (int start = 0; start < s.length(); start += maxSize) {
            list.add(s.substring(start, Math.min(s.length(), start + maxSize)));
        }
        return list;
    }
}
