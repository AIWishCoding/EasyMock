package com.aiwishcoding.easymock.security;

import com.aiwishcoding.easymock.autoconfigure.EasyMockProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EasyMockAdminFilterTest {

    private EasyMockProperties properties;
    private EasyMockAdminFilter filter;

    @BeforeEach
    void setUp() {
        properties = new EasyMockProperties();
        properties.setAdminToken("review-token-1234567890");
        filter = new EasyMockAdminFilter(properties);
    }

    @Test
    void rejectsUnauthenticatedManagementRequests() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("GET", "/easymock/api/rules", null, null), response,
                new MockFilterChain());
        assertEquals(401, response.getStatus());
        assertNotNull(response.getHeader("WWW-Authenticate"));
    }

    @Test
    void acceptsTokenHeaderBasicAuthAndBearerAuth() throws Exception {
        for (String authorization : new String[]{basic(), "Bearer review-token-1234567890"}) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request("GET", "/easymock/api/rules", authorization, null), response,
                    new MockFilterChain());
            assertEquals(200, response.getStatus());
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("GET", "/easymock/api/rules", null,
                "review-token-1234567890"), response, new MockFilterChain());
        assertEquals(200, response.getStatus());
    }

    @Test
    void rejectsCrossOriginWritesButAllowsSameOriginAndNonBrowserClients() throws Exception {
        assertEquals(403, status("POST", "/easymock/api/rules", "https://evil.example"));
        assertEquals(200, status("POST", "/easymock/api/rules", "http://localhost"));
        assertEquals(200, status("POST", "/easymock/api/rules", null));
    }

    @Test
    void doesNotBlockBusinessApisWithSimilarPrefix() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("GET", "/easymock-business/api", null, null), response,
                new MockFilterChain());
        assertEquals(200, response.getStatus());
    }

    @Test
    void rejectsUnauthenticatedRequestsWithPathParameters() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request("GET", "/easymock/api;x/rules", null, null), response,
                new MockFilterChain());
        assertEquals(401, response.getStatus());
    }

    @Test
    void rejectsWeakTokensAtStartup() {
        properties.setAdminToken("short");
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> new EasyMockAdminFilter(properties));
    }

    private int status(String method, String path, String origin) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = request(method, path, basic(), null);
        if (origin != null) request.addHeader("Origin", origin);
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    private MockHttpServletRequest request(String method, String path, String authorization,
                                           String token) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServerName("localhost");
        request.setServerPort(80);
        request.setScheme("http");
        if (authorization != null) request.addHeader("Authorization", authorization);
        if (token != null) request.addHeader("X-EasyMock-Token", token);
        return request;
    }

    private String basic() {
        return "Basic " + Base64.getEncoder().encodeToString(
                "easymock:review-token-1234567890".getBytes(StandardCharsets.UTF_8));
    }
}
