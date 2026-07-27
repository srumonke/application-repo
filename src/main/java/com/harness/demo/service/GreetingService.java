package com.harness.demo.service;

import org.springframework.stereotype.Service;

@Service
public class GreetingService {

    public String getHealthStatus() {
        return "UP";
    }

    public String greet(String name) {
        if (name == null || name.isBlank()) {
            name = "World";
        }
        return "Hello, " + name + "!";
    }
}
