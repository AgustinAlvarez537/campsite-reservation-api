package com.nitzer.campsitereservation.entities.contraints;

import java.time.LocalDate;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.nitzer.campsitereservation.entities.Reservation;

public class ValidReservationDateIntervalValidator implements ConstraintValidator<ValidReservationDateInterval, Reservation> {

	@Override
	public boolean isValid(Reservation value, ConstraintValidatorContext context) {
		if(value == null || value.getArrivalDate() == null || value.getDepartureDate() == null) {
			return true;
		}
		
		LocalDate today = LocalDate.now();
		LocalDate todayPlus1Month = LocalDate.now().plusMonths(1);
		
		return today.isBefore(value.getArrivalDate()) && value.getArrivalDate().isBefore(todayPlus1Month);
	}

}
