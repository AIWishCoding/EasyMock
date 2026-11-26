package com.aiwishcoding.easymock.service;

import com.aiwishcoding.easymock.model.MatchType;
import com.aiwishcoding.easymock.model.MockRule;
import com.aiwishcoding.easymock.repository.MockRuleRepository;
import com.aiwishcoding.easymock.util.JsonPathEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MockRuleMatcherTest {

    private MockRuleRepository repository;
    private MockRuleMatcher matcher;

    @BeforeEach
    void setUp() {
        repository = mock(MockRuleRepository.class);
        matcher = new MockRuleMatcher(repository, new JsonPathEvaluator());
    }

    @Test
    void matchesPathVariablesAndKeepsPriorityOrder() {
        when(repository.findAllByEnabledTrueOrderByPriorityDescIdAsc())
                .thenReturn(Arrays.asList(rule(20L, "/api/items/{id}", MatchType.URL),
                        rule(10L, "/api/items/42", MatchType.URL)));
        assertEquals("rule-20", matcher.findMatch("/api/items/42", "{}").get().getRuleName());
    }

    @Test
    void parameterRuleWinsOverHigherPriorityUrlRule() {
        MockRule parameter = rule(10L, "/api/items/{id}", MatchType.PARAMETER);
        parameter.setParamConditions("[{\"conditions\":[{\"path\":\"$.ok\",\"operator\":\"EQUALS\","
                + "\"value\":\"true\"}],\"responseBody\":\"{\\\"matched\\\":true}\"}]");
        parameter.setName("parameter");
        when(repository.findAllByEnabledTrueOrderByPriorityDescIdAsc())
                .thenReturn(Arrays.asList(rule(100L, "/api/items/{id}", MatchType.URL), parameter));
        assertEquals("parameter", matcher.findMatch("/api/items/42", "{\"ok\":true}")
                .get().getRuleName());
        assertEquals("rule-100", matcher.findMatch("/api/items/42", "{\"ok\":false}")
                .get().getRuleName());
    }

    @Test
    void ignoresUnrelatedPaths() {
        when(repository.findAllByEnabledTrueOrderByPriorityDescIdAsc())
                .thenReturn(Collections.singletonList(rule(1L, "/api/other", MatchType.URL)));
        assertFalse(matcher.findMatch("/api/items/42", "{}").isPresent());
    }

    @Test
    void fallsBackToRealImplementationWhenNothingMatches() {
        when(repository.findAllByEnabledTrueOrderByPriorityDescIdAsc())
                .thenReturn(Collections.emptyList());
        assertEquals(Optional.empty(), matcher.findMatch("/api/items/42", "{}"));
    }

    private MockRule rule(long id, String url, MatchType type) {
        MockRule rule = new MockRule();
        rule.setId(id);
        rule.setName("rule-" + id);
        rule.setUrl(url);
        rule.setMatchType(type);
        rule.setResponseBody("{}");
        return rule;
    }
}
