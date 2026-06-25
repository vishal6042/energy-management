package com.dems.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A single savings data point (cost + energy saved) for a device over a period. */
@Entity
@Table(name = "saving_records")
public class SavingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceId;

    @Enumerated(EnumType.STRING)
    private Granularity granularity;

    /** YYYY-MM-DD for daily, YYYY-MM for monthly. */
    private String period;

    private double costSaved;
    private double energySavedKwh;

    /** Energy consumed by the device in the period (kWh). */
    private double usageKwh;

    /** Cost of the energy consumed in the period. */
    private double usageCost;

    protected SavingRecord() {
        // for JPA
    }

    public SavingRecord(String deviceId, Granularity granularity, String period,
                        double costSaved, double energySavedKwh,
                        double usageKwh, double usageCost) {
        this.deviceId = deviceId;
        this.granularity = granularity;
        this.period = period;
        this.costSaved = costSaved;
        this.energySavedKwh = energySavedKwh;
        this.usageKwh = usageKwh;
        this.usageCost = usageCost;
    }

    public Long getId() {
        return id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public Granularity getGranularity() {
        return granularity;
    }

    public String getPeriod() {
        return period;
    }

    public double getCostSaved() {
        return costSaved;
    }

    public double getEnergySavedKwh() {
        return energySavedKwh;
    }

    public double getUsageKwh() {
        return usageKwh;
    }

    public double getUsageCost() {
        return usageCost;
    }

    public void setCostSaved(double costSaved) {
        this.costSaved = costSaved;
    }

    public void setEnergySavedKwh(double energySavedKwh) {
        this.energySavedKwh = energySavedKwh;
    }

    public void setUsageKwh(double usageKwh) {
        this.usageKwh = usageKwh;
    }

    public void setUsageCost(double usageCost) {
        this.usageCost = usageCost;
    }
}
