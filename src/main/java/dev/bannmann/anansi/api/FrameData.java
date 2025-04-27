package dev.bannmann.anansi.api;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import org.jspecify.annotations.Nullable;

@Value
@Builder(access = AccessLevel.PRIVATE)
public class FrameData
{
    public static FrameData from(StackTraceElement element)
    {
        Integer lineNumber = element.getLineNumber();
        if (lineNumber < 0)
        {
            lineNumber = null;
        }

        return FrameData.builder()
            .className(element.getClassName())
            .methodName(element.getMethodName())
            .fileName(element.getFileName())
            .line(lineNumber)
            .build();
    }

    public static FrameData from(StackWalker.StackFrame element)
    {
        Integer lineNumber = element.getLineNumber();
        if (lineNumber < 0)
        {
            lineNumber = null;
        }

        return FrameData.builder()
            .className(element.getClassName())
            .methodName(element.getMethodName())
            .fileName(element.getFileName())
            .line(lineNumber)
            .build();
    }

    @NonNull
    String className;

    @NonNull
    String methodName;

    @Nullable
    String fileName;

    @Nullable
    Integer line;

    /**
     * @return class name and method name concatenated using a period
     */
    public String getLocation()
    {
        return className + "." + methodName;
    }

    @Override
    public String toString()
    {
        return getLocation() + "(" + getSource() + ")";
    }

    private String getSource()
    {
        if (fileName == null || line == null)
        {
            return "Unknown Source";
        }

        return fileName + ":" + line;
    }
}
