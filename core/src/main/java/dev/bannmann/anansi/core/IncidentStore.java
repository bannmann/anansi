package dev.bannmann.anansi.core;

public interface IncidentStore
{
    void store(Incident incident, StorableFingerprint fingerprint);
}
