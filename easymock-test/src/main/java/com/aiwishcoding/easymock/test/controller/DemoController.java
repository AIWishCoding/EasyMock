package com.aiwishcoding.easymock.test.controller;

import com.aiwishcoding.easymock.test.service.ExternalApiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class DemoController {

    private final ExternalApiService externalApiService;

    public DemoController(ExternalApiService externalApiService) {
        this.externalApiService = externalApiService;
    }

    @PostMapping("/api/order/create")
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "Order created successfully");

        Map<String, Object> data = new HashMap<>();
        data.put("orderId", UUID.randomUUID().toString());
        data.put("productName", request.get("productName"));
        data.put("amount", request.get("amount"));
        data.put("status", "CREATED");
        result.put("data", data);

        return result;
    }

    @PostMapping("/api/user/register")
    public Map<String, Object> registerUser(@RequestBody Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "User registered successfully");

        Map<String, Object> data = new HashMap<>();
        data.put("userId", UUID.randomUUID().toString());
        data.put("username", request.get("username"));
        data.put("email", request.get("email"));
        data.put("status", "ACTIVE");
        result.put("data", data);

        return result;
    }

    @GetMapping("/api/product/list")
    public Map<String, Object> listProducts(@RequestParam(required = false) String category) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "Product list fetched");
        result.put("category", category);
        result.put("total", 0);
        result.put("data", new Object[]{});
        return result;
    }

    /**
     * This endpoint internally calls ExternalApiService.sendNotification(),
     * which is annotated with @MockApi("/api/external/notify").
     * When a mock rule matches "/api/external/notify", the service call returns mock data.
     */
    @PostMapping("/api/test/notify")
    public Map<String, Object> testNotify(@RequestBody Map<String, Object> request) {
        return externalApiService.sendNotification(request);
    }
}
