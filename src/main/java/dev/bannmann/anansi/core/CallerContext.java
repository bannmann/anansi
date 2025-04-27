package dev.bannmann.anansi.core;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.Nullable;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.ThreadSafe;
import dev.bannmann.labs.annotations.SuppressWarningsRationale;

@ThreadSafe
@Getter(AccessLevel.PACKAGE)
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class CallerContext
{
    @SuppressWarnings("NullAway")
    @SuppressWarningsRationale(
        "NullAway claims the type parameter cannot be @Nullable due to the upper bound of Map<..., V> not being @Nullable")
    private final HashMap<String, @Nullable Object> data = new HashMap<>();

    private final Consumer<CallerContext> cleanupCall;

    private volatile boolean failed;

    @CheckReturnValue
    public CallerContext set(@NonNull String key, @Nullable Object value)
    {
        data.put(key, value);
        return this;
    }

    @CheckReturnValue
    @SuppressWarnings("NullAway")
    @SuppressWarningsRationale(
        "NullAway claims the type parameter cannot be @Nullable due to the upper bound of Map<..., V> not being @Nullable")
    public CallerContext setAll(@NonNull Map<String, @Nullable Object> map)
    {
        data.putAll(map);
        return this;
    }

    public void succeed()
    {
        cleanupCall.accept(this);
    }

    public void fail()
    {
        failed = true;
    }
}
