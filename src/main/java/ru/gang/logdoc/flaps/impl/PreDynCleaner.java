package ru.gang.logdoc.flaps.impl;

import ru.gang.logdoc.flaps.Cleaner;
import ru.gang.logdoc.model.DynamicPosFields;
import ru.gang.logdoc.model.StaticPosFields;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 14:45
 * logback-adapter ☭ sweat and blood
 */
public class PreDynCleaner implements Cleaner {
    private final PreCleaner preCleaner;
    private final DynCleaner dynCleaner;

    public PreDynCleaner(final StaticPosFields staticPrefix, final DynamicPosFields dynamicFields) {
        preCleaner = new PreCleaner(staticPrefix);
        dynCleaner = new DynCleaner(dynamicFields);
    }

    @Override
    public String apply(final String s) {
        return preCleaner.andThen(dynCleaner).apply(s);
    }
}
