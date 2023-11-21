package dev.bannmann.anansi.core;

import dev.bannmann.anansi.core.Incident;

class CustomException extends RuntimeException
{
    public CustomException(Incident incident, Throwable cause)
    {
        super(incident.getId()
            .getValue(), cause);
    }
}
