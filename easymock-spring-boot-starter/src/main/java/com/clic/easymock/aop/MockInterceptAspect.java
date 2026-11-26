package com.clic.easymock.aop;

import com.clic.easymock.annotation.MockApi;
import com.clic.easymock.autoconfigure.EasyMockProperties;
import com.clic.easymock.model.MatchResult;
import com.clic.easymock.service.MockRuleMatcher;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;

@Aspect
@Component
public class MockInterceptAspect {

    private static final Logger log = LoggerFactory.getLogger(MockInterceptAspect.class);

    private final MockRuleMatcher ruleMatcher;
    private final EasyMockProperties properties;
    private final ObjectMapper objectMapper;
    private final boolean profileAllowed;

    public MockInterceptAspect(MockRuleMatcher ruleMatcher,
                               EasyMockProperties properties,
                               Environment environment) {
        this.ruleMatcher = ruleMatcher;
        this.properties = properties;
        this.objectMapper = new ObjectMapper();

        String[] activeProfiles = environment.getActiveProfiles();
        this.profileAllowed = activeProfiles.length == 0 ||
                Arrays.stream(activeProfiles)
                        .anyMatch(p -> properties.getAllowedProfiles().contains(p));

        if (this.profileAllowed) {
            log.info("EasyMock: Aspect is ACTIVE for profiles: {}",
                    Arrays.toString(activeProfiles));
        } else {
            log.info("EasyMock: Aspect is BYPASSED. Active profiles {} not in allowed list {}",
                    Arrays.toString(activeProfiles), properties.getAllowedProfiles());
        }
    }

    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping)")
    public void postMappingPointcut() {}

    @Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping)")
    public void getMappingPointcut() {}

    @Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public void requestMappingPointcut() {}

    @Pointcut("@annotation(com.clic.easymock.annotation.MockApi)")
    public void mockApiPointcut() {}

    @Around("postMappingPointcut()")
    public Object aroundPostMapping(ProceedingJoinPoint joinPoint) throws Throwable {
        return handleMockWithUrl(joinPoint, null);
    }

    @Around("getMappingPointcut()")
    public Object aroundGetMapping(ProceedingJoinPoint joinPoint) throws Throwable {
        return handleMockWithUrl(joinPoint, null);
    }

    @Around("requestMappingPointcut()")
    public Object aroundRequestMapping(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequestMapping rm = method.getAnnotation(RequestMapping.class);
        if (rm != null) {
            RequestMethod[] methods = rm.method();
            if (methods.length > 0) {
                boolean hasGetOrPost = Arrays.asList(methods).contains(RequestMethod.POST)
                        || Arrays.asList(methods).contains(RequestMethod.GET);
                if (!hasGetOrPost) {
                    return joinPoint.proceed();
                }
            }
            // If no method specified, treat as all methods -> intercept
        }
        return handleMockWithUrl(joinPoint, null);
    }

    @Around("mockApiPointcut()")
    public Object aroundMockApi(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        MockApi mockApi = method.getAnnotation(MockApi.class);
        String annotationUrl = (mockApi != null) ? mockApi.value() : null;
        return handleMockWithUrl(joinPoint, annotationUrl);
    }

    /**
     * Core mock handling logic.
     * @param annotationUrl URL from annotation value (for @MockApi, @Path). Can be null.
     */
    Object handleMockWithUrl(ProceedingJoinPoint joinPoint, String annotationUrl) throws Throwable {
        if (!profileAllowed) {
            return joinPoint.proceed();
        }

        String requestUri;

        // For @MockApi / @Path, annotation URL takes priority for rule matching
        if (annotationUrl != null && !annotationUrl.isEmpty()) {
            requestUri = annotationUrl;
        } else {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                requestUri = request.getRequestURI();

                String pagePath = properties.getPagePath();
                String apiPath = properties.getApiPath();
                if (requestUri.startsWith(pagePath) || requestUri.startsWith(apiPath)) {
                    return joinPoint.proceed();
                }
            } else {
                return joinPoint.proceed();
            }
        }

        String bodyJson = extractRequestBody(joinPoint);

        Optional<MatchResult> matchOpt = ruleMatcher.findMatch(requestUri, bodyJson);
        if (!matchOpt.isPresent()) {
            return joinPoint.proceed();
        }

        MatchResult matchResult = matchOpt.get();
        if (matchResult.getGroupName() != null) {
            log.info("EasyMock: Intercepted '{}', returning mock from rule '{}' -> group '{}'",
                    requestUri, matchResult.getRuleName(), matchResult.getGroupName());
        } else {
            log.info("EasyMock: Intercepted '{}', returning mock response from rule '{}'",
                    requestUri, matchResult.getRuleName());
        }

        return buildReturnValue(joinPoint, matchResult);
    }

    private String extractRequestBody(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        Object[] args = joinPoint.getArgs();

        // Priority 1: @RequestBody annotated parameter
        for (int i = 0; i < paramAnnotations.length; i++) {
            for (Annotation annotation : paramAnnotations[i]) {
                if (annotation instanceof RequestBody && args[i] != null) {
                    try {
                        return objectMapper.writeValueAsString(args[i]);
                    } catch (Exception e) {
                        log.warn("EasyMock: Failed to serialize @RequestBody arg: {}",
                                e.getMessage());
                        return null;
                    }
                }
            }
        }

        // Priority 2: First non-framework argument (fallback for @MockApi, JAX-RS methods)
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null && !isFrameworkType(args[i])) {
                try {
                    return objectMapper.writeValueAsString(args[i]);
                } catch (Exception e) {
                    // skip this argument
                }
            }
        }

        return null;
    }

    private boolean isFrameworkType(Object obj) {
        if (obj instanceof HttpServletRequest || obj instanceof HttpServletResponse) {
            return true;
        }
        String className = obj.getClass().getName();
        return className.startsWith("javax.servlet.")
                || className.startsWith("org.springframework.");
    }

    private Object buildReturnValue(ProceedingJoinPoint joinPoint, MatchResult matchResult) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> returnType = method.getReturnType();
        String responseBody = matchResult.getResponseBody();
        int responseStatus = matchResult.getResponseStatus() != null ? matchResult.getResponseStatus() : 200;

        try {
            if (void.class.equals(returnType) || Void.class.equals(returnType)) {
                return null;
            }

            if (ResponseEntity.class.isAssignableFrom(returnType)) {
                Type genericReturnType = method.getGenericReturnType();
                if (genericReturnType instanceof ParameterizedType) {
                    Type actualType = ((ParameterizedType) genericReturnType)
                            .getActualTypeArguments()[0];
                    JavaType javaType = objectMapper.getTypeFactory()
                            .constructType(actualType);
                    Object body = objectMapper.readValue(responseBody, javaType);
                    return ResponseEntity.status(responseStatus).body(body);
                }
                return ResponseEntity.status(responseStatus).body(responseBody);
            }

            if (String.class.equals(returnType)) {
                return responseBody;
            }

            return objectMapper.readValue(responseBody, returnType);
        } catch (Exception e) {
            log.error("EasyMock: Failed to build return value for rule '{}': {}",
                    matchResult.getRuleName(), e.getMessage());
            try {
                return joinPoint.proceed();
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }
}
