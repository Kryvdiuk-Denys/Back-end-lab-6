package com.coworking.booking.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminRestController {

    @GetMapping("/status")
    public Map<String, String> status() {
        return Map.of("status", "admin access granted");
    }
}
