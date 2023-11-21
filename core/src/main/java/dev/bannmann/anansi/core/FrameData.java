package dev.bannmann.anansi.core;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import com.github.mizool.core.validation.Nullable;

@Value
@Builder(access = AccessLevel.PRIVATE)
public class FrameData
{
    static FrameData from(StackTraceElement element)
    {
        String methodName = element.getClassName() + "." + element.getMethodName();

        Integer lineNumber = element.getLineNumber();
        if (lineNumber < 0)
        {
            lineNumber = null;
        }

        return FrameData.builder()
            .methodName(methodName)
            .fileName(element.getFileName())
            .line(lineNumber)
            .build();
    }

    @NonNull String methodName;

    @Nullable
    String fileName;

    @Nullable
    Integer line;

    @Override
    public String toString()
    {
        String location = "Unknown Source";
        if (fileName != null && line != null)
        {
            location = fileName + ":" + line;
        }
        return methodName + "(" + location + ")";
    }
}
