package com.ecommerce.pedidos.presentation.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class ResilienceHealthController {
    
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    public ResilienceHealthController(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }
    
    @GetMapping("/circuit-breaker")
    public Map<String, Object> getCircuitBreakerStatus() {
        Map<String, Object> status = new HashMap<>();
        
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            Map<String, Object> cbStatus = new HashMap<>();
            cbStatus.put("state", cb.getState().name());
            cbStatus.put("failureRate", cb.getMetrics().getFailureRate() + "%");
            cbStatus.put("numberOfFailedCalls", cb.getMetrics().getNumberOfFailedCalls());
            cbStatus.put("numberOfSuccessfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
            cbStatus.put("numberOfNotPermittedCalls", cb.getMetrics().getNumberOfNotPermittedCalls());
            status.put(cb.getName(), cbStatus);
        });
        
        return status;
    }
}