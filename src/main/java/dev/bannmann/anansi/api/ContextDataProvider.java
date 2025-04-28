package dev.bannmann.anansi.api;

import java.util.Map;

public abstract class ContextDataProvider<T extends Throwable>
{
    public abstract Class<T> getThrowableClass();

    public final Map<String, Object> getContextDataForThrowable(Throwable throwable)
    {
        T cast = getThrowableClass().cast(throwable);
        return getContextData(cast);
    }

    protected abstract Map<String, Object> getContextData(T throwable);
}
