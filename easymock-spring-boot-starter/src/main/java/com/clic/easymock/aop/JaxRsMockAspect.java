package com.clic.easymock.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import javax.ws.rs.Path;
import java.lang.reflect.Method;

/**
 * Aspect for intercepting JAX-RS @Path annotated methods.
 * Only loaded when javax.ws.rs.Path is on the classpath.
 */
@Aspect
@Component
@ConditionalOnClass(name = "javax.ws.rs.Path")
public class JaxRsMockAspect {

    private static final Logger log = LoggerFactory.getLogger(JaxRsMockAspect.class);

    private final MockInterceptAspect mockInterceptAspect;

    public JaxRsMockAspect(MockInterceptAspect mockInterceptAspect) {
        this.mockInterceptAspect = mockInterceptAspect;
        log.info("EasyMock: JAX-RS @Path support is enabled");
    }

    @Pointcut("@annotation(javax.ws.rs.Path)")
    public void jaxrsPathPointcut() {}

    @Around("jaxrsPathPointcut()")
    public Object aroundJaxrsPath(ProceedingJoinPoint joinPoint) throws Throwable {
        String fullPath = resolveJaxrsPath(joinPoint);
        return mockInterceptAspect.handleMockWithUrl(joinPoint, fullPath);
    }

    /**
     * Resolve the full JAX-RS path by combining class-level and method-level @Path values.
     */
    private String resolveJaxrsPath(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // Method-level @Path
        Path methodPath = method.getAnnotation(Path.class);
        String methodPathValue = (methodPath != null) ? methodPath.value() : "";

        // Class-level @Path
        Class<?> targetClass = joinPoint.getTarget().getClass();
        Path classPath = targetClass.getAnnotation(Path.class);
        String classPathValue = (classPath != null) ? classPath.value() : "";

        // Combine paths
        String fullPath = normalizePath(classPathValue) + normalizePath(methodPathValue);
        return fullPath.isEmpty() ? "/" : fullPath;
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}
