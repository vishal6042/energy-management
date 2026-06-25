package com.dems.backend.service;

import com.dems.backend.domain.Granularity;
import com.dems.backend.domain.SavingRecord;
import com.dems.backend.dto.DeviceSavingsDto;
import com.dems.backend.dto.SavingsRecordDto;
import com.dems.backend.dto.SavingsSummaryDto;
import com.dems.backend.repository.SavingRecordRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

@Service
public class SavingsService {

    private final SavingRecordRepository repository;

    public SavingsService(SavingRecordRepository repository) {
        this.repository = repository;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    /** Per-device savings series, grouped by device and split by granularity. */
    public List<DeviceSavingsDto> getDeviceSavings() {
        Map<String, List<SavingsRecordDto>> daily = new LinkedHashMap<>();
        Map<String, List<SavingsRecordDto>> monthly = new LinkedHashMap<>();

        for (SavingRecord r : repository.findByGranularityOrderByPeriodAsc(Granularity.DAILY)) {
            daily.computeIfAbsent(r.getDeviceId(), k -> new ArrayList<>())
                    .add(toDto(r));
        }
        for (SavingRecord r : repository.findByGranularityOrderByPeriodAsc(Granularity.MONTHLY)) {
            monthly.computeIfAbsent(r.getDeviceId(), k -> new ArrayList<>())
                    .add(toDto(r));
        }

        List<DeviceSavingsDto> result = new ArrayList<>();
        for (String deviceId : daily.keySet()) {
            result.add(new DeviceSavingsDto(
                    deviceId,
                    daily.getOrDefault(deviceId, List.of()),
                    monthly.getOrDefault(deviceId, List.of())));
        }
        return result;
    }

    /** Totals plus per-period aggregates summed across all devices. */
    public SavingsSummaryDto getSummary() {
        List<SavingsRecordDto> daily = aggregateByPeriod(
                repository.findByGranularityOrderByPeriodAsc(Granularity.DAILY));
        List<SavingsRecordDto> monthly = aggregateByPeriod(
                repository.findByGranularityOrderByPeriodAsc(Granularity.MONTHLY));

        double totalCost = monthly.stream().mapToDouble(SavingsRecordDto::costSaved).sum();
        double totalEnergy = monthly.stream().mapToDouble(SavingsRecordDto::energySavedKwh).sum();
        double totalUsage = monthly.stream().mapToDouble(SavingsRecordDto::usageKwh).sum();
        double totalUsageCost = monthly.stream().mapToDouble(SavingsRecordDto::usageCost).sum();

        return new SavingsSummaryDto(round2(totalCost), round1(totalEnergy),
                round1(totalUsage), round2(totalUsageCost), daily, monthly);
    }

    private List<SavingsRecordDto> aggregateByPeriod(List<SavingRecord> records) {
        Map<String, double[]> byPeriod = new TreeMap<>();
        for (SavingRecord r : records) {
            double[] acc = byPeriod.computeIfAbsent(r.getPeriod(), k -> new double[4]);
            acc[0] += r.getCostSaved();
            acc[1] += r.getEnergySavedKwh();
            acc[2] += r.getUsageKwh();
            acc[3] += r.getUsageCost();
        }
        List<SavingsRecordDto> result = new ArrayList<>();
        byPeriod.forEach((period, acc) -> result.add(new SavingsRecordDto(
                period, round2(acc[0]), round1(acc[1]), round1(acc[2]), round2(acc[3]))));
        return result;
    }

    private SavingsRecordDto toDto(SavingRecord r) {
        return new SavingsRecordDto(r.getPeriod(), r.getCostSaved(), r.getEnergySavedKwh(),
                r.getUsageKwh(), r.getUsageCost());
    }
}
