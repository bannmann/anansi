package dev.bannmann.anansi.core;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apiguardian.api.API;

public interface Fingerprintable
{
    Map<String, Object> getFingerprintData();

    @API(status = EXPERIMENTAL)
    default List<FrameData> getAdditionalFrames()
    {
        return Collections.emptyList();
    }
}
