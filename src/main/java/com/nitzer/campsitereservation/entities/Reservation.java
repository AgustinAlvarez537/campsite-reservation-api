package com.nitzer.campsitereservation.entities;

import java.time.LocalDate;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.validator.constraints.Length;

import com.nitzer.campsitereservation.entities.contraints.ValidReservationDateInterval;
import com.nitzer.campsitereservation.entities.contraints.ValidReservationLength;

import lombok.Data;

@Data
@Entity
@Table(name="bookings")
@ValidReservationLength
@ValidReservationDateInterval
public class Reservation {
	
	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	private UUID id;
	
	@Column(name="check_in_date")
	@NotNull(message = "You must specify a reservation start date")
	private LocalDate checkInDate;

	@Column(name="check_out_date")
	@NotNull(message = "You must specify a reservation end date")
	private LocalDate checkOutDate;
	
	@Column(name = "arrival_date")
	@NotNull(message = "You must specify an arrival date")
	private LocalDate arrivalDate;

	@Column(name = "departure_date")
	@NotNull(message = "You must specify a departure date")
	private LocalDate departureDate;
	
	@NotEmpty(message = "You must specify an email")
	@Email(message = "Invalid email")
	@Length(max = 320)
	private String email;

	@Column(name = "full_name")
	@NotEmpty(message="You must enter your full name")
	@Length(max=100)
	private String fullName;
	
}
