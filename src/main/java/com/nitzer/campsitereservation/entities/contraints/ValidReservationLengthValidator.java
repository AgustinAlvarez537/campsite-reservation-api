package com.nitzer.campsitereservation.entities.contraints;

import java.time.temporal.ChronoUnit;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.nitzer.campsitereservation.entities.Reservation;

public class ValidReservationLengthValidator implements ConstraintValidator<ValidReservationLength, Reservation> {

	private final Long MAX_LENGTH = 3L;
	
	@Override
	public boolean isValid(Reservation value, ConstraintValidatorContext context) {
		if(value == null || value.getArrivalDate() == null || value.getDepartureDate() == null) {
			return true;
		}

		return ChronoUnit.DAYS.between(value.getArrivalDate(),value.getDepartureDate()) <= MAX_LENGTH;
	}

}
