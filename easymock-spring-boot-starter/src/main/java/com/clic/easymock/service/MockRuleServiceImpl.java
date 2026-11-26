package com.clic.easymock.service;

import com.clic.easymock.model.MockRule;
import com.clic.easymock.repository.MockRuleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional("easymockTransactionManager")
public class MockRuleServiceImpl implements MockRuleService {

    private final MockRuleRepository repository;
    private final ObjectMapper objectMapper;

    public MockRuleServiceImpl(MockRuleRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    @Transactional(value = "easymockTransactionManager", readOnly = true)
    public List<MockRule> listAll() {
        return repository.findAllByOrderByUrlAscPriorityDesc();
    }

    @Override
    @Transactional(value = "easymockTransactionManager", readOnly = true)
    public List<MockRule> listByUrl(String url) {
        return repository.findByUrlAndEnabledTrueOrderByPriorityDesc(url);
    }

    @Override
    @Transactional(value = "easymockTransactionManager", readOnly = true)
    public MockRule getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mock rule not found: " + id));
    }

    @Override
    public MockRule create(MockRule rule) {
        return repository.save(rule);
    }

    @Override
    public MockRule update(Long id, MockRule rule) {
        MockRule existing = getById(id);
        existing.setName(rule.getName());
        existing.setUrl(rule.getUrl());
        existing.setMatchType(rule.getMatchType());
        existing.setParamConditions(rule.getParamConditions());
        existing.setResponseBody(rule.getResponseBody());
        existing.setResponseStatus(rule.getResponseStatus());
        existing.setEnabled(rule.getEnabled());
        existing.setPriority(rule.getPriority());
        existing.setDescription(rule.getDescription());
        return repository.saveAndFlush(existing);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public void toggleEnabled(Long id) {
        MockRule rule = getById(id);
        rule.setEnabled(!rule.getEnabled());
        repository.save(rule);
    }

    @Override
    @Transactional(value = "easymockTransactionManager", readOnly = true)
    public String exportAll() {
        List<MockRule> rules = repository.findAll();
        return toExportJson(rules);
    }

    @Override
    @Transactional(value = "easymockTransactionManager", readOnly = true)
    public String exportByUrl(String url) {
        List<MockRule> rules = repository.findByUrlAndEnabledTrueOrderByPriorityDesc(url);
        return toExportJson(rules);
    }

    @Override
    public int importRules(String json) {
        try {
            List<Map<String, Object>> ruleDataList = objectMapper.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {});
            int count = 0;
            for (Map<String, Object> ruleData : ruleDataList) {
                MockRule rule = new MockRule();
                rule.setName((String) ruleData.get("name"));
                rule.setUrl((String) ruleData.get("url"));
                rule.setMatchType(
                        com.clic.easymock.model.MatchType.valueOf((String) ruleData.get("matchType")));
                rule.setParamConditions((String) ruleData.getOrDefault("paramConditions", null));
                rule.setResponseBody((String) ruleData.get("responseBody"));
                rule.setResponseStatus((Integer) ruleData.getOrDefault("responseStatus", 200));
                rule.setEnabled((Boolean) ruleData.getOrDefault("enabled", true));
                rule.setPriority((Integer) ruleData.getOrDefault("priority", 0));
                rule.setDescription((String) ruleData.getOrDefault("description", null));
                repository.save(rule);
                count++;
            }
            return count;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse import JSON: " + e.getMessage(), e);
        }
    }

    private String toExportJson(List<MockRule> rules) {
        try {
            List<Map<String, Object>> exportList = new ArrayList<>();
            for (MockRule rule : rules) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", rule.getName());
                map.put("url", rule.getUrl());
                map.put("matchType", rule.getMatchType().name());
                map.put("paramConditions", rule.getParamConditions());
                map.put("responseBody", rule.getResponseBody());
                map.put("responseStatus", rule.getResponseStatus());
                map.put("enabled", rule.getEnabled());
                map.put("priority", rule.getPriority());
                map.put("description", rule.getDescription());
                exportList.add(map);
            }
            return objectMapper.writeValueAsString(exportList);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to export rules: " + e.getMessage(), e);
        }
    }
}
