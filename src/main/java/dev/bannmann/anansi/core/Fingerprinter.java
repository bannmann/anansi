package dev.bannmann.anansi.core;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apiguardian.api.API;

import dev.bannmann.anansi.api.FrameData;

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
        return extract(throwable, this::extractData);
    }

    private <E> E extract(Throwable throwable, Function<T, E> function)
    {
        T cast = getThrowableClass().cast(throwable);
        return function.apply(cast);
    }

    protected abstract Map<String, Object> extractData(T throwable);

    @API(status = EXPERIMENTAL)
    public final List<FrameData> extractFramesFromThrowable(Throwable throwable)
    {
        return extract(throwable, this::extractFrames);
    }

    protected List<FrameData> extractFrames(T throwable)
    {
        return Collections.emptyList();
    }
}
