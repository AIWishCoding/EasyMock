package com.aiwishcoding.easymock.service;

import com.aiwishcoding.easymock.dto.MockRuleRequest;
import com.aiwishcoding.easymock.exception.RuleValidationException;
import com.aiwishcoding.easymock.model.MatchType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RuleValidator {

    private static final List<String> OPERATORS = Arrays.asList(
            "EQUALS", "NOT_EQUALS", "CONTAINS", "GT", "GTE", "LT", "LTE",
            "REGEX", "EXISTS", "NOT_EXISTS");
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final Validator validator;
    private final ObjectMapper objectMapper;

    public RuleValidator(Validator validator, ObjectMapper objectMapper) {
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    public void validate(MockRuleRequest request) {
        if (request == null) {
            throw new RuleValidationException("规则不能为空");
        }
        Set<ConstraintViolation<MockRuleRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
            throw new RuleValidationException(message);
        }
        validateJson(request.getResponseBody(), "responseBody");
        validateUrlPattern(request.getUrl());
        if (request.getMatchType() == MatchType.PARAMETER) {
            validateConditions(request.getParamConditions());
        }
    }

    private void validateUrlPattern(String url) {
        int index = 0;
        while ((index = url.indexOf('{', index)) >= 0) {
            int closing = url.indexOf('}', index + 1);
            int nextOpening = url.indexOf('{', index + 1);
            if (closing <= index + 1 || nextOpening >= 0 && nextOpening < closing
                    || closing + 1 < url.length() && url.charAt(closing + 1) == '{') {
                throw new RuleValidationException("无效的 URL 匹配模式: " + url);
            }
            index = closing + 1;
        }
        try {
            PATH_MATCHER.match(url, "/");
        } catch (IllegalArgumentException e) {
            throw new RuleValidationException("无效的 URL 匹配模式: " + url);
        }
    }

    private void validateJson(String json, String field) {
        try {
            objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuleValidationException(field + " 必须是合法 JSON");
        }
    }

    private void validateConditions(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new RuleValidationException("参数匹配规则必须包含条件组");
        }
        try {
            JsonNode groups = objectMapper.readTree(json);
            if (!groups.isArray() || groups.size() == 0) {
                throw new RuleValidationException("paramConditions 必须是非空数组");
            }
            for (JsonNode group : groups) {
                if (group.hasNonNull("responseBody")) {
                    validateJson(group.get("responseBody").asString(), "条件组 responseBody");
                }
                JsonNode conditions = group.has("conditions") ? group.get("conditions") : group;
                if (!conditions.isArray() || conditions.size() == 0) {
                    throw new RuleValidationException("每个条件组至少包含一个条件");
                }
                for (JsonNode condition : conditions) {
                    validateCondition(condition);
                }
            }
        } catch (RuleValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new RuleValidationException("paramConditions 必须是合法 JSON 数组");
        }
    }

    private void validateCondition(JsonNode condition) {
        String path = condition.path("path").asString();
        String operator = condition.path("operator").asString().toUpperCase();
        if (!path.startsWith("$")) {
            throw new RuleValidationException("条件 path 必须以 $ 开头");
        }
        try {
            JsonPath.compile(path);
        } catch (Exception e) {
            throw new RuleValidationException("无效 JsonPath: " + path);
        }
        if (!OPERATORS.contains(operator)) {
            throw new RuleValidationException("无效条件操作符: " + operator);
        }
        if (!Arrays.asList("EXISTS", "NOT_EXISTS").contains(operator)
                && !condition.hasNonNull("value")) {
            throw new RuleValidationException("条件 " + operator + " 必须提供 value");
        }
        if ("REGEX".equals(operator)) {
            try {
                java.util.regex.Pattern.compile(condition.get("value").asString());
            } catch (Exception e) {
                throw new RuleValidationException("无效正则表达式: " + condition.get("value").asString());
            }
        }
    }
}
