package com.lognex.api.clients.endpoints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Техническая аннотация, ставящаяся над конечным звеном запроса (например в <code>entity().counterparty().get()</code>
 * метод <code>get()</code> должен быть отмечен этой аннотацией)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ApiEndpoint {
}
