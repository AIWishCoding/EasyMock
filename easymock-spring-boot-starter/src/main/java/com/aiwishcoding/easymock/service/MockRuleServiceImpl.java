package com.aiwishcoding.easymock.service;

import com.aiwishcoding.easymock.dto.MockRuleRequest;
import com.aiwishcoding.easymock.exception.RuleNotFoundException;
import com.aiwishcoding.easymock.exception.RuleValidationException;
import com.aiwishcoding.easymock.model.MockRule;
import com.aiwishcoding.easymock.repository.MockRuleRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional("easymockTransactionManager")
public class MockRuleServiceImpl implements MockRuleService {

    private final MockRuleRepository repository;
    private final RuleValidator ruleValidator;
    private final ObjectMapper objectMapper;

    public MockRuleServiceImpl(MockRuleRepository repository, RuleValidator ruleValidator) {
        this.repository = repository;
        this.ruleValidator = ruleValidator;
        this.objectMapper = new ObjectMapper();
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
                .orElseThrow(() -> new RuleNotFoundException(id));
    }

    @Override
    public MockRule create(MockRuleRequest request) {
        ruleValidator.validate(request);
        return repository.save(toRule(request));
    }

    @Override
    public MockRule update(Long id, MockRuleRequest request) {
        ruleValidator.validate(request);
        MockRule existing = getById(id);
        existing.setName(request.getName());
        existing.setUrl(request.getUrl());
        existing.setMatchType(request.getMatchType());
        existing.setParamConditions(request.getParamConditions());
        existing.setResponseBody(request.getResponseBody());
        existing.setResponseStatus(request.getResponseStatus());
        existing.setEnabled(request.getEnabled());
        existing.setPriority(request.getPriority());
        existing.setDescription(request.getDescription());
        return repository.saveAndFlush(existing);
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuleNotFoundException(id);
        }
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
            JsonNode tree = objectMapper.readTree(json);
            if (tree == null || !tree.isArray() || tree.size() == 0) {
                throw new RuleValidationException("导入文件必须包含至少一条规则");
            }
            for (JsonNode node : tree) {
                if (!node.isObject()) {
                    throw new RuleValidationException("导入文件必须是合法 JSON 数组");
                }
                if (!node.hasNonNull("responseStatus")) {
                    ((tools.jackson.databind.node.ObjectNode) node).put("responseStatus", 200);
                }
                if (!node.hasNonNull("enabled")) {
                    ((tools.jackson.databind.node.ObjectNode) node).put("enabled", true);
                }
                if (!node.hasNonNull("priority")) {
                    ((tools.jackson.databind.node.ObjectNode) node).put("priority", 0);
                }
            }
            JavaType requestListType = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, MockRuleRequest.class);
            List<MockRuleRequest> requests = objectMapper.convertValue(tree, requestListType);
            if (requests.size() > 1000) {
                throw new RuleValidationException("单次最多导入 1000 条规则");
            }
            for (MockRuleRequest request : requests) {
                ruleValidator.validate(request);
            }
            List<MockRule> rules = requests.stream().map(this::toRule)
                    .collect(Collectors.toList());
            repository.saveAll(rules);
            return rules.size();
        } catch (JacksonException | IllegalArgumentException e) {
            throw new RuleValidationException("导入文件必须是合法 JSON 数组", e);
        }
    }

    private MockRule toRule(MockRuleRequest request) {
        MockRule rule = new MockRule();
        rule.setName(request.getName());
        rule.setUrl(request.getUrl());
        rule.setMatchType(request.getMatchType());
        rule.setParamConditions(request.getParamConditions());
        rule.setResponseBody(request.getResponseBody());
        rule.setResponseStatus(request.getResponseStatus());
        rule.setEnabled(request.getEnabled());
        rule.setPriority(request.getPriority());
        rule.setDescription(request.getDescription());
        return rule;
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
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportList);
        } catch (JacksonException e) {
            throw new RuntimeException("Failed to export rules: " + e.getMessage(), e);
        }
    }
}
