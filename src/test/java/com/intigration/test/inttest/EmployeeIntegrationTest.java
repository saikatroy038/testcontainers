package com.intigration.test.inttest;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EmployeeIntegrationTest {

	private static final String URI_FORMAT = "http://localhost:%d/%s";
	private static final String RESOURCE = "employees";
	private static final PostgreSQLContainer<?> CONTAINER = new PostgreSQLContainer<>("postgres:latest");
	
	static {
		CONTAINER.start();
	}

	@LocalServerPort
	private int port;
	
	private String uri;
	private RestTemplate restTemplate;
	
	@Autowired
	private EmployeeRepository employeeRepository;

	@DynamicPropertySource
	public static void setDatasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", CONTAINER::getJdbcUrl);
		registry.add("spring.datasource.username", CONTAINER::getUsername);
		registry.add("spring.datasource.password", CONTAINER::getPassword);
	}

	@BeforeAll
	public void init() {
		restTemplate = new RestTemplate();
		uri = String.format(URI_FORMAT, port, RESOURCE);
	}

	@AfterAll
	public void tearDown() {
		CONTAINER.stop();
	}

	@AfterEach
	public void cleanup() {
		employeeRepository.deleteAll();
	}

	@Test
	public void testSaveEmployees() {
		Employee emp = Employee.builder().userId("123").name("somename").build();
		ResponseEntity<Employee> response = restTemplate.postForEntity(uri, emp, Employee.class);
		assertEquals("Unexpected HTTP status code", HttpStatus.OK, response.getStatusCode());
		assertTrue("Response body is null", response.hasBody());
		assertEquals("Name mismatch between request and response", emp.getName(), response.getBody().getName());
		assertEquals("User ID mismatch between request and response", emp.getUserId(), response.getBody().getUserId());
	}
}
