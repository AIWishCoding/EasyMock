package com.aiwishcoding.easymock.autoconfigure;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Arrays;
import java.util.List;

public class EasyMockProfileCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment environment = context.getEnvironment();
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return false;
        }
        List<String> allowedProfiles = Binder.get(environment)
                .bind("easymock.allowed-profiles", Bindable.listOf(String.class))
                .orElse(Arrays.asList("local", "dev"));
        return Arrays.stream(activeProfiles).anyMatch(allowedProfiles::contains);
    }
}
