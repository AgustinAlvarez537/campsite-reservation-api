package com.nitzer.campsitereservation.services;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.nitzer.campsitereservation.entities.Reservation;
import com.nitzer.campsitereservation.exceptions.OverlappingDatesException;
import com.nitzer.campsitereservation.exceptions.ReservationNotFoundException;
import com.nitzer.campsitereservation.repositories.ReservationRepository;

@Service
public class ReservationService {
	
	@Autowired
	private ReservationRepository repository;
	
	public List<LocalDate> getAvailableDates(LocalDate dateFrom, LocalDate dateTo){
		List<Reservation> reservations = this.repository.findByDateInterval(dateFrom, dateTo);
		List<LocalDate> bookedDates = reservations.stream().flatMap(b -> b.getCheckInDate().datesUntil(b.getCheckOutDate())).collect(Collectors.toList());
		List<LocalDate> result = dateFrom.datesUntil(dateTo).filter(d -> !bookedDates.contains(d)).collect(Collectors.toList());
		return result;
	}
	

	@Transactional(isolation=Isolation.SERIALIZABLE, rollbackFor=Exception.class)
	public Reservation reserve(Reservation reservation) throws RuntimeException {
		
		if(!this.repository.existsOverlappingReservation(reservation.getCheckInDate(), reservation.getCheckOutDate(),null)) {
			reservation = this.repository.saveAndFlush(reservation);
		}else{
			throw new OverlappingDatesException(reservation.getCheckInDate(),reservation.getCheckOutDate());
		}
		
		return reservation;
	}

	@Transactional(isolation=Isolation.SERIALIZABLE, rollbackFor=Exception.class)
	public Reservation update(Reservation reservation, UUID id) throws RuntimeException {
		
		Reservation toUpdate = this.repository.findById(id).orElseThrow(() -> new ReservationNotFoundException(id));
		
		toUpdate.setCheckInDate(reservation.getCheckInDate());
		toUpdate.setCheckOutDate(reservation.getCheckOutDate());
		
		if(!this.repository.existsOverlappingReservation(reservation.getCheckInDate(), reservation.getCheckOutDate(),toUpdate.getId())) {
			toUpdate = this.repository.saveAndFlush(toUpdate);
		}else{
			throw new OverlappingDatesException(toUpdate.getCheckInDate(),toUpdate.getCheckOutDate());
		}
		return toUpdate;
	}

	@Transactional(isolation=Isolation.SERIALIZABLE, rollbackFor=Exception.class)
	public void cancel(UUID id) throws RuntimeException {
		Reservation toDelete = this.repository.findById(id).orElseThrow(() -> new ReservationNotFoundException(id));
		this.repository.delete(toDelete);
	}
	
	public Reservation getOne(UUID id) {
		return this.repository.findById(id).orElseThrow(() -> new ReservationNotFoundException(id));
	}
	
	public List<Reservation> getAll(){
		return this.repository.findAll();
	}
}
