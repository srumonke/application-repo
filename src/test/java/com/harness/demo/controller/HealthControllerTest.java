package com.harness.demo.controller;

import com.harness.demo.service.GreetingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GreetingService greetingService;

    @Test
    void testHealthEndpoint() throws Exception {
        when(greetingService.getHealthStatus()).thenReturn("UP");

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testGreetWithName() throws Exception {
        when(greetingService.greet("Alice")).thenReturn("Hello, Alice!");

        mockMvc.perform(get("/greet").param("name", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, Alice!"));
    }

    @Test
    void testGreetWithoutName() throws Exception {
        when(greetingService.greet(null)).thenReturn("Hello, World!");

        mockMvc.perform(get("/greet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, World!"));
    }
}
