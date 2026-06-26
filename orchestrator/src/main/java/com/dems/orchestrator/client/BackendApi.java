package com.dems.orchestrator.client;

import com.dems.orchestrator.client.dto.DeviceDto;
import com.dems.orchestrator.client.dto.DeviceSavingsDto;
import com.dems.orchestrator.client.dto.SavingsSummaryDto;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PutExchange;

/** Declarative HTTP-interface client for the backend's data + action API. */
@HttpExchange("/api")
public interface BackendApi {

    @GetExchange("/devices")
    List<DeviceDto> listDevices();

    @GetExchange("/savings/summary")
    SavingsSummaryDto getSavingsSummary();

    @GetExchange("/savings/devices")
    List<DeviceSavingsDto> getDeviceSavings();

    @PutExchange("/devices/{id}/status")
    DeviceDto setDeviceStatus(@PathVariable String id, @RequestBody Map<String, Object> body);

    @PutExchange("/devices/{id}/algorithm")
    DeviceDto applyAlgorithm(@PathVariable String id, @RequestBody Map<String, Object> body);
}
