package com.aiwishcoding.easymock.test.service;

import com.aiwishcoding.easymock.annotation.MockApi;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Simulates an external API call service.
 * The @MockApi annotation allows EasyMock to intercept and mock these calls.
 */
@Service
public class ExternalApiService {

    @MockApi("/api/external/notify")
    public Map<String, Object> sendNotification(Map<String, Object> request) {
        // In real implementation, this would call an external HTTP API
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "Notification sent to: " + request.get("target"));
        result.put("status", "SENT");
        return result;
    }
}
