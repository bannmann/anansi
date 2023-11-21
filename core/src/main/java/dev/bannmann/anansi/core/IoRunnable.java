package dev.bannmann.anansi.core;

import java.io.IOException;

@FunctionalInterface
public interface IoRunnable
{
    void run() throws IOException;
}
