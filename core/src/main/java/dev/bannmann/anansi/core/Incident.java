package dev.bannmann.anansi.core;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import com.github.mizool.core.Identifiable;
import com.github.mizool.core.Identifier;

@Value
@Builder(access = AccessLevel.PACKAGE)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Incident implements Identifiable<Incident>
{
    @NonNull Identifier<Incident> id;
    @NonNull OffsetDateTime timestamp;
    @NonNull FingerprintData fingerprintData;
    @NonNull Severity severity;
    @NonNull Map<String, Object> contextData;
    @NonNull List<ThrowableData> throwableDetails;

    @NonNull String applicationBuildInfo;

    public String getFingerprint()
    {
        return String.format("%08X", fingerprintData.hashCode());
    }
}
