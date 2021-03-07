package ru.gang.logdoc.flaps.impl;

import ru.gang.logdoc.flaps.Cleaner;
import ru.gang.logdoc.model.DynamicPosFields;
import ru.gang.logdoc.model.StaticPosFields;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 14:45
 * logback-adapter ☭ sweat and blood
 */
public class DynPostCleaner implements Cleaner {
    private final DynCleaner dynCleaner;
    private final PostCleaner postCleaner;

    public DynPostCleaner(final DynamicPosFields dynamicFields, final StaticPosFields staticSuffix) {
        dynCleaner = new DynCleaner(dynamicFields);
        postCleaner = new PostCleaner(staticSuffix);
    }

    @Override
    public String apply(final String s) {
        return dynCleaner.andThen(postCleaner).apply(s);
    }
}
