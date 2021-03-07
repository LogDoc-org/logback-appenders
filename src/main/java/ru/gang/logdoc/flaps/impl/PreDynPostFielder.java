package ru.gang.logdoc.flaps.impl;

import ru.gang.logdoc.flaps.Fielder;
import ru.gang.logdoc.model.DynamicPosFields;
import ru.gang.logdoc.model.StaticPosFields;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 13:44
 * logback-adapter ☭ sweat and blood
 */
public class PreDynPostFielder implements Fielder {
    private final PreFielder preFielder;
    private final DynFielder dynFielder;
    private final PostFielder postFielder;

    public PreDynPostFielder(final StaticPosFields prefixGroup, final DynamicPosFields dynamicGroup, final StaticPosFields suffixGroup) {
        preFielder = new PreFielder(prefixGroup);
        dynFielder = new DynFielder(dynamicGroup);
        postFielder = new PostFielder(suffixGroup);
    }

    @Override
    public Map<String, String> apply(final String s) {
        final Map<String, String> map = new HashMap<>(0);

        map.putAll(preFielder.apply(s));
        map.putAll(dynFielder.apply(s));
        map.putAll(postFielder.apply(s));

        return map;
    }
}
