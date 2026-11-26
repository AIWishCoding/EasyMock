package com.clic.easymock.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to mark a method as mockable by EasyMock.
 * The value specifies the URL path used for mock rule matching.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MockApi {

    /**
     * The URL path used for mock rule matching.
     * Example: "/api/order/create"
     */
    String value();
}
