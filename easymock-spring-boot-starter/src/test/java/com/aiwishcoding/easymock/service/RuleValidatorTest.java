package com.aiwishcoding.easymock.service;

import com.aiwishcoding.easymock.dto.MockRuleRequest;
import com.aiwishcoding.easymock.exception.RuleValidationException;
import com.aiwishcoding.easymock.model.MatchType;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuleValidatorTest {

    private ValidatorFactory factory;
    private RuleValidator validator;

    @BeforeEach
    void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = new RuleValidator(factory.getValidator(), new ObjectMapper());
    }

    @Test
    void acceptsValidUrlAndParameterRules() {
        assertDoesNotThrow(() -> validator.validate(request(MatchType.URL, null, "{}")));
        assertDoesNotThrow(() -> validator.validate(request(MatchType.PARAMETER,
                "[{\"conditions\":[{\"path\":\"$.id\",\"operator\":\"EXISTS\"}],"
                        + "\"responseBody\":\"{\\\"ok\\\":true}\"}]", "{}")));
    }

    @Test
    void rejectsInvalidJsonStatusPathAndMissingConditions() {
        assertThrows(RuleValidationException.class,
                () -> validator.validate(request(MatchType.URL, null, "not-json")));
        MockRuleRequest status = request(MatchType.URL, null, "{}");
        status.setResponseStatus(99);
        assertThrows(RuleValidationException.class, () -> validator.validate(status));
        MockRuleRequest path = request(MatchType.URL, null, "{}");
        path.setUrl("api/items");
        assertThrows(RuleValidationException.class, () -> validator.validate(path));
        MockRuleRequest pattern = request(MatchType.URL, null, "{}");
        pattern.setUrl("/api/{id}{name}");
        assertThrows(RuleValidationException.class, () -> validator.validate(pattern));
        assertThrows(RuleValidationException.class,
                () -> validator.validate(request(MatchType.PARAMETER, null, "{}")));
    }

    @Test
    void rejectsInvalidConditionsAndRegex() {
        assertThrows(RuleValidationException.class, () -> validator.validate(request(MatchType.PARAMETER,
                "[{\"conditions\":[]}]", "{}")));
        assertThrows(RuleValidationException.class, () -> validator.validate(request(MatchType.PARAMETER,
                "[{\"conditions\":[{\"path\":\"$.id\",\"operator\":\"UNKNOWN\",\"value\":\"1\"}]}]", "{}")));
        assertThrows(RuleValidationException.class, () -> validator.validate(request(MatchType.PARAMETER,
                "[{\"conditions\":[{\"path\":\"$.id\",\"operator\":\"REGEX\",\"value\":\"[\"}]}]", "{}")));
        assertThrows(RuleValidationException.class, () -> validator.validate(request(MatchType.PARAMETER,
                "[{\"conditions\":[{\"path\":\"id\",\"operator\":\"EXISTS\"}]}]", "{}")));
    }

    private MockRuleRequest request(MatchType type, String conditions, String responseBody) {
        MockRuleRequest request = new MockRuleRequest();
        request.setName("test");
        request.setUrl("/api/items/{id}");
        request.setMatchType(type);
        request.setParamConditions(conditions);
        request.setResponseBody(responseBody);
        request.setResponseStatus(200);
        request.setEnabled(true);
        request.setPriority(0);
        return request;
    }
}
