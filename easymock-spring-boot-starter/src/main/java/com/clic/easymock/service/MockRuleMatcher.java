package com.clic.easymock.service;

import com.clic.easymock.model.ConditionGroup;
import com.clic.easymock.model.MatchResult;
import com.clic.easymock.model.MatchType;
import com.clic.easymock.model.MockRule;
import com.clic.easymock.repository.MockRuleRepository;
import com.clic.easymock.util.JsonPathEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class MockRuleMatcher {

    private static final Logger log = LoggerFactory.getLogger(MockRuleMatcher.class);

    private final MockRuleRepository repository;
    private final JsonPathEvaluator jsonPathEvaluator;

    public MockRuleMatcher(MockRuleRepository repository, JsonPathEvaluator jsonPathEvaluator) {
        this.repository = repository;
        this.jsonPathEvaluator = jsonPathEvaluator;
    }

    public Optional<MatchResult> findMatch(String url, String requestBodyJson) {
        List<MockRule> rules = repository.findByUrlAndEnabledTrueOrderByPriorityDesc(url);
        if (rules.isEmpty()) {
            return Optional.empty();
        }

        MatchResult urlLevelMatch = null;

        for (MockRule rule : rules) {
            if (rule.getMatchType() == MatchType.PARAMETER) {
                Optional<ConditionGroup> groupOpt =
                        jsonPathEvaluator.evaluateGroups(requestBodyJson, rule.getParamConditions());
                if (groupOpt.isPresent()) {
                    MatchResult result = MatchResult.fromGroup(rule, groupOpt.get());
                    if (result.getGroupName() != null) {
                        log.debug("EasyMock: Parameter-level match for URL '{}', rule '{}' -> group '{}'",
                                url, rule.getName(), result.getGroupName());
                    } else {
                        log.debug("EasyMock: Parameter-level match for URL '{}', rule '{}'",
                                url, rule.getName());
                    }
                    return Optional.of(result);
                }
            } else if (rule.getMatchType() == MatchType.URL && urlLevelMatch == null) {
                urlLevelMatch = MatchResult.fromRule(rule);
            }
        }

        if (urlLevelMatch != null) {
            log.debug("EasyMock: URL-level match found for URL '{}', rule '{}'",
                    url, urlLevelMatch.getRuleName());
            return Optional.of(urlLevelMatch);
        }

        return Optional.empty();
    }
}
