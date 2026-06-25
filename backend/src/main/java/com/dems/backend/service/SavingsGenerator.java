package com.dems.backend.service;

import com.dems.backend.domain.AlgorithmType;
import com.dems.backend.domain.Device;
import com.dems.backend.domain.Granularity;
import com.dems.backend.domain.SavingRecord;
import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * Generates per-period usage and savings figures for a device. Shared by the
 * initial data seeder and the turn-on/off logic so both produce consistent
 * numbers.
 *
 * <p>Usage exists for any online device (even with {@code algorithm = none});
 * energy/cost savings only accrue when an algorithm is applied. An offline
 * device contributes nothing.
 */
@Component
public class SavingsGenerator {

    /** Energy tariff in $/kWh, used to price consumption. */
    private static final double TARIFF = 0.18;

    private static final double DAILY_HOURS = 8.0;
    private static final int DAYS_PER_MONTH = 30;

    private final Random random = new Random(42);

    /** Build a record for the given device/period. Offline → all-zero. */
    public SavingRecord build(Device device, Granularity granularity, String period, boolean online) {
        if (!online) {
            return new SavingRecord(device.getId(), granularity, period, 0, 0, 0, 0);
        }
        double usageKwh = usageKwh(device, granularity);
        double usageCost = round2(usageKwh * TARIFF);
        double savingsScale = savingsScale(device);
        double energySaved = round1(savingsBase(granularity, true) * savingsScale);
        double costSaved = round2(savingsBase(granularity, false) * savingsScale);
        return new SavingRecord(device.getId(), granularity, period,
                costSaved, energySaved, round1(usageKwh), usageCost);
    }

    /** Recompute an existing record in place (used when a device resumes). */
    public void fill(SavingRecord record, Device device, boolean online) {
        if (!online) {
            record.setUsageKwh(0);
            record.setUsageCost(0);
            record.setEnergySavedKwh(0);
            record.setCostSaved(0);
            return;
        }
        double usageKwh = round1(usageKwh(device, record.getGranularity()));
        double savingsScale = savingsScale(device);
        record.setUsageKwh(usageKwh);
        record.setUsageCost(round2(usageKwh * TARIFF));
        record.setEnergySavedKwh(round1(savingsBase(record.getGranularity(), true) * savingsScale));
        record.setCostSaved(round2(savingsBase(record.getGranularity(), false) * savingsScale));
    }

    private double usageKwh(Device device, Granularity granularity) {
        double kw = device.getNominalPowerW() / 1000.0;
        double daily = kw * DAILY_HOURS * (0.85 + random.nextDouble() * 0.3);
        return granularity == Granularity.DAILY ? daily : daily * DAYS_PER_MONTH;
    }

    private double savingsBase(Granularity granularity, boolean energy) {
        if (granularity == Granularity.DAILY) {
            return energy ? 5 + random.nextDouble() * 4 : 3 + random.nextDouble() * 2.5;
        }
        return energy ? 130 + random.nextDouble() * 80 : 90 + random.nextDouble() * 50;
    }

    private double savingsScale(Device device) {
        if (device.getAlgorithm() == AlgorithmType.NONE) {
            return 0;
        }
        if (device.getNominalPowerW() >= 2400) {
            return 1.4;
        }
        if (device.getNominalPowerW() >= 1800) {
            return 1.1;
        }
        return 0.85;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
