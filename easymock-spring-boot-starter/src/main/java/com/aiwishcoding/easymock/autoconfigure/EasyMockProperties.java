package com.aiwishcoding.easymock.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "easymock")
public class EasyMockProperties {

    private boolean enabled = false;

    private List<String> allowedProfiles = Arrays.asList("local", "dev");

    private String dbPath = "./data/easymock";

    private String pagePath = "/easymock";

    private String apiPath = "/easymock/api";

    private String adminToken;

    @PostConstruct
    public void validatePaths() {
        validatePath("page-path", pagePath);
        validatePath("api-path", apiPath);
        if (pagePath.equals(apiPath) || pagePath.startsWith(apiPath + "/")) {
            throw new IllegalStateException("easymock.page-path must not be inside easymock.api-path");
        }
    }

    private void validatePath(String name, String path) {
        if (path == null || !path.startsWith("/") || path.length() > 1 && path.endsWith("/")
                || path.contains("*")) {
            throw new IllegalStateException("easymock." + name + " must be a concrete path beginning with /");
        }
    }

    public String getAdminToken() {
        return adminToken;
    }

    public void setAdminToken(String adminToken) {
        this.adminToken = adminToken;
    }

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
