package com.nitzer.campsitereservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitzer.campsitereservation.entities.Reservation;
import com.nitzer.campsitereservation.repositories.ReservationRepository;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestMethodOrder(OrderAnnotation.class)
class CampsiteReservationApplicationTests {
	
	private LocalDate today = LocalDate.now();
	
	private String host = "http://localhost:";
	
	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;
	
	@Autowired
	private ReservationRepository repository;

    private static final int THREAD_COUNT = 10;

    private final Object reserverThreadLock = new Object();
    
    private final Object checkerThreadLock = new Object();

    private volatile int reserverThreadReadyCount = 0;
    
    private volatile int checkerThreadReadyCount = 0;
    
    private volatile int conflictReservation = 0;
    
    private volatile int errorReservation = 0;
    
    private volatile int errorChecking = 0;
	
    @Autowired
    protected ObjectMapper objectMapper;

    @Test
    @Order(1)
    public void contextLoads() {
    	assertThat(this.repository != null);
    }
    
	@Test
	@Order(2)
	public void shouldReturnDefaultMessage() throws Exception {
		assertThat(this.restTemplate.getForObject("http://localhost:" + port + "/",
				String.class)).contains("Welcome to campsite reservation API");
	}
	
	@Test
	@Order(3)
	public void reserveAndCancel() throws Exception {
		
		Reservation reservation = new Reservation();
		reservation.setArrivalDate(today);
		reservation.setDepartureDate(today.plusDays(3));
		reservation.setCheckInDate(today.plusDays(1));
		reservation.setCheckOutDate(today.plusDays(3));
		reservation.setEmail("test@test.com");
		reservation.setFullName("Test 1");
		
		ResponseEntity<Object> creationResponse = this.restTemplate.postForEntity("http://localhost:" + port + "/reservation", reservation, Object.class );
		
		if(creationResponse.getStatusCode().is2xxSuccessful()) {
			Reservation created = objectMapper.convertValue(creationResponse.getBody(), Reservation.class);
			log.info("Reservation saved with identifier: " + created.getId());
			
			log.info("Reservation {} will be deleted." + created.getId());
			
			this.restTemplate.delete(host + port + "/reservation/" + created.getId());
			
			Optional<Reservation> existsReservation = this.repository.findById(created.getId());
			
			assertTrue(existsReservation.isEmpty());
		}else {
			log.info("Error saving reservation: {}",creationResponse.getBody());
			fail();
		}
	}
	

	@Test
	@Order(4)
	public void invalidDates() throws Exception {
		Reservation reservation = new Reservation();
		reservation.setEmail("test@test.com");
		reservation.setFullName("Test 1");
		reservation.setArrivalDate(today);
		reservation.setDepartureDate(today.plusDays(3));
		reservation.setCheckInDate(today.plusDays(-1));
		reservation.setCheckOutDate(today);
		ResponseEntity<Object> creationResponse = this.restTemplate.postForEntity("http://localhost:" + port + "/reservation", reservation, Object.class );

		assertTrue(creationResponse.getStatusCode().is4xxClientError());
	}
	
	@Test
	@Order(5)
	public void reserveAndModify() throws Exception {
		Reservation reservation = new Reservation();
		reservation.setArrivalDate(today.plusDays(1));
		reservation.setDepartureDate(today.plusDays(3));
		reservation.setCheckInDate(today.plusDays(2));
		reservation.setCheckOutDate(today.plusDays(3));
		reservation.setEmail("test@test.com");
		reservation.setFullName("Test 1");
		
		ResponseEntity<Object> creationResponse = this.restTemplate.postForEntity("http://localhost:" + port + "/reservation", reservation, Object.class);
		
		if(creationResponse.getStatusCode().is2xxSuccessful()) {
			Reservation toUpdate = objectMapper.convertValue(creationResponse.getBody(), Reservation.class);
			log.info("Reservation saved with identifier: " + toUpdate.getId());
			
			
			LocalDate newCheckInDate = today.plusDays(2);
			
			toUpdate.setCheckInDate(newCheckInDate);

		    HttpHeaders headers = new HttpHeaders();
		    headers.setContentType(MediaType.APPLICATION_JSON); 
			
			HttpEntity<Reservation> entity = new HttpEntity<Reservation>(toUpdate, headers); 
			
			ResponseEntity<Object> updateResponse = this.restTemplate.exchange(host + port + "/reservation/" + toUpdate.getId(),HttpMethod.PUT,entity,Object.class);
			if(updateResponse.getStatusCode().is2xxSuccessful()) {
				Reservation updated = objectMapper.convertValue(updateResponse.getBody(), Reservation.class);
				assertEquals(updated.getCheckInDate(),newCheckInDate);
			}else {
				log.info("Error updating reservation: {}",updateResponse.getBody());
				fail();
			}
		}else {
			log.info("Error making reservation: {}",creationResponse.getBody());
		
			fail();
		}
	}
	
	@Test
	@Order(6)
	public void generateReservationsConcurrently() throws Exception {
		// Create threads
        var threadList = IntStream.range(0, THREAD_COUNT)
                .mapToObj(i -> new Thread(() -> testReserverThread(i)))
                .collect(Collectors.toList());
        
        threadList.forEach(Thread::start);

        // Wait for threads to be ready
        do {
            threadSleep(100);
        } while (reserverThreadReadyCount < THREAD_COUNT);

        // Start test
        synchronized (reserverThreadLock) {
            reserverThreadLock.notifyAll();
        }

        // Join threads
        for (Thread thread : threadList) {
            thread.join();
        }
        
		assertTrue(conflictReservation ==  (THREAD_COUNT-1) && errorReservation == 0);
	}
	
	private Reservation generateRandomReservation(int i) {
    	Reservation b = new Reservation();
    	b.setEmail(String.format("test%s@gmail.com",i));
    	b.setFullName(String.format("Thready %s", i));
    	b.setCheckInDate(today.plusDays(5));
    	b.setCheckOutDate(today.plusDays(6));
    	b.setArrivalDate(today.plusDays(4));
    	b.setDepartureDate(today.plusDays(6));
    	return b;
    }
	
	private void testReserverThread(int i) {
        log.info("Reserver Thread {} started", i);
        reserverThreadWait();
        log.info("Reserver Thread {} will save a reservation", i);
        ResponseEntity<Object> creationResponse = this.restTemplate.postForEntity(host + port + "/reservation", generateRandomReservation(i),Object.class);
        	
    	if(creationResponse.getStatusCode().is2xxSuccessful()) {
    		Reservation created = objectMapper.convertValue(creationResponse.getBody(), Reservation.class);
        	log.info("Reserver Thread {} make his reservation. Id: {}",i,created.getId());
    	}else {
    		LinkedHashMap<?,?> response = (LinkedHashMap<?,?>) creationResponse.getBody();
    		if(creationResponse.getStatusCode().is4xxClientError()) {
	    		synchronized(reserverThreadLock) {
	    			conflictReservation++;
	    		}
	        	log.info("Reserver Thread {} had a client error making his reservation. Error: {}", i,response.get("errors"));
	    	}else {
	    		synchronized(reserverThreadLock) {
	    			errorReservation++;
	    		}
	        	log.info("Reserver Thread {} had a server error making his reservation. Error: {}", i,response.get("errors"));
	    	}
    	}
    	
        threadSleep(100);
        log.info("Reserver Thread {} stopping", i);
    }
	
	@SneakyThrows(InterruptedException.class)
    private void reserverThreadWait() {
        synchronized (reserverThreadLock) {
            reserverThreadReadyCount++;
            reserverThreadLock.wait();
        }
    }
	
	@Test
	@Order(7)
	public void checkAvaibilityConcurrently() throws Exception {
		// Create threads
        var threadList = IntStream.range(0, THREAD_COUNT)
                .mapToObj(i -> new Thread(() -> testCheckerThread(i)))
                .collect(Collectors.toList());
        
        threadList.forEach(Thread::start);

        // Wait for threads to be ready
        do {
            threadSleep(100);
        } while (checkerThreadReadyCount < THREAD_COUNT);
        
        // Start test
        synchronized (checkerThreadLock) {
            checkerThreadLock.notifyAll();
        }

        // Join threads
        for (Thread thread : threadList) {
            thread.join();
        }
        
		assertTrue(errorChecking == 0);
	}
	
	private void testCheckerThread(int i) {
        log.info("Checker Thread {} started", i);
        checkerThreadWait();
        log.info("Checker Thread {} will check availability", i);
        try {
	        ResponseEntity<Object> checkAvailabilityResponse = this.restTemplate.getForEntity(host + port + "/reservation/available",Object.class);
	        	
	    	if(checkAvailabilityResponse.getStatusCode().is2xxSuccessful()) {
	    		List<?> created = objectMapper.convertValue(checkAvailabilityResponse.getBody(), List.class);
	        	log.info("Checker Thread {} checked availability. Dates: {}",i,created.toString());
	    	}else {
	    		LinkedHashMap<?,?> response = (LinkedHashMap<?,?>) checkAvailabilityResponse.getBody();
	    		synchronized(checkerThreadLock) {
	    			errorChecking++;
	    		}
	        	log.info("Checker Thread {} had a server error checking availability. Error: {}", i,response.get("errors"));
		    	
	    	}
        }catch(Exception e) {
        	synchronized(checkerThreadLock) {
    			errorChecking++;
    		}
        	log.info("Check Thread {} had a server error checking availability. Error: {}", i,e.getMessage());
	    	
        }
    	
        threadSleep(100);
        log.info("Checker Thread {} stopping", i);
    }
	
	 @SneakyThrows(InterruptedException.class)
	    private void checkerThreadWait() {
	        synchronized (checkerThreadLock) {
	            checkerThreadReadyCount++;
	            checkerThreadLock.wait();
	        }
	    }
    
	
    @SneakyThrows(InterruptedException.class)
    private void threadSleep(int duration) {
        Thread.sleep(duration);
    }
    
    

}
