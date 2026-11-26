package com.clic.easymock.controller;

import com.clic.easymock.model.MockRule;
import com.clic.easymock.scanner.EndpointScanner;
import com.clic.easymock.service.MockRuleService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${easymock.api-path:/easymock/api}")
public class MockRuleApiController {

    private final MockRuleService mockRuleService;
    private final EndpointScanner endpointScanner;

    public MockRuleApiController(MockRuleService mockRuleService,
                                 EndpointScanner endpointScanner) {
        this.mockRuleService = mockRuleService;
        this.endpointScanner = endpointScanner;
    }

    @GetMapping("/rules")
    public ResponseEntity<List<MockRule>> listRules() {
        return ResponseEntity.ok(mockRuleService.listAll());
    }

    @PostMapping("/rules")
    public ResponseEntity<MockRule> createRule(@RequestBody MockRule rule) {
        return ResponseEntity.ok(mockRuleService.create(rule));
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<MockRule> updateRule(@PathVariable Long id,
                                               @RequestBody MockRule rule) {
        return ResponseEntity.ok(mockRuleService.update(id, rule));
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Map<String, Object>> deleteRule(@PathVariable Long id) {
        mockRuleService.delete(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/rules/{id}/toggle")
    public ResponseEntity<MockRule> toggleRule(@PathVariable Long id) {
        mockRuleService.toggleEnabled(id);
        return ResponseEntity.ok(mockRuleService.getById(id));
    }

    @GetMapping("/endpoints")
    public ResponseEntity<List<String>> scanEndpoints() {
        return ResponseEntity.ok(endpointScanner.scanEndpoints());
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportRules(
            @RequestParam(required = false) String url) {
        String json;
        if (url != null && !url.trim().isEmpty()) {
            json = mockRuleService.exportByUrl(url);
        } else {
            json = mockRuleService.exportAll();
        }

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=easymock-rules.json")
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(bytes.length)
                .body(bytes);
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importRules(
            @RequestParam("file") MultipartFile file) {
        try {
            String json = new String(file.getBytes(), StandardCharsets.UTF_8);
            int count = mockRuleService.importRules(json);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("count", count);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Failed to read file: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}
