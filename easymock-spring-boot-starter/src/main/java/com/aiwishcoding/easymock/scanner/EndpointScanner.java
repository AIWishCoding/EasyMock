package com.aiwishcoding.easymock.scanner;

import com.aiwishcoding.easymock.annotation.MockApi;
import com.aiwishcoding.easymock.autoconfigure.EasyMockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Component
public class EndpointScanner {

    private static final Logger log = LoggerFactory.getLogger(EndpointScanner.class);

    private final RequestMappingHandlerMapping handlerMapping;
    private final EasyMockProperties properties;
    private final ApplicationContext applicationContext;

    public EndpointScanner(RequestMappingHandlerMapping handlerMapping,
                           EasyMockProperties properties,
                           ApplicationContext applicationContext) {
        this.handlerMapping = handlerMapping;
        this.properties = properties;
        this.applicationContext = applicationContext;
    }

    /**
     * Scan all mockable endpoints: Spring MVC GET/POST + @MockApi + JAX-RS @Path.
     */
    public List<String> scanEndpoints() {
        Set<String> endpoints = new TreeSet<>();

        String pagePath = properties.getPagePath();
        String apiPath = properties.getApiPath();

        // 1. Scan Spring MVC endpoints (GET + POST)
        Map<RequestMappingInfo, HandlerMethod> methods = handlerMapping.getHandlerMethods();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : methods.entrySet()) {
            RequestMappingInfo info = entry.getKey();
            Set<RequestMethod> requestMethods = info.getMethodsCondition().getMethods();

            boolean isGetOrPost = requestMethods.isEmpty()
                    || requestMethods.contains(RequestMethod.GET)
                    || requestMethods.contains(RequestMethod.POST);

            if (isGetOrPost) {
                Set<String> patterns = info.getPatternValues();

                for (String pattern : patterns) {
                    if (!isSameOrChildPath(pattern, pagePath) && !isSameOrChildPath(pattern, apiPath)) {
                        endpoints.add(pattern);
                    }
                }
            }
        }

        // 2. Scan @MockApi annotated methods
        scanMockApiEndpoints(endpoints);

        // 3. Scan JAX-RS @Path annotated methods (conditional)
        scanJaxRsEndpoints(endpoints);

        return new ArrayList<>(endpoints);
    }

    private boolean isSameOrChildPath(String path, String configuredPath) {
        return path.equals(configuredPath) || path.startsWith(configuredPath + "/");
    }

    private void scanMockApiEndpoints(Set<String> endpoints) {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            try {
                Object bean = applicationContext.getBean(beanName);
                Class<?> targetClass = org.springframework.aop.support.AopUtils.getTargetClass(bean);

                for (Method method : targetClass.getDeclaredMethods()) {
                    MockApi mockApi = method.getAnnotation(MockApi.class);
                    if (mockApi != null && !mockApi.value().isEmpty()) {
                        endpoints.add(mockApi.value());
                    }
                }
            } catch (Exception e) {
                // skip beans that can't be introspected
            }
        }
    }

    private void scanJaxRsEndpoints(Set<String> endpoints) {
        Class<? extends Annotation> pathAnnotation;
        try {
            pathAnnotation = (Class<? extends Annotation>)
                    Class.forName("jakarta.ws.rs.Path");
        } catch (ClassNotFoundException e) {
            // JAX-RS not on classpath, skip
            return;
        }

        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            try {
                Class<?> beanType = applicationContext.getType(beanName);
                if (beanType == null) continue;

                // Class-level @Path
                Annotation classPathAnn = beanType.getAnnotation(pathAnnotation);
                String classPath = classPathAnn != null ? getPathValue(classPathAnn) : "";

                for (Method method : beanType.getDeclaredMethods()) {
                    Annotation methodPathAnn = method.getAnnotation(pathAnnotation);
                    if (methodPathAnn != null) {
                        String methodPath = getPathValue(methodPathAnn);
                        String fullPath = normalizePath(classPath) + normalizePath(methodPath);
                        if (!fullPath.isEmpty()) {
                            endpoints.add(fullPath);
                        }
                    }
                }
            } catch (Exception e) {
                // skip
            }
        }
    }

    private String getPathValue(Annotation pathAnnotation) {
        try {
            Method valueMethod = pathAnnotation.annotationType().getMethod("value");
            return (String) valueMethod.invoke(pathAnnotation);
        } catch (Exception e) {
            return "";
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) return "";
        if (!path.startsWith("/")) path = "/" + path;
        if (path.endsWith("/") && path.length() > 1) path = path.substring(0, path.length() - 1);
        return path;
    }
}
