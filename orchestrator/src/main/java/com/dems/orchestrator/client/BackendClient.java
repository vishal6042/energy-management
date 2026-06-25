package com.dems.orchestrator.client;

import com.dems.orchestrator.client.dto.DeviceDto;
import com.dems.orchestrator.client.dto.DeviceSavingsDto;
import com.dems.orchestrator.client.dto.SavingsSummaryDto;
import com.dems.orchestrator.config.OrchestratorProperties;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Calls the existing data/action API (system of record) over REST. */
@Component
public class BackendClient {

    private final RestClient http;

    public BackendClient(OrchestratorProperties props) {
        this.http = HttpClients.create(props.backend().baseUrl(), 20);
    }

    public List<DeviceDto> listDevices() {
        return http.get().uri("/api/devices").retrieve()
                .body(new ParameterizedTypeReference<List<DeviceDto>>() {});
    }

    public SavingsSummaryDto getSavingsSummary() {
        return http.get().uri("/api/savings/summary").retrieve().body(SavingsSummaryDto.class);
    }

    public List<DeviceSavingsDto> getDeviceSavings() {
        return http.get().uri("/api/savings/devices").retrieve()
                .body(new ParameterizedTypeReference<List<DeviceSavingsDto>>() {});
    }

    public DeviceDto setDeviceStatus(String id, String status) {
        return http.put().uri("/api/devices/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("status", status))
                .retrieve().body(DeviceDto.class);
    }

    public DeviceDto applyAlgorithm(String id, String algorithm) {
        return http.put().uri("/api/devices/{id}/algorithm", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("algorithm", algorithm))
                .retrieve().body(DeviceDto.class);
    }
}
