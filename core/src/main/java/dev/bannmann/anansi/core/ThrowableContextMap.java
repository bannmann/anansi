package dev.bannmann.anansi.core;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.annotation.concurrent.NotThreadSafe;

import com.github.mizool.core.exception.CodeInconsistencyException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Holds identity references to exception instances and associated context data. <br>
 * <br>
 * This class is not thread safe as it is intended to be used inside one thread, with the caller storing each instance
 * in a {@link ThreadLocal}.
 */
@NotThreadSafe
final class ThrowableContextMap
{
    private final Cache<Throwable, Map<String, Object>> mapsByThrowable = CacheBuilder.newBuilder()
        .weakKeys()
        .expireAfterWrite(Duration.ofMinutes(5))
        .build();

    public void put(Throwable throwable, String key, Object value)
    {
        try
        {
            Map<String, Object> dataMap = mapsByThrowable.get(throwable, HashMap::new);
            dataMap.put(key, value);
        }
        catch (ExecutionException e)
        {
            throw new CodeInconsistencyException(
                "At the time of writing, the cache loader could not throw a checked exception",
                e);
        }
    }

    public Optional<Map<String, Object>> getAll(Throwable throwable)
    {
        Map<String, Object> result = mapsByThrowable.getIfPresent(throwable);
        return Optional.ofNullable(result);
    }
}
