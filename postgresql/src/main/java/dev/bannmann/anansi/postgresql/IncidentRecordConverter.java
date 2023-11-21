package dev.bannmann.anansi.postgresql;

import java.util.List;

import javax.inject.Inject;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import com.github.mizool.core.converter.EnumConverter;
import com.github.mizool.core.converter.IdentifierConverter;
import dev.bannmann.anansi.core.Incident;
import dev.bannmann.anansi.core.ThrowableData;
import dev.bannmann.anansi.postgresql.generated.tables.records.IncidentRecord;

@RequiredArgsConstructor(onConstructor_ = @Inject, access = AccessLevel.PROTECTED)
class IncidentRecordConverter
{
    private final IdentifierConverter identifierConverter;
    private final EnumConverter enumConverter;

    public IncidentRecord fromPojo(Incident pojo)
    {
        IncidentRecord result = null;

        if (pojo != null)
        {
            result = new IncidentRecord();
            result.setId(identifierConverter.fromPojo(pojo.getId()));
            result.setTimestamp(pojo.getTimestamp());
            result.setFingerprintId(pojo.getFingerprint());
            result.setSeverity(enumConverter.fromPojo(pojo.getSeverity()));
            result.setThrowableDetails(createThrowableDetailsReport(pojo.getThrowableDetails()));
            result.setBuild(pojo.getApplicationBuildInfo());
        }

        return result;
    }

    /**
     * TODO this isn't postgres specific and should therefore move to core at some point
     */
    private String createThrowableDetailsReport(List<ThrowableData> throwableDetails)
    {
        StringBuilder result = new StringBuilder();

        ThrowableData previous = null;
        for (ThrowableData current : throwableDetails)
        {
            if (result.length() > 0)
            {
                result.append("\n\n");
            }

            result.append(current.getThrowableClassName());
            result.append(" at ");
            result.append(current.getFrameData());
            if (isNewMessage(previous, current))
            {
                result.append(":\n");
                result.append(current.getThrowableMessage());
            }

            previous = current;
        }

        return result.toString();
    }

    /**
     * @return whether current message is non-null and equals neither the previous one nor a trivial "class: message"
     */
    private boolean isNewMessage(ThrowableData previous, ThrowableData current)
    {
        if (previous == null)
        {
            return true;
        }

        String currentMessage = current.getThrowableMessage();
        if (currentMessage == null)
        {
            return false;
        }

        String previousMessage = previous.getThrowableMessage();
        String previousClassAndMessage = previous.getThrowableClassName() + ": " + previousMessage;
        return !(currentMessage.equals(previousMessage) || currentMessage.equals(previousClassAndMessage));
    }
}
