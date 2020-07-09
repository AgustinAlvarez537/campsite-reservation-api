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
@Constraint(validatedBy = { ValidReservationDateIntervalValidator.class })
public @interface ValidReservationDateInterval {
	String message() default "Invalid dates selected (must be 1 day ahead of arrival and up to 1 month in advance, also check-out date must be lower or equals than departure date)";
	Class<?>[] groups() default { };
	Class<? extends Payload>[] payload() default { };
}
