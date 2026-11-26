package com.clic.easymock.model;

public class ParamCondition {

    private String path;
    private String operator;
    private String value;

    public ParamCondition() {
    }

    public ParamCondition(String path, String operator, String value) {
        this.path = path;
        this.operator = operator;
        this.value = value;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
