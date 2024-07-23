package dev.bannmann.anansi.core;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import org.jspecify.annotations.Nullable;

@Value
@Builder(access = AccessLevel.PRIVATE)
public class ThrowableData
{
    static ThrowableData from(Throwable throwable)
    {
        var frameData = getFrameDataOrNull(throwable);
        String throwableClassName = throwable.getClass()
            .getName();
        String throwableMessage = throwable.getMessage();

        return ThrowableData.builder()
            .frameData(frameData)
            .throwableClassName(throwableClassName)
            .throwableMessage(throwableMessage)
            .build();
    }

    private static @Nullable FrameData getFrameDataOrNull(Throwable throwable)
    {
        var stackTrace = throwable.getStackTrace();
        if (stackTrace.length == 0)
        {
            return null;
        }

        return FrameData.from(stackTrace[0]);
    }

    @Nullable FrameData frameData;
    @NonNull String throwableClassName;

    @Nullable String throwableMessage;

    @Override
    public String toString()
    {
        return "%s at %s: %s".formatted(throwableClassName,
            frameData != null
                ? frameData
                : "[?]",
            throwableMessage);
    }
}
