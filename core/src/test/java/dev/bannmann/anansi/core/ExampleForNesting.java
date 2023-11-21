package dev.bannmann.anansi.core;

import lombok.extern.slf4j.XSlf4j;

import dev.bannmann.anansi.core.Anansi;

@XSlf4j
final class ExampleForNesting
{
    public void run()
    {
        Anansi.withContext()
            .set("callingA", true)
            .invokeRunnable(this::methodA);
    }

    private void methodA()
    {
        log.entry();

        Anansi.withContext()
            .set("callingB", true)
            .invokeRunnable(this::methodB);

        log.exit();
    }

    private void methodB()
    {
        log.entry();

        for (int i = 0; i < 2; i++)
        {
            boolean shouldFailAfterD = i == 1;

            Anansi.withContext()
                .set("callingC", true)
                .invokeRunnable(() -> methodC(shouldFailAfterD));
        }

        log.exit();
    }

    private void methodC(boolean shouldFailAfterD)
    {
        log.entry();

        Anansi.withContext()
            .set("callingD", true)
            .invokeRunnable(this::methodD);

        if (shouldFailAfterD)
        {
            throw new IllegalStateException("simulated failure");
        }

        log.exit();
    }

    private void methodD()
    {
        log.entry();

        // Nothing to do

        log.exit();
    }
}
