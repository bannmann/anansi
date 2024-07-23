package dev.bannmann.anansi.core;

import java.util.function.BiConsumer;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.Nullable;

import dev.bannmann.labs.annotations.SuppressWarningsRationale;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class FluentAttacher
{
    @SuppressWarnings("NullAway")
    @SuppressWarningsRationale(
        "NullAway claims the type parameter cannot be @Nullable due to the upper bound of BiConsumer<..., U> not being @Nullable")
    private final BiConsumer<String, @Nullable Object> putCall;

    public FluentAttacher set(String key, @Nullable Object value)
    {
        putCall.accept(key, value);
        return this;
    }
}
