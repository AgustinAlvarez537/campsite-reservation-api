package com.nitzer.campsitereservation.controllers;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.nitzer.campsitereservation.entities.Reservation;
import com.nitzer.campsitereservation.services.ReservationService;

import lombok.AllArgsConstructor;


@AllArgsConstructor
@RestController
public class ReservationController {
	
	private ReservationService service;
	
	@GetMapping("/")
	public String home(){
		return "Welcome to campsite reservation API";
	}
	
	@GetMapping("/reservation/available")
	public List<LocalDate> getAvailableDates(@RequestParam("dateFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> dateFrom, @RequestParam("dateTo") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> dateTo){
		if(dateFrom.isEmpty()) {
			dateFrom = Optional.of(LocalDate.now());
		}
		
		if(dateTo.isEmpty()) {
			dateTo = Optional.of(dateFrom.get().plusMonths(1));
		}
		
		return this.service.getAvailableDates(dateFrom.get(), dateTo.get());
	}
	
	@PostMapping("/reservation")
	public ResponseEntity<Reservation> reserve(@Valid @RequestBody Reservation reservation) throws RuntimeException {
		Reservation newReservation = this.service.reserve(reservation);

		URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
		          .path("/reservation/{id}")
		          .buildAndExpand(newReservation.getId())
		          .toUri();
		
		return ResponseEntity.created(uri).body(newReservation);
	}
	

	@PutMapping("/reservation/{id}")
	public ResponseEntity<Reservation> updateBooking(@Valid @RequestBody Reservation booking, @PathVariable Long id) throws RuntimeException {
		return ResponseEntity.ok(this.service.update(booking,id));
	}
	
	@DeleteMapping("/reservation/{id}")
	public void cancel(@PathVariable Long id) throws RuntimeException {
		this.service.cancel(id);
	}
}
