package com.aiwishcoding.easymock.service;

import com.aiwishcoding.easymock.dto.MockRuleRequest;
import com.aiwishcoding.easymock.model.MockRule;

import java.util.List;

public interface MockRuleService {

    List<MockRule> listAll();

    List<MockRule> listByUrl(String url);

    MockRule getById(Long id);

    MockRule create(MockRuleRequest rule);

    MockRule update(Long id, MockRuleRequest rule);

    void delete(Long id);

    void toggleEnabled(Long id);

    String exportAll();

    String exportByUrl(String url);

    int importRules(String json);
}
