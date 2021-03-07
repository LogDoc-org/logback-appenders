package ru.gang.logdoc.flaps.impl;

import ru.gang.logdoc.flaps.Fielder;
import ru.gang.logdoc.model.DynamicPosFields;
import ru.gang.logdoc.model.StaticPosFields;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 13:43
 * logback-adapter ☭ sweat and blood
 */
public class DynPostFielder implements Fielder {
    private final PostFielder postFielder;
    private final DynFielder dynFielder;

    public DynPostFielder(final DynamicPosFields dynamicGroup, final StaticPosFields suffixGroup) {
        dynFielder = new DynFielder(dynamicGroup);
        postFielder = new PostFielder(suffixGroup);
    }

    @Override
    public Map<String, String> apply(final String s) {
        final Map<String, String> map = new HashMap<>(0);

        map.putAll(dynFielder.apply(s));
        map.putAll(postFielder.apply(s));

        return map;
    }

}
