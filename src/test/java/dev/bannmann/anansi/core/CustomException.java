package dev.bannmann.anansi.core;

class CustomException extends RuntimeException
{
    public CustomException(Incident incident, Throwable cause)
    {
        super(incident.getId()
            .getValue(), cause);
    }
}
