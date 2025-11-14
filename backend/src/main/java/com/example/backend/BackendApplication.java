package com.example.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

    @CrossOrigin(origins = {"http://localhost:4200", "http://20.57.79.136", "http://20.57.79.136:80", "http://20.57.79.136:8080"})
    @GetMapping("/hello")
    public String hello() {
        return "Hello from Spring Boot";
    }

}
