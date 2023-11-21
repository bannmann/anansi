package dev.bannmann.anansi.core;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
final class ExampleForWrapping
{
    private final Anansi anansi;

    public void run()
    {
        Anansi.withContext()
            .set("outerMethodData", "4yo8s28")
            .invokeRunnable(this::runInner);
    }

    private void runInner()
    {
        try
        {
            throw new IllegalStateException("simulated unspecified failure");
        }
        catch (RuntimeException e)
        {
            Anansi.attachingTo(e)
                .set("someValue", 42);

            anansi.recordIncident(e, Severity.API_FAILURE)
                .ifPresent(incident -> {
                    throw new CustomException(incident, e);
                });

            throw e;
        }
    }
}
