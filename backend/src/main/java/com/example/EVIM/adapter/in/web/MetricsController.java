package com.example.EVIM.adapter.in.web;

import com.example.EVIM.application.SlaMetricsService;
import com.example.EVIM.application.dto.MetricsDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private final SlaMetricsService slaMetricsService;

    public MetricsController(SlaMetricsService slaMetricsService) {
        this.slaMetricsService = slaMetricsService;
    }

    @GetMapping
    public ResponseEntity<MetricsDTO> getMetrics(
            @RequestParam(required = false) Long sprintId) {
        return ResponseEntity.ok(slaMetricsService.getMetrics(sprintId));
    }
}
