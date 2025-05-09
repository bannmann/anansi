package dev.bannmann.anansi.core;

import static dev.bannmann.labs.core.NullSafe.tryGet;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import org.jspecify.annotations.Nullable;

import dev.bannmann.anansi.api.FrameData;
import dev.bannmann.labs.annotations.SuppressWarningsRationale;

@Value
@Builder(access = AccessLevel.PRIVATE)
public class StorableFingerprint
{
    public static StorableFingerprint from(Incident incident)
    {
        FingerprintData fingerprintData = incident.getFingerprintData();

        @SuppressWarnings("NullAway")
        @SuppressWarningsRationale(
            "NullAway does not yet observe @Nullable on generic types, so it claims tryGet() doesn't accept nullable parameters")
        String location = tryGet(fingerprintData.getLocation(), FrameData::toString);

        return StorableFingerprint.builder()
            .id(incident.getFingerprint())
            .name(fingerprintData.getThrowableClassName())
            .location(location)
            .frames(toMultiLineString(fingerprintData.getRelevantFrames()))
            .extraData(toSortedMultiLineString(fingerprintData.getExtraData()))
            .build();
    }

    private static String toMultiLineString(List<FrameData> frameData)
    {
        return frameData.stream()
            .map(FrameData::toString)
            .collect(Collectors.joining("\n"));
    }

    private static String toSortedMultiLineString(@NonNull Map<String, Object> extraDataMap)
    {
        var sortedMap = new TreeMap<>(extraDataMap);
        return sortedMap.entrySet()
            .stream()
            .map(entry -> entry.getKey() + " = " + entry.getValue())
            .collect(Collectors.joining("\n"));
    }

    @NonNull String id;

    @NonNull String name;

    @Nullable
    String location;

    @NonNull String frames;

    @NonNull String extraData;
}
