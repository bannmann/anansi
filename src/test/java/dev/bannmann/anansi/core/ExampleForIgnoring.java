package dev.bannmann.anansi.core;

import lombok.RequiredArgsConstructor;

import com.github.mizool.core.exception.InvalidBackendReplyException;

@RequiredArgsConstructor
final class ExampleForIgnoring
{
    private final Anansi anansi;

    public void run()
    {
        Anansi.withContext()
            .set("outerMethodData", "3wjvf8h3w")
            .invokeRunnable(this::runInner);
    }

    private void runInner()
    {
        try
        {
            throw new InvalidBackendReplyException("simulated harmless problem");
        }
        catch (RuntimeException e)
        {
            if (e.getMessage() != null &&
                e.getMessage()
                    .contains("harmless"))
            {
                anansi.recordIncident(e, Severity.IGNORE);
            }
            else
            {
                throw e;
            }
        }
    }
}
