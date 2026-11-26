package com.aiwishcoding.easymock.service;

import com.aiwishcoding.easymock.dto.MockRuleRequest;
import com.aiwishcoding.easymock.exception.RuleNotFoundException;
import com.aiwishcoding.easymock.exception.RuleValidationException;
import com.aiwishcoding.easymock.model.MatchType;
import com.aiwishcoding.easymock.model.MockRule;
import com.aiwishcoding.easymock.repository.MockRuleRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MockRuleServiceImplTest {

    private MockRuleRepository repository;
    private MockRuleServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(MockRuleRepository.class);
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        RuleValidator validator = new RuleValidator(factory.getValidator(), new ObjectMapper());
        service = new MockRuleServiceImpl(repository, validator);
    }

    @Test
    void updatePreservesServerManagedFieldsAndAppliesRequestFields() {
        MockRule existing = existing();
        existing.setCreatedAt(null);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(existing)).thenReturn(existing);
        MockRuleRequest request = request();
        request.setResponseStatus(201);
        request.setEnabled(false);
        service.update(1L, request);
        assertEquals(Integer.valueOf(201), existing.getResponseStatus());
        assertEquals(false, existing.getEnabled());
        assertNull(existing.getCreatedAt());
    }

    @Test
    void malformedImportIsRejectedBeforeSavingAnything() {
        assertThrows(RuleValidationException.class, () -> service.importRules("not-json"));
        assertThrows(RuleValidationException.class, () -> service.importRules("[null]"));
        verify(repository, never()).save(any(MockRule.class));
    }

    @Test
    void legacyImportUsesDefaultsForOmittedRuleSettings() {
        String json = "[{\"name\":\"legacy\",\"url\":\"/api/legacy\","
                + "\"matchType\":\"URL\",\"responseBody\":\"{}\"}]";
        assertEquals(1, service.importRules(json));
        ArgumentCaptor<List<MockRule>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        MockRule imported = captor.getValue().get(0);
        assertEquals(Integer.valueOf(200), imported.getResponseStatus());
        assertEquals(true, imported.getEnabled());
        assertEquals(Integer.valueOf(0), imported.getPriority());
    }

    @Test
    void missingRuleOperationsReturnNotFoundSemantics() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        when(repository.existsById(99L)).thenReturn(false);
        assertThrows(RuleNotFoundException.class, () -> service.getById(99L));
        assertThrows(RuleNotFoundException.class, () -> service.delete(99L));
    }

    private MockRule existing() {
        MockRule rule = new MockRule();
        rule.setId(1L);
        rule.setName("old");
        return rule;
    }

    private MockRuleRequest request() {
        MockRuleRequest request = new MockRuleRequest();
        request.setName("new");
        request.setUrl("/api/items/{id}");
        request.setMatchType(MatchType.URL);
        request.setResponseBody("{}");
        request.setResponseStatus(200);
        request.setEnabled(true);
        request.setPriority(0);
        return request;
    }

    private void assertNull(Object value) {
        assertEquals(null, value);
    }
}
