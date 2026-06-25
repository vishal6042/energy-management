package com.dems.backend.service;

import com.dems.backend.domain.AlgorithmType;
import com.dems.backend.domain.Device;
import com.dems.backend.domain.DeviceStatus;
import com.dems.backend.domain.Granularity;
import com.dems.backend.domain.SavingRecord;
import com.dems.backend.repository.DeviceRepository;
import com.dems.backend.repository.SavingRecordRepository;
import com.dems.backend.web.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final SavingRecordRepository savingRepository;
    private final SavingsGenerator generator;

    public DeviceService(DeviceRepository deviceRepository,
                         SavingRecordRepository savingRepository,
                         SavingsGenerator generator) {
        this.deviceRepository = deviceRepository;
        this.savingRepository = savingRepository;
        this.generator = generator;
    }

    private Device require(String id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device " + id + " not found"));
    }

    public Device applyAlgorithm(String id, AlgorithmType algorithm) {
        Device device = require(id);
        device.setAlgorithm(algorithm);
        return deviceRepository.save(device);
    }

    /**
     * Turns a device on or off. Turning on restores the rated power draw and
     * resumes contribution from the latest period (earlier offline periods stay
     * zero); turning off drops current draw to zero and zeroes the latest period.
     */
    @Transactional
    public Device setStatus(String id, DeviceStatus status) {
        Device device = require(id);
        boolean online = status == DeviceStatus.ONLINE;
        device.setStatus(status);
        device.setPowerW(online ? device.getNominalPowerW() : 0);
        Device saved = deviceRepository.save(device);

        refreshLatest(saved, Granularity.DAILY, online);
        refreshLatest(saved, Granularity.MONTHLY, online);
        return saved;
    }

    private void refreshLatest(Device device, Granularity granularity, boolean online) {
        SavingRecord latest = savingRepository
                .findFirstByDeviceIdAndGranularityOrderByPeriodDesc(device.getId(), granularity)
                .orElse(null);
        if (latest != null) {
            generator.fill(latest, device, online);
            savingRepository.save(latest);
        }
    }
}
