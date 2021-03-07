package ru.gang.logdoc.flaps.impl;

import ru.gang.logdoc.flaps.Cleaner;
import ru.gang.logdoc.model.DynamicPosFields;
import ru.gang.logdoc.model.StaticPosFields;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 14:45
 * logback-adapter ☭ sweat and blood
 */
public class PrePostCleaner implements Cleaner {
    private final PreCleaner preCleaner;
    private final PostCleaner postCleaner;

    public PrePostCleaner(final StaticPosFields staticPrefix, final StaticPosFields staticSuffix) {
        preCleaner = new PreCleaner(staticPrefix);
        postCleaner = new PostCleaner(staticSuffix);
    }

    @Override
    public String apply(final String s) {
        return preCleaner.andThen(postCleaner).apply(s);
    }
}
