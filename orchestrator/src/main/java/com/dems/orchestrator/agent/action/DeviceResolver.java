package com.dems.orchestrator.agent.action;

import com.dems.orchestrator.client.BackendApi;
import com.dems.orchestrator.client.dto.DeviceDto;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Maps a device id or friendly name (e.g. "Open Office AC") to a backend device. */
@Component
public class DeviceResolver {

    private final BackendApi backend;

    public DeviceResolver(BackendApi backend) {
        this.backend = backend;
    }

    public Optional<DeviceDto> resolve(String idOrName) {
        if (idOrName == null || idOrName.isBlank()) {
            return Optional.empty();
        }
        String needle = idOrName.trim().toLowerCase();
        List<DeviceDto> devices = backend.listDevices();
        Optional<DeviceDto> byId = devices.stream()
                .filter(d -> d.id().equalsIgnoreCase(idOrName.trim()))
                .findFirst();
        if (byId.isPresent()) {
            return byId;
        }
        return devices.stream().filter(d -> d.name().equalsIgnoreCase(idOrName.trim())).findFirst()
                .or(() -> devices.stream()
                        .filter(d -> d.name().toLowerCase().contains(needle))
                        .findFirst());
    }
}
