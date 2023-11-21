package dev.bannmann.anansi.postgresql;

import javax.inject.Inject;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import dev.bannmann.anansi.core.StorableFingerprint;
import dev.bannmann.anansi.postgresql.generated.tables.records.FingerprintRecord;

@RequiredArgsConstructor(onConstructor_ = @Inject, access = AccessLevel.PROTECTED)
class FingerprintRecordConverter
{
    public FingerprintRecord fromPojo(StorableFingerprint pojo)
    {
        FingerprintRecord result = null;

        if (pojo != null)
        {
            result = new FingerprintRecord();
            result.setId(pojo.getId());
            result.setName(pojo.getName());
            result.setLocation(pojo.getLocation());
            result.setFrames(pojo.getFrames());
            result.setExtraData(pojo.getExtraData());
        }

        return result;
    }
}
