package com.aiwishcoding.easymock.model;

public class MatchResult {

    private String responseBody;
    private Integer responseStatus;
    private String ruleName;
    private String groupName;

    public MatchResult() {
    }

    public MatchResult(String responseBody, Integer responseStatus,
                       String ruleName, String groupName) {
        this.responseBody = responseBody;
        this.responseStatus = responseStatus;
        this.ruleName = ruleName;
        this.groupName = groupName;
    }

    public static MatchResult fromRule(MockRule rule) {
        return new MatchResult(
                rule.getResponseBody(),
                rule.getResponseStatus(),
                rule.getName(),
                null
        );
    }

    public static MatchResult fromGroup(MockRule rule, ConditionGroup group) {
        return new MatchResult(
                group.getResponseBody() != null ? group.getResponseBody() : rule.getResponseBody(),
                rule.getResponseStatus(),
                rule.getName(),
                group.getName()
        );
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
