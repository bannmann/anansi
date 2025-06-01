package dev.bannmann.anansi.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import com.github.mizool.core.MetaInfServices;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import dev.bannmann.anansi.api.Fingerprinter;

@Slf4j
final class FingerprinterCatalog
{
    private final Multimap<Class<?>, Fingerprinter<?>> data;

    @SuppressWarnings("rawtypes")
    public FingerprinterCatalog()
    {
        Iterable<Fingerprinter> fingerprinters = MetaInfServices.instances(Fingerprinter.class);
        data = buildCatalog(fingerprinters);
    }

    @SuppressWarnings("rawtypes")
    private Multimap<Class<?>, Fingerprinter<?>> buildCatalog(Iterable<Fingerprinter> fingerprinters)
    {
        ListMultimap<Class<?>, Fingerprinter<?>> result = MultimapBuilder.hashKeys()
            .arrayListValues()
            .build();

        for (Fingerprinter<?> fingerprinter : fingerprinters)
        {
            Class<? extends Throwable> throwableClass = fingerprinter.getThrowableClass();
            result.put(throwableClass, fingerprinter);
        }

        return result;
    }

    public Iterable<Fingerprinter<?>> lookup(Throwable t)
    {
        List<Fingerprinter<?>> result = new ArrayList<>();
        addFingerprinters(t.getClass(), result);
        return result;
    }

    private void addFingerprinters(Class<?> aClass, List<Fingerprinter<?>> result)
    {
        Class<?> superclass = aClass.getSuperclass();
        if (superclass != null && !superclass.equals(Throwable.class))
        {
            addFingerprinters(superclass, result);
        }

        for (Class<?> anInterface : aClass.getInterfaces())
        {
            addFingerprinters(anInterface, result);
        }

        Collection<Fingerprinter<?>> fingerprinters = data.get(aClass);
        result.addAll(fingerprinters);
    }
}
