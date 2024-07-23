package dev.bannmann.anansi.core;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import org.jspecify.annotations.Nullable;

@Value
@Builder
public class FingerprintData
{
    @NonNull String throwableClassName;

    /**
     * The topmost in-app frame that encountered (or threw) the throwable. If it was the application itself that threw
     * the throwable (as opposed to library code), this is identical to the first element of
     * {@link #relevantFrames}.<br>
     * <br>
     * This may be {@code null} when dealing with asynchronous code such as Futures.
     */
    @Nullable
    FrameData location;

    /**
     * All relevant frames. A frame is relevant if it either passes the filter or is the topmost frame (which usually is
     * the one where the exception was thrown).
     */
    @NonNull List<FrameData> relevantFrames;

    /**
     * Data supplied by {@link Fingerprinter}s.
     */
    @NonNull Map<String, Object> extraData;
}
