package com.dems.backend.repository;

import com.dems.backend.domain.Granularity;
import com.dems.backend.domain.SavingRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavingRecordRepository extends JpaRepository<SavingRecord, Long> {

    List<SavingRecord> findByGranularityOrderByPeriodAsc(Granularity granularity);

    Optional<SavingRecord> findFirstByDeviceIdAndGranularityOrderByPeriodDesc(
            String deviceId, Granularity granularity);
}
