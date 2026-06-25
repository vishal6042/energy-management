package com.dems.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** An AC device that can have an energy-saving algorithm applied. */
@Entity
@Table(name = "devices")
public class Device {

    @Id
    private String id;

    private String name;
    private String location;
    private String model;

    @Enumerated(EnumType.STRING)
    private DeviceStatus status;

    /** Current measured room temperature in °C. */
    private double currentTempC;

    /** Configured setpoint in °C. */
    private double setpointC;

    /** Instantaneous power draw in watts (0 when offline). */
    private int powerW;

    /** Rated power draw in watts, used for usage generation and restored on turn-on. */
    private int nominalPowerW;

    @Enumerated(EnumType.STRING)
    private AlgorithmType algorithm;

    protected Device() {
        // for JPA
    }

    public Device(String id, String name, String location, String model,
                  DeviceStatus status, double currentTempC, double setpointC,
                  int powerW, int nominalPowerW, AlgorithmType algorithm) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.model = model;
        this.status = status;
        this.currentTempC = currentTempC;
        this.setpointC = setpointC;
        this.powerW = powerW;
        this.nominalPowerW = nominalPowerW;
        this.algorithm = algorithm;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getModel() {
        return model;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public double getCurrentTempC() {
        return currentTempC;
    }

    public double getSetpointC() {
        return setpointC;
    }

    public int getPowerW() {
        return powerW;
    }

    public int getNominalPowerW() {
        return nominalPowerW;
    }

    public AlgorithmType getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(AlgorithmType algorithm) {
        this.algorithm = algorithm;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public void setPowerW(int powerW) {
        this.powerW = powerW;
    }
}
