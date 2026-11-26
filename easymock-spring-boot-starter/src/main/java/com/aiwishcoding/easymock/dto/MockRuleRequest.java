package com.aiwishcoding.easymock.dto;

import com.aiwishcoding.easymock.model.MatchType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class MockRuleRequest {

    @NotBlank
    @Size(max = 256)
    private String name;

    @NotBlank
    @Size(max = 512)
    @Pattern(regexp = "^/.*", message = "路径必须以 / 开头")
    private String url;

    @NotNull
    private MatchType matchType;

    @Size(max = 1048576)
    private String paramConditions;

    @NotBlank
    @Size(max = 1048576)
    private String responseBody;

    @NotNull
    @Min(100)
    @Max(599)
    private Integer responseStatus;

    @NotNull
    private Boolean enabled;

    @NotNull
    @Min(-9999)
    @Max(9999)
    private Integer priority;

    @Size(max = 1024)
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(MatchType matchType) {
        this.matchType = matchType;
    }

    public String getParamConditions() {
        return paramConditions;
    }

    public void setParamConditions(String paramConditions) {
        this.paramConditions = paramConditions;
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
