package dev.bannmann.anansi.core;

import dev.bannmann.anansi.core.Incident;
import dev.bannmann.anansi.core.IncidentStore;
import dev.bannmann.anansi.core.StorableFingerprint;

public class NoOpIncidentStore implements IncidentStore
{
    @Override
    public void store(Incident incident, StorableFingerprint fingerprint)
    {
        // No-op
    }
}
