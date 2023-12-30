package dev.bannmann.anansi.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import com.github.mizool.core.Identifier;
import com.google.errorprone.annotations.CheckReturnValue;
import dev.bannmann.labs.core.function.IoRunnable;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public final class FluentContext
{
    private final Map<String, Object> values = new HashMap<>();

    @CheckReturnValue
    public FluentContext set(String key, Identifier<?> id)
    {
        return set(key, id.getValue());
    }

    @CheckReturnValue
    public FluentContext set(String key, Object value)
    {
        values.put(key, value);
        return this;
    }

    @CheckReturnValue
    public FluentContext setAll(Map<String, Object> collection)
    {
        values.putAll(collection);
        return this;
    }

    @SuppressWarnings("java:S1181")
    public void invokeRunnable(Runnable runnable)
    {
        CallerContext context = createCallerContext();

        try
        {
            runnable.run();
            context.succeed();
        }
        catch (Throwable e)
        {
            context.fail();
            throw e;
        }
    }

    private CallerContext createCallerContext()
    {
        return Anansi.createAndRegisterContext()
            .setAll(values);
    }

    @SuppressWarnings("java:S1181")
    public void invokeIoRunnable(IoRunnable ioRunnable) throws IOException
    {
        CallerContext context = createCallerContext();

        try
        {
            ioRunnable.run();
            context.succeed();
        }
        catch (Throwable e)
        {
            context.fail();
            throw e;
        }
    }

    @SuppressWarnings("java:S1181")
    public <T> T invokeSupplier(Supplier<T> supplier)
    {
        CallerContext context = createCallerContext();

        try
        {
            T result = supplier.get();
            context.succeed();
            return result;
        }
        catch (Throwable e)
        {
            context.fail();
            throw e;
        }
    }
}
