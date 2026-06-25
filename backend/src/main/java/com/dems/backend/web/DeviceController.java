package com.dems.backend.web;

import com.dems.backend.domain.Device;
import com.dems.backend.dto.ApplyAlgorithmRequest;
import com.dems.backend.dto.SetStatusRequest;
import com.dems.backend.repository.DeviceRepository;
import com.dems.backend.service.DeviceService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceRepository repository;
    private final DeviceService deviceService;

    public DeviceController(DeviceRepository repository, DeviceService deviceService) {
        this.repository = repository;
        this.deviceService = deviceService;
    }

    @GetMapping
    public List<Device> getDevices() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Device getDevice(@PathVariable String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device " + id + " not found"));
    }

    @PutMapping("/{id}/algorithm")
    public Device applyAlgorithm(@PathVariable String id,
                                 @Valid @RequestBody ApplyAlgorithmRequest request) {
        return deviceService.applyAlgorithm(id, request.algorithm());
    }

    @PutMapping("/{id}/status")
    public Device setStatus(@PathVariable String id,
                            @Valid @RequestBody SetStatusRequest request) {
        return deviceService.setStatus(id, request.status());
    }
}
