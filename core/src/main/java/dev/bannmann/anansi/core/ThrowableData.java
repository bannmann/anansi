package dev.bannmann.anansi.core;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import com.github.mizool.core.validation.Nullable;

@Value
@Builder(access = AccessLevel.PRIVATE)
public class ThrowableData
{
    static ThrowableData from(Throwable throwable)
    {
        FrameData frameData = FrameData.from(throwable.getStackTrace()[0]);
        String throwableClassName = throwable.getClass()
            .getName();
        String throwableMessage = throwable.getMessage();

        return ThrowableData.builder()
            .frameData(frameData)
            .throwableClassName(throwableClassName)
            .throwableMessage(throwableMessage)
            .build();
    }

    @NonNull FrameData frameData;
    @NonNull String throwableClassName;

    @Nullable
    String throwableMessage;

    @Override
    public String toString()
    {
        return throwableClassName + " at " + frameData + ": " + throwableMessage;
    }
}
