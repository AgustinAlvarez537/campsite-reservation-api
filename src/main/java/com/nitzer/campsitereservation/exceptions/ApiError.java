package com.nitzer.campsitereservation.exceptions;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;

import lombok.Data;

@Data
public class ApiError {
    private List<String> errors;
 
    public ApiError(List<String> errors) {
        super();
        this.errors = errors;
    }
 
    public ApiError(String error) {
        super();
        errors = Arrays.asList(error);
    }
}