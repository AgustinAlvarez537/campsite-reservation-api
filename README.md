# Campsite reservation API

## Requirements
* Maven
* Java 11
* Lombok

## Swagger
The API has swagger, to access it only go to:

url/swagger-ui.html

## Current endpoints:
* **GET /**: To check if API load correctly
* **GET /reservation/available**: To check available dates
* **POST /reservation**: To make a reservation
* **PUT /reservation/{id}**: To modify a reservation
* **DELETE /reservation/{id}**: To cancel a reservation

## Current limitations:
* Anybody can modify a reservation with only the id (which is an 128 bit uuid)
* A single person can reserve all available dates and resell then (To avoid this, we should ask for a valid credential to confirm the reservation)
* When a reservation is canceled, it is deleted physically
