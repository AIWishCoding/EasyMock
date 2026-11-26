package com.aiwishcoding.easymock.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EasyMockProfileConditionTest {

    private final EasyMockProfileCondition condition = new EasyMockProfileCondition();

    @Test
    void noActiveProfileFailsClosed() {
        assertFalse(matches(new String[0], "local,dev"));
    }

    @Test
    void onlyExplicitlyAllowedProfilesActivateEasyMock() {
        assertTrue(matches(new String[]{"local"}, "local,dev"));
        assertTrue(matches(new String[]{"test", "dev"}, "local,dev"));
        assertFalse(matches(new String[]{"prod"}, "local,dev"));
        assertFalse(matches(new String[]{"dev"}, ""));
    }

    private boolean matches(String[] activeProfiles, String allowedProfiles) {
        StandardEnvironment environment = new StandardEnvironment();
        TestPropertyValues.of("easymock.allowed-profiles=" + allowedProfiles).applyTo(environment);
        environment.setActiveProfiles(activeProfiles);
        ConditionContext context = mock(ConditionContext.class);
        when(context.getEnvironment()).thenReturn(environment);
        return condition.matches(context, mock(AnnotatedTypeMetadata.class));
    }
}
