package com.nitzer.campsitereservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.LinkedHashMap;
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
	
	private String host = "http://localhost:";
	
	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;
	
	@Autowired
	private ReservationRepository repository;

    private static final int THREAD_COUNT = 1000;

    private final Object threadLock = new Object();

    private volatile int threadReadyCount = 0;
    
    private volatile int conflictReservation = 0;
    
    private volatile int errorReservation = 0;
	
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
		reservation.setArrivalDate(LocalDate.now().plusDays(1));
		reservation.setDepartureDate(LocalDate.now().plusDays(3));
		reservation.setEmail("test@test.com");
		reservation.setFullName("Test 1");
		ResponseEntity<Object> creationResponse = this.restTemplate.postForEntity("http://localhost:" + port + "/reservation", reservation, Object.class );
		
		if(creationResponse.getStatusCode().is2xxSuccessful()) {
			Reservation created = objectMapper.convertValue(creationResponse.getBody(), Reservation.class);
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
		reservation.setArrivalDate(LocalDate.now().plusDays(-1));
		reservation.setDepartureDate(LocalDate.now());
		reservation.setEmail("test@test.com");
		reservation.setFullName("Test 1");
		ResponseEntity<Object> creationResponse = this.restTemplate.postForEntity("http://localhost:" + port + "/reservation", reservation, Object.class );

		
		log.info("Reservation result: {}",creationResponse);
		
		assertTrue(creationResponse.getStatusCode().is4xxClientError());
	}
	
	@Test
	@Order(5)
	public void reserveAndModify() throws Exception {
		Reservation reservation = new Reservation();
		reservation.setArrivalDate(LocalDate.now().plusDays(1));
		reservation.setDepartureDate(LocalDate.now().plusDays(3));
		reservation.setEmail("test@test.com");
		reservation.setFullName("Test 1");
		
		ResponseEntity<Object> creationResponse = this.restTemplate.postForEntity("http://localhost:" + port + "/reservation", reservation, Object.class);
		
		if(creationResponse.getStatusCode().is2xxSuccessful()) {
			Reservation toUpdate = objectMapper.convertValue(creationResponse.getBody(), Reservation.class);
			LocalDate newArrivalDate = LocalDate.now().plusDays(2);
			
			toUpdate.setArrivalDate(newArrivalDate);

		    HttpHeaders headers = new HttpHeaders();
		    headers.setContentType(MediaType.APPLICATION_JSON); 
			
			HttpEntity<Reservation> entity = new HttpEntity<Reservation>(toUpdate, headers); 
			
			ResponseEntity<Object> updateResponse = this.restTemplate.exchange(host + port + "/reservation/" + toUpdate.getId(),HttpMethod.PUT,entity,Object.class);
			if(updateResponse.getStatusCode().is2xxSuccessful()) {
				Reservation updated = objectMapper.convertValue(updateResponse.getBody(), Reservation.class);
				assertEquals(updated.getArrivalDate(),newArrivalDate);
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
                .mapToObj(i -> new Thread(() -> testThread(i)))
                .collect(Collectors.toList());
        
        threadList.forEach(Thread::start);

        // Wait for threads to be ready
        do {
            threadSleep(100);
        } while (threadReadyCount < THREAD_COUNT);

        // Start test
        synchronized (threadLock) {
            threadLock.notifyAll();
        }

        // Join threads
        for (Thread thread : threadList) {
            thread.join();
        }
        
		assertEquals(conflictReservation, (THREAD_COUNT-1));
	}
	
    private Reservation generateRandomReservation(int i) {
    	Reservation b = new Reservation();
    	b.setEmail(String.format("test%s@gmail.com",i));
    	b.setFullName(String.format("Thready %s", i));
    	b.setArrivalDate(LocalDate.now().plusDays(4));
    	b.setDepartureDate(LocalDate.now().plusDays(5));
    	return b;
    }
	
	private void testThread(int i) {
        log.info("Thread {} started", i);
        threadWait();
        log.info("Thread {} will save a reservation", i);
        ResponseEntity<Object> creationResponse = this.restTemplate.postForEntity(host + port + "/reservation", generateRandomReservation(i),Object.class);
        	
    	if(creationResponse.getStatusCode().is2xxSuccessful()) {
    		Reservation created = objectMapper.convertValue(creationResponse.getBody(), Reservation.class);
        	log.info("Thread {} make his reservation. Id: {}",i,created.getId());
    	}else {
    		LinkedHashMap<?,?> response = (LinkedHashMap<?,?>) creationResponse.getBody();
    		if(creationResponse.getStatusCode().is4xxClientError()) {
	    		synchronized(threadLock) {
	    			conflictReservation++;
	    		}
	        	log.info("Thread {} had a client error making his reservation. Error: {}", i,response.get("errors"));
	    	}else {
	    		synchronized(threadLock) {
	    			errorReservation++;
	    		}
	        	log.info("Thread {} had a server error making his reservation. Error: {}", i,response.get("errors"));
	    	}
    	}
    	
        threadSleep(100);
        log.info("Thread {} stopping", i);
    }
	
    @SneakyThrows(InterruptedException.class)
    private void threadSleep(int duration) {
        Thread.sleep(duration);
    }
    
    @SneakyThrows(InterruptedException.class)
    private void threadWait() {
        synchronized (threadLock) {
            threadReadyCount++;
            threadLock.wait();
        }
    }
}
