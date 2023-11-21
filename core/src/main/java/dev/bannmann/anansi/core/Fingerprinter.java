package dev.bannmann.anansi.core;

import java.util.Map;

/**
 * Enriches the set of data used for fingerprinting an exception. This is useful for things like REST requests performed
 * by the application, where the same exception can occur in the same application frames, but you want to distinguish
 * different status codes.
 */
public abstract class Fingerprinter<T extends Throwable>
{
    public abstract Class<T> getThrowableClass();

    public final Map<String, Object> extractDataFromThrowable(Throwable throwable)
    {
        T cast = getThrowableClass().cast(throwable);
        return extractData(cast);
    }

    protected abstract Map<String, Object> extractData(T throwable);
}
