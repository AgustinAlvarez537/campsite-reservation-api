package com.nitzer.campsitereservation.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ReservationNotFoundException extends RuntimeException{

    private static final long serialVersionUID = 1L;

    public ReservationNotFoundException(Long id){
        super(String.format("Reservation with id %s not found",id));
    }
}
