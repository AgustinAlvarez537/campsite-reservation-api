package com.nitzer.campsitereservation.entities.contraints;

import java.time.LocalDate;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.nitzer.campsitereservation.entities.Reservation;

public class ValidReservationDateIntervalValidator
		implements ConstraintValidator<ValidReservationDateInterval, Reservation> {

	@Override
	public boolean isValid(Reservation value, ConstraintValidatorContext context) {
		if (value == null || value.getCheckInDate() == null || value.getCheckOutDate() == null || value.getCheckInDate() == null || value.getCheckOutDate() == null) {
			return true;
		}

		LocalDate today = LocalDate.now();
		LocalDate todayPlus1Month = LocalDate.now().plusMonths(1);

		return today.isBefore(value.getCheckInDate()) && value.getArrivalDate().isBefore(value.getCheckInDate())
				&& value.getCheckInDate().isBefore(todayPlus1Month);
	}

}
