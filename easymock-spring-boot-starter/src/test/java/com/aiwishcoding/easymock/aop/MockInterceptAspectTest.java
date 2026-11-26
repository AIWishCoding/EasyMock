package com.aiwishcoding.easymock.aop;

import com.aiwishcoding.easymock.autoconfigure.EasyMockProperties;
import com.aiwishcoding.easymock.model.MatchResult;
import com.aiwishcoding.easymock.service.MockRuleMatcher;
import tools.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MockInterceptAspectTest {

    private MockRuleMatcher matcher;
    private EasyMockProperties properties;
    private Environment environment;
    private ProceedingJoinPoint joinPoint;
    private MethodSignature signature;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() throws Exception {
        matcher = mock(MockRuleMatcher.class);
        properties = new EasyMockProperties();
        environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        joinPoint = mock(ProceedingJoinPoint.class);
        signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        request = new MockHttpServletRequest("GET", "/api/items/42");
        response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void noProfileBypassesMock() throws Throwable {
        when(environment.getActiveProfiles()).thenReturn(new String[0]);
        MockInterceptAspect aspect = aspect();
        assertNull(aspect.aroundGetMapping(joinPoint));
        verify(joinPoint).proceed();
        verify(matcher, never()).findMatch(anyString(), anyString());
    }

    @Test
    void stripsContextPathBeforeMatching() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/items/42");
        request.setContextPath("/app");
        request.setRequestURI("/app/api/items/42");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        when(signature.getMethod()).thenReturn(Endpoints.class.getMethod("map"));
        when(matcher.findMatch("/api/items/42", null))
                .thenReturn(Optional.of(new MatchResult("{\"ok\":true}", 201, "rule", null)));
        assertTrue(aspect().aroundGetMapping(joinPoint) instanceof Map);
        new MockStatusInterceptor().postHandle(request, response, null, null);
        assertEquals(201, response.getStatus());
        verify(matcher).findMatch("/api/items/42", null);
    }

    @Test
    void appliesStatusToOrdinaryControllerReturnValues() throws Throwable {
        when(signature.getMethod()).thenReturn(Endpoints.class.getMethod("map"));
        when(matcher.findMatch("/api/items/42", null))
                .thenReturn(Optional.of(new MatchResult("{\"ok\":true}", 202, "rule", null)));
        Object value = aspect().aroundGetMapping(joinPoint);
        new MockStatusInterceptor().postHandle(request, response, null, null);
        assertEquals(202, response.getStatus());
        assertEquals(true, ((Map<?, ?>) value).get("ok"));
    }

    @Test
    void supportsParameterizedResponseEntity() throws Throwable {
        when(signature.getMethod()).thenReturn(Endpoints.class.getMethod("response"));
        when(matcher.findMatch("/api/items/42", null))
                .thenReturn(Optional.of(new MatchResult("{\"ok\":true}", 418, "rule", null)));
        ResponseEntity<?> value = (ResponseEntity<?>) aspect().aroundGetMapping(joinPoint);
        assertEquals(418, value.getStatusCode().value());
        assertEquals(true, ((Map<?, ?>) value.getBody()).get("ok"));
    }

    @Test
    void invalidMockNeverFallsBackToRealBusinessMethod() throws Throwable {
        when(signature.getMethod()).thenReturn(Endpoints.class.getMethod("map"));
        when(matcher.findMatch("/api/items/42", null))
                .thenReturn(Optional.of(new MatchResult("not-json", 200, "broken", null)));
        assertThrows(IllegalStateException.class, () -> aspect().aroundGetMapping(joinPoint));
        verify(joinPoint, never()).proceed();
    }

    @Test
    void managementEndpointsAreNotIntercepted() throws Throwable {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/easymock/api/rules");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));
        when(signature.getMethod()).thenReturn(Endpoints.class.getMethod("map"));
        assertNull(aspect().aroundGetMapping(joinPoint));
        verify(joinPoint).proceed();
        verify(matcher, never()).findMatch(anyString(), anyString());
    }

    @Test
    void managementPathsWithPathParametersAreNotIntercepted() throws Throwable {
        MockHttpServletRequest semicolonRequest = new MockHttpServletRequest("GET", "/easymock/api;x/rules");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(semicolonRequest, response));
        when(signature.getMethod()).thenReturn(Endpoints.class.getMethod("map"));
        assertNull(aspect().aroundGetMapping(joinPoint));
        verify(joinPoint).proceed();
        verify(matcher, never()).findMatch(anyString(), anyString());
    }

    private MockInterceptAspect aspect() {
        return new MockInterceptAspect(matcher, properties, environment, new ObjectMapper());
    }

    public static class Endpoints {
        public Map<String, Object> map() {
            return Collections.emptyMap();
        }

        public ResponseEntity<Map<String, Object>> response() {
            return ResponseEntity.ok(Collections.emptyMap());
        }
    }
}
