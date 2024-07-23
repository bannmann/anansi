package dev.bannmann.anansi.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.jspecify.annotations.Nullable;

import com.github.mizool.core.Identifier;
import com.google.errorprone.annotations.CheckReturnValue;
import dev.bannmann.labs.annotations.SuppressWarningsRationale;
import dev.bannmann.labs.core.function.IoRunnable;
import dev.bannmann.labs.core.function.IoSupplier;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public final class FluentContext
{
    @SuppressWarnings("NullAway")
    @SuppressWarningsRationale(
        "NullAway claims the type parameter cannot be @Nullable due to the upper bound of Map<..., V> not being @Nullable")
    private final Map<String, @Nullable Object> values = new HashMap<>();

    @CheckReturnValue
    public FluentContext set(String key, Identifier<?> id)
    {
        return set(key, id.getValue());
    }

    @CheckReturnValue
    public FluentContext set(String key, @Nullable Object value)
    {
        values.put(key, value);
        return this;
    }

    @CheckReturnValue
    @SuppressWarnings("NullAway")
    @SuppressWarningsRationale(
        "NullAway claims the type parameter cannot be @Nullable due to the upper bound of Map<..., V> not being @Nullable")
    public FluentContext setAll(Map<String, @Nullable Object> collection)
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

    @SuppressWarnings("java:S1181")
    public <T> T invokeIoSupplier(IoSupplier<T> ioSupplier) throws IOException
    {
        CallerContext context = createCallerContext();

        try
        {
            T result = ioSupplier.get();
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
