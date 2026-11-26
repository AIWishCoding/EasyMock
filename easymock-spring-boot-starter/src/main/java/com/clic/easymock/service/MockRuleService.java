package com.clic.easymock.service;

import com.clic.easymock.model.MockRule;

import java.util.List;

public interface MockRuleService {

    List<MockRule> listAll();

    List<MockRule> listByUrl(String url);

    MockRule getById(Long id);

    MockRule create(MockRule rule);

    MockRule update(Long id, MockRule rule);

    void delete(Long id);

    void toggleEnabled(Long id);

    String exportAll();

    String exportByUrl(String url);

    int importRules(String json);
}
