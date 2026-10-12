package com.example.EVIM.adapter.in.web;

import com.example.EVIM.adapter.out.persistence.entity.ProviderConfig;
import com.example.EVIM.adapter.out.persistence.ProviderConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/admin/config")
public class ProviderController {

    private final ProviderConfigService configService;

    public ProviderController(ProviderConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public ResponseEntity<List<ProviderConfig>> getAll() {
        return ResponseEntity.ok(configService.findAll());
    }

    @PostMapping
    public ResponseEntity<ProviderConfig> create(@RequestBody Map<String, String> body) {
        ProviderConfig saved = configService.save(
                body.get("key"),
                body.get("value"),
                body.get("description")
        );
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/public")
    public ResponseEntity<Map<String, String>> getPublicConfig() {
        Map<String, String> result = new HashMap<>();
        result.put("internalDomain", configService.getInternalDomain());
        result.put("jiraBaseUrl",    configService.getBaseUrl());
        configService.findAll().stream()
                .filter(c -> c.getKey().startsWith("status.color."))
                .forEach(c -> result.put(c.getKey(), c.getValue()));
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{key}")
    public ResponseEntity<ProviderConfig> update(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        ProviderConfig saved = configService.save(key, body.get("value"), body.get("description"));
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        configService.delete(id);
        return ResponseEntity.noContent().build();
    }

}

