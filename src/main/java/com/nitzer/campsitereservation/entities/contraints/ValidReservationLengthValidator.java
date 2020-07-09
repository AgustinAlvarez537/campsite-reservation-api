package com.nitzer.campsitereservation.entities.contraints;

import java.time.temporal.ChronoUnit;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.nitzer.campsitereservation.entities.Reservation;

public class ValidReservationLengthValidator implements ConstraintValidator<ValidReservationLength, Reservation> {

	private final Long MAX_LENGTH = 3L;
	
	@Override
	public boolean isValid(Reservation value, ConstraintValidatorContext context) {
		if(value == null || value.getCheckInDate() == null || value.getCheckOutDate() == null) {
			return true;
		}

		return ChronoUnit.DAYS.between(value.getCheckInDate(),value.getCheckOutDate()) <= MAX_LENGTH;
	}

}
