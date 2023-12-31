package dev.bannmann.anansi.core;

import java.util.function.BiConsumer;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class FluentAttacher
{
    private final BiConsumer<String, Object> putCall;

    public FluentAttacher set(String key, Object value)
    {
        putCall.accept(key, value);
        return this;
    }
}
