package de.schaffbar.core_pos.shared.cqrs;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.transaction.annotation.Transactional;

/**
 * A public method of a service, use case or read facade that only reads. Not Spring Data's {@code @Query} – that one
 * belongs to repositories.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Transactional(readOnly = true)
public @interface Query {}
