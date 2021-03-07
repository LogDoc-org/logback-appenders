package ru.gang.logdoc.flaps.impl;

import ru.gang.logdoc.flaps.Cleaner;
import ru.gang.logdoc.model.DynamicPosFields;
import ru.gang.logdoc.model.StaticPosFields;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 14:44
 * logback-adapter ☭ sweat and blood
 */
public class PreDynPostCleaner implements Cleaner {
    private final PreCleaner preCleaner;
    private final DynCleaner dynCleaner;
    private final PostCleaner postCleaner;

    public PreDynPostCleaner(final StaticPosFields staticPrefix, final DynamicPosFields dynamicFields, final StaticPosFields staticSuffix) {
        preCleaner = new PreCleaner(staticPrefix);
        dynCleaner = new DynCleaner(dynamicFields);
        postCleaner = new PostCleaner(staticSuffix);
    }

    @Override
    public String apply(final String s) {
        return preCleaner.andThen(dynCleaner).andThen(postCleaner).apply(s);
    }
}
