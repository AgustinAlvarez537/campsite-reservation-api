package com.nitzer.campsitereservation.services;

import java.time.LocalDate;
import java.util.List;
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
		List<LocalDate> bookedDates = reservations.stream().flatMap(b -> b.getArrivalDate().datesUntil(b.getDepartureDate())).collect(Collectors.toList());
		List<LocalDate> result = dateFrom.datesUntil(dateTo).filter(d -> !bookedDates.contains(d)).collect(Collectors.toList());
		return result;
	}
	

	@Transactional(isolation=Isolation.SERIALIZABLE, rollbackFor=Exception.class)
	public Reservation reserve(Reservation reservation) throws RuntimeException {
		
		if(!this.repository.existsOverlappingReservation(reservation.getArrivalDate(), reservation.getDepartureDate(),null)) {
			reservation = this.repository.save(reservation);
		}else{
			throw new OverlappingDatesException(reservation.getArrivalDate(),reservation.getDepartureDate());
		}
		
		return reservation;
	}

	@Transactional(isolation=Isolation.SERIALIZABLE, rollbackFor=Exception.class)
	public Reservation update(Reservation reservation, Long id) throws RuntimeException {
		
		Reservation toUpdate = this.repository.findById(id).orElseThrow(() -> new ReservationNotFoundException(id));
		
		toUpdate.setArrivalDate(reservation.getArrivalDate());
		toUpdate.setDepartureDate(reservation.getDepartureDate());
		
		if(!this.repository.existsOverlappingReservation(reservation.getArrivalDate(), reservation.getDepartureDate(),toUpdate.getId())) {
			toUpdate = this.repository.save(toUpdate);
		}else{
			throw new OverlappingDatesException(toUpdate.getArrivalDate(),toUpdate.getDepartureDate());
		}
		return toUpdate;
	}

	@Transactional(isolation=Isolation.SERIALIZABLE, rollbackFor=Exception.class)
	public void cancel(Long id) throws RuntimeException {
		Reservation toDelete = this.repository.findById(id).orElseThrow(() -> new ReservationNotFoundException(id));
		this.repository.delete(toDelete);
	}
	
	public Reservation getOne(Long id) {
		return this.repository.findById(id).orElseThrow(() -> new ReservationNotFoundException(id));
	}
	
	public List<Reservation> getAll(){
		return this.repository.findAll();
	}
}
