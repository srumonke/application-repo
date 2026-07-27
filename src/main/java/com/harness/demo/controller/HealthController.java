package com.harness.demo.controller;

import com.harness.demo.model.GreetingResponse;
import com.harness.demo.model.HealthResponse;
import com.harness.demo.service.GreetingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final GreetingService greetingService;

    public HealthController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse(greetingService.getHealthStatus());
    }

    @GetMapping("/greet")
    public GreetingResponse greet(@RequestParam(required = false) String name) {
        return new GreetingResponse(greetingService.greet(name));
    }
}
