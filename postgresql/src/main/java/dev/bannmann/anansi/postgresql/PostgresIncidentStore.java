package dev.bannmann.anansi.postgresql;

import static dev.bannmann.anansi.postgresql.generated.Tables.FINGERPRINT;
import static dev.bannmann.anansi.postgresql.generated.Tables.INCIDENT;
import static dev.bannmann.anansi.postgresql.generated.Tables.INCIDENT_DATA;
import static org.apiguardian.api.API.Status.STABLE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.apiguardian.api.API;
import org.jooq.Row3;
import org.jooq.impl.DSL;

import com.github.bannmann.labs.records_api.Records;
import dev.bannmann.anansi.core.Incident;
import dev.bannmann.anansi.core.IncidentStore;
import dev.bannmann.anansi.core.StorableFingerprint;

@API(status = STABLE)
@RequiredArgsConstructor(onConstructor_ = @Inject, access = AccessLevel.PROTECTED)
public class PostgresIncidentStore implements IncidentStore
{
    private final Records records;
    private final IncidentRecordConverter incidentRecordConverter;
    private final FingerprintRecordConverter fingerprintRecordConverter;

    public void store(Incident pojo, StorableFingerprint fingerprint)
    {
        records.insertInto(FINGERPRINT)
            .withAnonymousConvertedVia(fingerprintRecordConverter::fromPojo)
            .fromPojo(fingerprint)
            .onDuplicateKeyIgnore()
            .voidExecute();

        records.insertInto(INCIDENT)
            .withIdentifiableConvertedVia(incidentRecordConverter::fromPojo)
            .fromPojoWithPresetId(pojo)
            .voidExecute();

        records.execute(dslContext -> dslContext.insertInto(INCIDENT_DATA,
                INCIDENT_DATA.INCIDENT_ID,
                INCIDENT_DATA.KEY,
                INCIDENT_DATA.VALUE)
            .valuesOfRows(getDataRows(pojo)));
    }

    private List<Row3<String, String, String>> getDataRows(Incident incident)
    {
        // Prepare pairs to write in a map to get rid of duplicate keys (to avoid failing inserts)
        var map = new HashMap<String, Object>();
        map.putAll(incident.getContextData());
        map.putAll(incident.getFingerprintData()
            .getExtraData());

        return map.entrySet()
            .stream()
            .map(stringObjectEntry -> getDataRow(incident, stringObjectEntry))
            .collect(Collectors.toList());
    }

    private Row3<String, String, String> getDataRow(Incident incident, Map.Entry<String, Object> entry)
    {
        String id = incident.getId()
            .getValue();

        String key = entry.getKey();

        String value = entry.getValue() == null
            ? ""
            : entry.getValue()
                .toString();

        return DSL.row(id, key, value);
    }
}
