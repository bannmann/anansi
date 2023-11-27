package dev.bannmann.anansi.core;

public class NoOpIncidentStore implements IncidentStore
{
    @Override
    public void store(Incident incident, StorableFingerprint fingerprint)
    {
        // No-op
    }
}
