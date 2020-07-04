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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;

@Tag(name = "Reservations", description = "The campsite reservation API")
@AllArgsConstructor
@RestController
public class ReservationController {
	
	private ReservationService service;
	
	@Operation(summary = "Check that API is working")
	@ApiResponses(value = { 
	  @ApiResponse(responseCode = "200", description = "Welcome message", 
	    content = { @Content(mediaType = "text/plain", 
	      schema = @Schema(implementation = String.class)) }),
	  @ApiResponse(responseCode = "400", description = "Not apply",
	    content = @Content),
	  @ApiResponse(responseCode = "404", description = "Not apply",
	    content = @Content),
	  @ApiResponse(responseCode = "409", description = "Not apply",
	    content = @Content)})
	@GetMapping("/")
	public ResponseEntity<String> home(){
		return ResponseEntity.ok("Welcome to campsite reservation API");
	}
	
	@Operation(summary = "Get available dates for reservation")
	@ApiResponses(value = { 
	  @ApiResponse(responseCode = "200", description = "List of available dates", 
	    content = { @Content(mediaType = "application/json", 
	      array = @ArraySchema(schema = @Schema(implementation = LocalDate.class))) }),
	  @ApiResponse(responseCode = "400", description = "Invalid dates specified",
	    content = @Content),
	  @ApiResponse(responseCode = "404", description = "Not apply",
	    content = @Content),
	  @ApiResponse(responseCode = "409", description = "Not apply",
	    content = @Content)})
	@GetMapping("/reservation/available")
	public ResponseEntity<List<?>> getAvailableDates(@Parameter(description = "Date from interval") @RequestParam("dateFrom") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> dateFrom, @Parameter(description = "Date to interval") @RequestParam("dateTo") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> dateTo){
		if(dateFrom.isEmpty()) {
			dateFrom = Optional.of(LocalDate.now());
		}
		
		if(dateTo.isEmpty()) {
			dateTo = Optional.of(dateFrom.get().plusMonths(1));
		}
		
		return ResponseEntity.ok(this.service.getAvailableDates(dateFrom.get(), dateTo.get()));
	}
	
	@Operation(summary = "Make a reservation")
	@ApiResponses(value = { 
	  @ApiResponse(responseCode = "201", description = "Reservation created succcessfully", 
	    content = { @Content(mediaType = "application/json", 
	      schema = @Schema(implementation = Reservation.class)) }),
	  @ApiResponse(responseCode = "400", description = "Fail to valid input",
	    content = @Content),
	  @ApiResponse(responseCode = "404", description = "Not apply",
	    content = @Content),
	  @ApiResponse(responseCode = "409", description = "Another reservation exists in the date interval",
	    content = @Content)})
	@PostMapping("/reservation")
	public ResponseEntity<Reservation> reserve(@Valid @RequestBody Reservation reservation) throws RuntimeException {
		Reservation newReservation = this.service.reserve(reservation);

		URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
		          .path("/reservation/{id}")
		          .buildAndExpand(newReservation.getId())
		          .toUri();
		
		return ResponseEntity.created(uri).body(newReservation);
	}
	
	@Operation(summary = "Modify a reservation")
	@ApiResponses(value = { 
	  @ApiResponse(responseCode = "200", description = "Reservation updated succcessfully", 
	    content = { @Content(mediaType = "application/json", 
	      schema = @Schema(implementation = Reservation.class)) }),
	  @ApiResponse(responseCode = "400", description = "Fail to valid input",
	    content = @Content),
	  @ApiResponse(responseCode = "404", description = "Reservation not exists",
	    content = @Content),
	  @ApiResponse(responseCode = "409", description = "Another reservation exists in the date interval",
	    content = @Content)})
	@PutMapping("/reservation/{id}")
	public ResponseEntity<Reservation> updateBooking(@Valid @RequestBody Reservation booking, @Parameter(description = "Reservation identifier") @PathVariable Long id) throws RuntimeException {
		return ResponseEntity.ok(this.service.update(booking,id));
	}
	
	@Operation(summary = "Cancel a reservation")
	@ApiResponses(value = { 
	  @ApiResponse(responseCode = "200", description = "Reservation canceled succcessfully", 
	    content = { @Content(mediaType = "application/json", 
	      schema = @Schema(implementation = Reservation.class)) }),
	  @ApiResponse(responseCode = "400", description = "Not apply",
	    content = @Content),
	  @ApiResponse(responseCode = "404", description = "Reservation not exists or already canceled",
	    content = @Content),
	  @ApiResponse(responseCode = "409", description = "Not apply",
	    content = @Content)})
	@DeleteMapping("/reservation/{id}")
	public ResponseEntity<Void> cancel(@Parameter(description = "Reservation identifier") @PathVariable Long id) throws RuntimeException {
		this.service.cancel(id);
		
		return ResponseEntity.ok().build();
	}
}
