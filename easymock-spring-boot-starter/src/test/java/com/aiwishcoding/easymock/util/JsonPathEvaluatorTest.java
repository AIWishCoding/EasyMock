package com.aiwishcoding.easymock.util;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonPathEvaluatorTest {

    private final JsonPathEvaluator evaluator = new JsonPathEvaluator();

    @Test
    void evaluatesAllSupportedOperators() {
        assertTrue(evaluator.evaluate("{\"n\":10,\"s\":\"abc\"}", "[" + condition("$.n", "EQUALS", "10") + "]"));
        assertTrue(evaluator.evaluate("{\"n\":10}", "[" + condition("$.n", "NOT_EQUALS", "11") + "]"));
        assertTrue(evaluator.evaluate("{\"s\":\"abc\"}", "[" + condition("$.s", "CONTAINS", "b") + "]"));
        assertTrue(evaluator.evaluate("{\"n\":10}", "[" + condition("$.n", "GT", "9") + "]"));
        assertTrue(evaluator.evaluate("{\"n\":10}", "[" + condition("$.n", "GTE", "10") + "]"));
        assertTrue(evaluator.evaluate("{\"n\":10}", "[" + condition("$.n", "LT", "11") + "]"));
        assertTrue(evaluator.evaluate("{\"n\":10}", "[" + condition("$.n", "LTE", "10") + "]"));
        assertTrue(evaluator.evaluate("{\"s\":\"abc\"}", "[" + condition("$.s", "REGEX", "a.*") + "]"));
        assertTrue(evaluator.evaluate("{\"n\":10}", "[" + condition("$.n", "EXISTS", null) + "]"));
        assertTrue(evaluator.evaluate("{\"n\":10}", "[" + condition("$.missing", "NOT_EXISTS", null) + "]"));
    }

    @Test
    void usesAndWithinGroupsAndFallsThroughToEmptyCatchAllGroup() {
        String json = "[{\"name\":\"first\",\"conditions\":["
                + condition("$.n", "GT", "9") + "," + condition("$.s", "EQUALS", "abc")
                + "],\"responseBody\":\"{\\\"group\\\":1}\"},"
                + "{\"name\":\"second\",\"conditions\":[],\"responseBody\":\"{\\\"group\\\":2}\"}]";
        assertTrue(evaluator.evaluateGroups("{\"n\":10,\"s\":\"abc\"}", json).get().getName()
                .equals("first"));
        assertTrue(evaluator.evaluateGroups("{\"n\":10,\"s\":\"other\"}", json).get().getName()
                .equals("second"));
    }

    @Test
    void supportsLegacyFlatConditions() {
        String json = "[" + condition("$.n", "GTE", "10") + "]";
        assertTrue(evaluator.evaluateGroups("{\"n\":10}", json).isPresent());
    }

    @Test
    void invalidInputDoesNotMatch() {
        assertFalse(evaluator.evaluateGroups("not-json", "[]").isPresent());
        assertFalse(evaluator.evaluateGroups("{}", "[" + condition("$.n", "UNKNOWN", "1") + "]").isPresent());
        assertFalse(evaluator.evaluateGroups("{}", "[" + condition("$.n", "REGEX", "[") + "]").isPresent());
    }

    private String condition(String path, String operator, String value) {
        return "{\"path\":\"" + path + "\",\"operator\":\"" + operator + "\",\"value\":"
                + (value == null ? "null" : "\"" + value + "\"") + "}";
    }
}
