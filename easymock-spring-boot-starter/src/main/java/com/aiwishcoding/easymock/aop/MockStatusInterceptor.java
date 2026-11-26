package com.aiwishcoding.easymock.aop;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class MockStatusInterceptor implements HandlerInterceptor {

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, org.springframework.web.servlet.ModelAndView modelAndView) {
        Object status = request.getAttribute(MockInterceptAspect.MOCK_STATUS_ATTRIBUTE);
        if (status instanceof Integer) {
            response.setStatus((Integer) status);
        }
        clearStatus(request);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        clearStatus(request);
    }

    private void clearStatus(HttpServletRequest request) {
        request.removeAttribute(MockInterceptAspect.MOCK_STATUS_ATTRIBUTE);
    }
}
