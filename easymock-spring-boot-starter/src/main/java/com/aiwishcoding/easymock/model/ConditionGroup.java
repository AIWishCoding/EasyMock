package com.aiwishcoding.easymock.model;

import java.util.List;

public class ConditionGroup {

    private String name;
    private List<ParamCondition> conditions;
    private String responseBody;

    public ConditionGroup() {
    }

    public ConditionGroup(String name, List<ParamCondition> conditions,
                          String responseBody) {
        this.name = name;
        this.conditions = conditions;
        this.responseBody = responseBody;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ParamCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<ParamCondition> conditions) {
        this.conditions = conditions;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }
}
