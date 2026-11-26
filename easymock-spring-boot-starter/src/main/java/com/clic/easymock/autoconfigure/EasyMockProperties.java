package com.clic.easymock.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "easymock")
public class EasyMockProperties {

    private boolean enabled = false;

    private List<String> allowedProfiles = Arrays.asList("local", "dev");

    private String dbPath = "./data/easymock";

    private String pagePath = "/easymock";

    private String apiPath = "/easymock/api";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getAllowedProfiles() {
        return allowedProfiles;
    }

    public void setAllowedProfiles(List<String> allowedProfiles) {
        this.allowedProfiles = allowedProfiles;
    }

    public String getDbPath() {
        return dbPath;
    }

    public void setDbPath(String dbPath) {
        this.dbPath = dbPath;
    }

    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    public String getApiPath() {
        return apiPath;
    }

    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }
}
