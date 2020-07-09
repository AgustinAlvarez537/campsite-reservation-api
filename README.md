# API for campsite reservation

Current limitations:
<br>
* Anybody can modify a reservation with only the id (which is an 128 bit uuid)
* A single person can reserve all available dates and resell then (To avoid this, we should ask for a valid credential to confirm the reservation)