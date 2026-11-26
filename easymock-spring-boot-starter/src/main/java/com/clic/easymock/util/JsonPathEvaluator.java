package com.clic.easymock.util;

import com.clic.easymock.model.ConditionGroup;
import com.clic.easymock.model.ParamCondition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class JsonPathEvaluator {

    private static final Logger log = LoggerFactory.getLogger(JsonPathEvaluator.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Evaluate condition groups from paramConditionsJson.
     * Supports both new format (ConditionGroup array) and legacy format (flat ParamCondition array).
     * Returns the first matched ConditionGroup, or empty if none matches.
     */
    public Optional<ConditionGroup> evaluateGroups(String requestBodyJson, String paramConditionsJson) {
        if (paramConditionsJson == null || paramConditionsJson.trim().isEmpty()) {
            return Optional.empty();
        }
        if (requestBodyJson == null || requestBodyJson.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            List<Map<String, Object>> rawList = objectMapper.readValue(paramConditionsJson,
                    new TypeReference<List<Map<String, Object>>>() {});
            if (rawList == null || rawList.isEmpty()) {
                return Optional.empty();
            }

            // Detect format by checking the first element
            Map<String, Object> first = rawList.get(0);
            if (first.containsKey("conditions")) {
                // New format: ConditionGroup array
                List<ConditionGroup> groups = objectMapper.readValue(paramConditionsJson,
                        new TypeReference<List<ConditionGroup>>() {});
                for (ConditionGroup group : groups) {
                    if (group.getConditions() == null || group.getConditions().isEmpty()
                            || evaluateConditionList(requestBodyJson, group.getConditions())) {
                        return Optional.of(group);
                    }
                }
            } else if (first.containsKey("path")) {
                // Legacy format: flat ParamCondition array
                List<ParamCondition> conditions = objectMapper.readValue(paramConditionsJson,
                        new TypeReference<List<ParamCondition>>() {});
                if (conditions == null || conditions.isEmpty()
                        || evaluateConditionList(requestBodyJson, conditions)) {
                    // Wrap as anonymous ConditionGroup with null responseBody (caller will fallback to rule-level)
                    ConditionGroup legacyGroup = new ConditionGroup(null, conditions, null);
                    return Optional.of(legacyGroup);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to evaluate param condition groups: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Legacy method: evaluate flat ParamCondition list with AND logic.
     * Kept for backward compatibility.
     */
    public boolean evaluate(String requestBodyJson, String paramConditionsJson) {
        if (paramConditionsJson == null || paramConditionsJson.trim().isEmpty()) {
            return true;
        }
        if (requestBodyJson == null || requestBodyJson.trim().isEmpty()) {
            return false;
        }

        try {
            List<ParamCondition> conditions = objectMapper.readValue(paramConditionsJson,
                    new TypeReference<List<ParamCondition>>() {});
            if (conditions == null || conditions.isEmpty()) {
                return true;
            }
            return evaluateConditionList(requestBodyJson, conditions);
        } catch (Exception e) {
            log.warn("Failed to evaluate param conditions: {}", e.getMessage());
            return false;
        }
    }

    private boolean evaluateConditionList(String requestBodyJson, List<ParamCondition> conditions) {
        for (ParamCondition condition : conditions) {
            if (!evaluateSingle(requestBodyJson, condition)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateSingle(String json, ParamCondition condition) {
        String operator = condition.getOperator().toUpperCase();

        if ("EXISTS".equals(operator)) {
            return pathExists(json, condition.getPath());
        }
        if ("NOT_EXISTS".equals(operator)) {
            return !pathExists(json, condition.getPath());
        }

        Object actualValue;
        try {
            actualValue = JsonPath.read(json, condition.getPath());
        } catch (PathNotFoundException e) {
            return false;
        } catch (Exception e) {
            log.warn("JsonPath read error for path '{}': {}", condition.getPath(), e.getMessage());
            return false;
        }

        if (actualValue == null) {
            return false;
        }

        String actualStr = String.valueOf(actualValue);
        String expectedStr = condition.getValue();

        switch (operator) {
            case "EQUALS":
                return actualStr.equals(expectedStr);
            case "NOT_EQUALS":
                return !actualStr.equals(expectedStr);
            case "CONTAINS":
                return actualStr.contains(expectedStr);
            case "GT":
                return compareNumeric(actualStr, expectedStr) > 0;
            case "GTE":
                return compareNumeric(actualStr, expectedStr) >= 0;
            case "LT":
                return compareNumeric(actualStr, expectedStr) < 0;
            case "LTE":
                return compareNumeric(actualStr, expectedStr) <= 0;
            case "REGEX":
                return Pattern.matches(expectedStr, actualStr);
            default:
                log.warn("Unknown operator: {}", operator);
                return false;
        }
    }

    private boolean pathExists(String json, String path) {
        try {
            JsonPath.read(json, path);
            return true;
        } catch (PathNotFoundException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private int compareNumeric(String actual, String expected) {
        try {
            BigDecimal a = new BigDecimal(actual);
            BigDecimal b = new BigDecimal(expected);
            return a.compareTo(b);
        } catch (NumberFormatException e) {
            return actual.compareTo(expected);
        }
    }
}
