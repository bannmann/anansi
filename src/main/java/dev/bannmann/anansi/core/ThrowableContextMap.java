package dev.bannmann.anansi.core;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.annotation.concurrent.NotThreadSafe;

import org.jspecify.annotations.Nullable;

import com.github.mizool.core.exception.CodeInconsistencyException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.bannmann.labs.annotations.SuppressWarningsRationale;

/**
 * Holds identity references to exception instances and associated context data. <br>
 * <br>
 * This class is not thread safe as it is intended to be used inside one thread, with the caller storing each instance
 * in a {@link ThreadLocal}.
 */
@NotThreadSafe
final class ThrowableContextMap
{
    @SuppressWarnings("NullAway")
    @SuppressWarningsRationale(
        "NullAway claims the type parameter cannot be @Nullable due to the upper bound of Map<..., V> not being @Nullable")
    private final Cache<Throwable, Map<String, @Nullable Object>> mapsByThrowable = CacheBuilder.newBuilder()
        .weakKeys()
        .expireAfterWrite(Duration.ofMinutes(5))
        .build();

    public void put(Throwable throwable, String key, @Nullable Object value)
    {
        try
        {
            @SuppressWarnings("NullAway")
            @SuppressWarningsRationale(
                "NullAway claims the type parameter cannot be @Nullable due to the upper bound of Map<..., V> not being @Nullable")
            Map<String, @Nullable Object> dataMap = mapsByThrowable.get(throwable, HashMap::new);

            dataMap.put(key, value);
        }
        catch (ExecutionException e)
        {
            throw new CodeInconsistencyException(
                "At the time of writing, the cache loader could not throw a checked exception",
                e);
        }
    }

    @SuppressWarnings("NullAway")
    @SuppressWarningsRationale(
        "NullAway claims the type parameter cannot be @Nullable due to the upper bound of Map<..., V> not being @Nullable")
    public Optional<Map<String, @Nullable Object>> getAll(Throwable throwable)
    {
        Map<String, @Nullable Object> result = mapsByThrowable.getIfPresent(throwable);
        return Optional.ofNullable(result);
    }
}
