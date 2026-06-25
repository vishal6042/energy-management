package com.dems.backend.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Connectivity status of a device. Serialized as lowercase. */
public enum DeviceStatus {
    ONLINE("online"),
    OFFLINE("offline");

    private final String value;

    DeviceStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static DeviceStatus fromValue(String value) {
        for (DeviceStatus s : values()) {
            if (s.value.equalsIgnoreCase(value)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}
