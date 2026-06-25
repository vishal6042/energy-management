package com.dems.backend.bootstrap;

import com.dems.backend.domain.AlgorithmType;
import com.dems.backend.domain.Device;
import com.dems.backend.domain.DeviceStatus;
import com.dems.backend.domain.Granularity;
import com.dems.backend.domain.SavingRecord;
import com.dems.backend.repository.DeviceRepository;
import com.dems.backend.repository.SavingRecordRepository;
import com.dems.backend.service.SavingsGenerator;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the in-memory H2 database on startup. Mirrors the data the React
 * frontend previously generated as mocks: 4 AC devices plus 14 days and 6
 * months of per-device usage and savings.
 *
 * <p>ac-004 is modelled as offline only recently: it has real usage/savings for
 * older periods and zero for the most recent stretch (the last
 * {@link #OFFLINE_DAILY_TAIL} days and the current month).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final String RECENTLY_OFFLINE_ID = "ac-004";
    private static final int OFFLINE_DAILY_TAIL = 3;

    private final DeviceRepository deviceRepository;
    private final SavingRecordRepository savingRepository;
    private final SavingsGenerator generator;

    public DataSeeder(DeviceRepository deviceRepository, SavingRecordRepository savingRepository,
                      SavingsGenerator generator) {
        this.deviceRepository = deviceRepository;
        this.savingRepository = savingRepository;
        this.generator = generator;
    }

    @Override
    public void run(String... args) {
        if (deviceRepository.count() > 0) {
            return;
        }

        List<Device> devices = List.of(
                new Device("ac-001", "Conference Room AC", "Floor 1 — East Wing",
                        "CoolMax 2.5T", DeviceStatus.ONLINE, 23.4, 24, 1850, 1850, AlgorithmType.COMFORT),
                new Device("ac-002", "Server Room AC", "Floor 1 — Core",
                        "CoolMax 3T", DeviceStatus.ONLINE, 19.8, 20, 2400, 2400, AlgorithmType.TARGET),
                new Device("ac-003", "Lobby AC", "Ground Floor",
                        "AeroCool 2T", DeviceStatus.ONLINE, 25.1, 25, 1600, 1600, AlgorithmType.NONE),
                // Offline now (powerW 0) but rated at 2100W; was online for older periods.
                new Device("ac-004", "Open Office AC", "Floor 2 — West Wing",
                        "CoolMax 4T", DeviceStatus.OFFLINE, 27.0, 24, 0, 2100, AlgorithmType.NONE));
        deviceRepository.saveAll(devices);

        List<SavingRecord> records = new ArrayList<>();
        for (Device d : devices) {
            // 14 daily points (oldest first).
            for (int i = 13; i >= 0; i--) {
                String period = LocalDate.now().minusDays(i).toString();
                boolean online = onlineForDaily(d, i);
                records.add(generator.build(d, Granularity.DAILY, period, online));
            }
            // 6 monthly points (oldest first).
            for (int i = 5; i >= 0; i--) {
                String period = YearMonth.now().minusMonths(i).format(MONTH);
                boolean online = onlineForMonthly(d, i);
                records.add(generator.build(d, Granularity.MONTHLY, period, online));
            }
        }
        savingRepository.saveAll(records);
    }

    /** {@code i} is days-ago (0 = today). The recently-offline device is dark for the last few days. */
    private boolean onlineForDaily(Device d, int i) {
        if (!RECENTLY_OFFLINE_ID.equals(d.getId())) {
            return d.getStatus() == DeviceStatus.ONLINE;
        }
        return i >= OFFLINE_DAILY_TAIL;
    }

    /** {@code i} is months-ago (0 = current month). The recently-offline device is dark this month. */
    private boolean onlineForMonthly(Device d, int i) {
        if (!RECENTLY_OFFLINE_ID.equals(d.getId())) {
            return d.getStatus() == DeviceStatus.ONLINE;
        }
        return i >= 1;
    }
}
