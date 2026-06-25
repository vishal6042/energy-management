package com.dems.backend.web;

import com.dems.backend.dto.DeviceSavingsDto;
import com.dems.backend.dto.SavingsSummaryDto;
import com.dems.backend.service.SavingsService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/savings")
public class SavingsController {

    private final SavingsService savingsService;

    public SavingsController(SavingsService savingsService) {
        this.savingsService = savingsService;
    }

    @GetMapping("/summary")
    public SavingsSummaryDto getSummary() {
        return savingsService.getSummary();
    }

    @GetMapping("/devices")
    public List<DeviceSavingsDto> getDeviceSavings() {
        return savingsService.getDeviceSavings();
    }
}
