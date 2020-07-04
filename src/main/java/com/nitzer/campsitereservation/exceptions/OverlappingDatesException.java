package com.nitzer.campsitereservation.exceptions;

import java.time.LocalDate;

public class OverlappingDatesException extends RuntimeException{

    private static final long serialVersionUID = 1L;

    public OverlappingDatesException(LocalDate dateFrom, LocalDate dateTo){
        super(String.format("Another reservation already exists in the date range specified. Range specified: %s - %s", dateFrom, dateTo));
    }
}
