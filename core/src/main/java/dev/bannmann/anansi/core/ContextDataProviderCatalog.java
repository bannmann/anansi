package dev.bannmann.anansi.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import com.github.mizool.core.MetaInfServices;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

@Slf4j
final class ContextDataProviderCatalog
{
    private final Multimap<Class<?>, ContextDataProvider<?>> data;

    @SuppressWarnings("rawtypes")
    public ContextDataProviderCatalog()
    {
        Iterable<ContextDataProvider> providers = MetaInfServices.instances(ContextDataProvider.class);
        data = buildCatalog(providers);
    }

    @SuppressWarnings("rawtypes")
    private Multimap<Class<?>, ContextDataProvider<?>> buildCatalog(Iterable<ContextDataProvider> providers)
    {
        ListMultimap<Class<?>, ContextDataProvider<?>> result = MultimapBuilder.hashKeys()
            .arrayListValues()
            .build();

        for (ContextDataProvider<?> provider : providers)
        {
            Class<? extends Throwable> throwableClass = provider.getThrowableClass();
            result.put(throwableClass, provider);
        }

        return result;
    }

    public Iterable<ContextDataProvider<?>> lookup(Throwable t)
    {
        List<ContextDataProvider<?>> result = new ArrayList<>();
        addProviders(t.getClass(), result);
        return result;
    }

    private void addProviders(Class<?> aClass, List<ContextDataProvider<?>> result)
    {
        Class<?> superclass = aClass.getSuperclass();
        if (superclass != null && !superclass.equals(Throwable.class))
        {
            addProviders(superclass, result);
        }

        for (Class<?> anInterface : aClass.getInterfaces())
        {
            addProviders(anInterface, result);
        }

        Collection<ContextDataProvider<?>> providers = data.get(aClass);
        result.addAll(providers);
    }
}
