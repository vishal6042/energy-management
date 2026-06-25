package com.dems.backend.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Energy-saving algorithm applied to a device. Serialized as lowercase. */
public enum AlgorithmType {
    COMFORT("comfort"),
    TARGET("target"),
    NONE("none");

    private final String value;

    AlgorithmType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AlgorithmType fromValue(String value) {
        for (AlgorithmType a : values()) {
            if (a.value.equalsIgnoreCase(value)) {
                return a;
            }
        }
        throw new IllegalArgumentException("Unknown algorithm: " + value);
    }
}
