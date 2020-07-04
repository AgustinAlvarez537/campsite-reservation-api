package com.nitzer.campsitereservation.entities.contraints;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { ValidReservationLengthValidator.class })
public @interface ValidReservationLength {
	String message() default "Max reservation length exceeded";
	Class<?>[] groups() default { };
	Class<? extends Payload>[] payload() default { };
}
