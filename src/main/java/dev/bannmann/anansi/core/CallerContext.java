package dev.bannmann.anansi.core;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.concurrent.ThreadSafe;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.errorprone.annotations.CheckReturnValue;

@ThreadSafe
@Getter(AccessLevel.PACKAGE)
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class CallerContext
{
    private final HashMap<String, Object> data = new HashMap<>();

    private final Consumer<CallerContext> cleanupCall;

    private volatile boolean failed;

    @CheckReturnValue
    public CallerContext set(@NonNull String key, @Nullable Object value)
    {
        data.put(key, value);
        return this;
    }

    @CheckReturnValue
    public CallerContext setAll(@NonNull Map<String, Object> map)
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
