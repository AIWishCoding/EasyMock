package com.aiwishcoding.easymock.test;

import com.aiwishcoding.easymock.model.MockRule;
import com.aiwishcoding.easymock.model.MatchType;
import com.aiwishcoding.easymock.repository.MockRuleRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.ServletException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "easymock.enabled=true",
        "easymock.admin-token=integration-token-1234567890",
        "easymock.db-path=target/integration-easymock"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import(EasyMockIntegrationTest.TestEndpoints.class)
class EasyMockIntegrationTest {

    private static final String AUTH = "Basic " + java.util.Base64.getEncoder().encodeToString(
            "easymock:integration-token-1234567890".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockRuleRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        TestEndpoints.realCalls.set(0);
    }

    @Test
    void managementPagesAndApisRequireAuthentication() throws Exception {
        mockMvc.perform(get("/easymock")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/easymock/api/rules")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/easymock/api;x/rules")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/easymock").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("可 Mock 接口列表")));
    }

    @Test
    void createsRuleAndAppliesStatusToOrdinaryController() throws Exception {
        createRule("/api/product/list", MatchType.URL, "{\"mocked\":true}", null, 201, true);
        mockMvc.perform(get("/api/product/list"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mocked").value(true));
    }

    @Test
    void configuredStatusOverridesResponseStatusAnnotation() throws Exception {
        createRule("/api/annotated-status", MatchType.URL, "{\"mocked\":true}", null, 418, true);
        mockMvc.perform(get("/api/annotated-status"))
                .andExpect(status().is(418))
                .andExpect(jsonPath("$.mocked").value(true));
    }

    @Test
    void matchesRulesInsideServletContextPath() throws Exception {
        createRule("/api/product/list", MatchType.URL, "{\"context\":true}", null, 200, true);
        mockMvc.perform(get("/app/api/product/list").contextPath("/app"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.context").value(true));
    }

    @Test
    void matchesPathVariableRules() throws Exception {
        createRule("/api/items/{id}", MatchType.URL, "{\"id\":42}", null, 200, true);
        mockMvc.perform(get("/api/items/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42));
    }

    @Test
    void matchesParameterGroupsBeforeUrlFallback() throws Exception {
        String conditions = "[{\"name\":\"large\",\"conditions\":[{\"path\":\"$.amount\","
                + "\"operator\":\"GT\",\"value\":\"100\"}],\"responseBody\":\"{\\\"group\\\":\\\"large\\\"}\"}]";
        createRule("/api/order/create", MatchType.PARAMETER, "{}", conditions, 409, true);
        mockMvc.perform(post("/api/order/create").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":200}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.group").value("large"));
    }

    @Test
    void deserializesNestedResponseEntityGenerics() throws Exception {
        createRule("/api/response-entity", MatchType.URL,
                "{\"data\":{\"ok\":true}}", null, 418, true);
        mockMvc.perform(get("/api/response-entity"))
                .andExpect(status().is(418))
                .andExpect(jsonPath("$.data.ok").value(true));
    }

    @Test
    void invalidMockResponseDoesNotExecuteBusinessMethod() throws Exception {
        MockRule invalid = new MockRule();
        invalid.setName("invalid legacy rule");
        invalid.setUrl("/api/must-not-run");
        invalid.setMatchType(MatchType.URL);
        invalid.setResponseBody("not-json");
        invalid.setResponseStatus(200);
        invalid.setEnabled(true);
        invalid.setPriority(0);
        repository.saveAndFlush(invalid);
        assertThrows(ServletException.class, () ->
                mockMvc.perform(get("/api/must-not-run")));
        assertEquals(0, TestEndpoints.realCalls.get());
    }

    @Test
    void disabledRuleLeavesBusinessResponseUntouched() throws Exception {
        createRule("/api/product/list", MatchType.URL, "{\"mocked\":true}", null, 201, false);
        mockMvc.perform(get("/api/product/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void invalidRuleAndImportReturnStructuredBadRequests() throws Exception {
        Map<String, Object> invalid = rule("/api/product/list", MatchType.URL,
                "not-json", null, 200, true);
        mockMvc.perform(post("/easymock/api/rules").header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        org.springframework.mock.web.MockMultipartFile badFile =
                new org.springframework.mock.web.MockMultipartFile("file", "bad.json",
                        MediaType.APPLICATION_JSON_VALUE, "not-json".getBytes());
        mockMvc.perform(multipart("/easymock/api/import").file(badFile)
                        .header("Authorization", AUTH))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("合法 JSON")));
    }

    @Test
    void crossOriginWritesAreRejectedAndMissingRulesReturn404() throws Exception {
        mockMvc.perform(post("/easymock/api/rules").header("Authorization", AUTH)
                        .header("Origin", "https://evil.example")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/easymock/api/rules/999").header("Authorization", AUTH))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    private void createRule(String url, MatchType type, String responseBody, String conditions,
                            int status, boolean enabled) throws Exception {
        mockMvc.perform(post("/easymock/api/rules").header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                rule(url, type, responseBody, conditions, status, enabled))))
                .andExpect(status().isOk());
    }

    private Map<String, Object> rule(String url, MatchType type, String responseBody,
                                     String conditions, int status, boolean enabled) {
        Map<String, Object> rule = new java.util.LinkedHashMap<>();
        rule.put("name", "integration");
        rule.put("url", url);
        rule.put("matchType", type);
        rule.put("paramConditions", conditions);
        rule.put("responseBody", responseBody);
        rule.put("responseStatus", status);
        rule.put("enabled", enabled);
        rule.put("priority", 0);
        return rule;
    }

    @RestController
    static class TestEndpoints {
        static final AtomicInteger realCalls = new AtomicInteger();

        @GetMapping("/api/annotated-status")
        @ResponseStatus(HttpStatus.CREATED)
        public Map<String, Object> annotatedStatus() {
            return Collections.singletonMap("real", true);
        }

        @GetMapping("/api/items/{id}")
        public Map<String, Object> item(@PathVariable String id) {
            return Collections.singletonMap("realId", id);
        }

        @GetMapping("/api/response-entity")
        public ResponseEntity<Envelope<Map<String, Object>>> responseEntity() {
            return ResponseEntity.ok(new Envelope<>(Collections.singletonMap("real", true)));
        }

        @GetMapping("/api/must-not-run")
        public Map<String, Object> mustNotRun() {
            realCalls.incrementAndGet();
            throw new IllegalStateException("real business method executed");
        }

        @PostMapping("/api/parameter-only")
        public Map<String, Object> parameter(@RequestBody Map<String, Object> body) {
            return body;
        }
    }

    public static class Envelope<T> {
        private T data;

        public Envelope() {
        }

        public Envelope(T data) {
            this.data = data;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }
}
