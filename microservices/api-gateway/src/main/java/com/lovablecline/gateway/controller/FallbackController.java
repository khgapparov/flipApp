package com.ecolightcline.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/auth")
    public Mono<ResponseEntity<Map<String, Object>>> authServiceFallback() {
        return createFallbackResponse("Authentication service is temporarily unavailable");
    }

    @GetMapping("/user")
    public Mono<ResponseEntity<Map<String, Object>>> userServiceFallback() {
        return createFallbackResponse("User service is temporarily unavailable");
    }

    @GetMapping("/project")
    public Mono<ResponseEntity<Map<String, Object>>> projectServiceFallback() {
        return createFallbackResponse("Project service is temporarily unavailable");
    }

    @GetMapping("/chat")
    public Mono<ResponseEntity<Map<String, Object>>> chatServiceFallback() {
        return createFallbackResponse("Chat service is temporarily unavailable");
    }

    @GetMapping("/gallery")
    public Mono<ResponseEntity<Map<String, Object>>> galleryServiceFallback() {
        return createFallbackResponse("Gallery service is temporarily unavailable");
    }

    private Mono<ResponseEntity<Map<String, Object>>> createFallbackResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SERVICE_UNAVAILABLE");
        response.put("message", message);
        response.put("code", 503);
        
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }
}
