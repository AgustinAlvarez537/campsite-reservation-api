package com.nitzer.campsitereservation.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.nitzer.campsitereservation.entities.Reservation;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
	
	@Query("SELECT b from Reservation b WHERE :dateFrom <= b.checkOutDate and :dateTo >= b.checkInDate")
	public List<Reservation> findByDateInterval(LocalDate dateFrom, LocalDate dateTo);
	
	@Query("SELECT count(b) > 0 from Reservation b WHERE :dateFrom <= b.checkOutDate and :dateTo >= b.checkInDate and (:id is null or id != :id)")
	public boolean existsOverlappingReservation(LocalDate dateFrom, LocalDate dateTo, UUID id);
}
