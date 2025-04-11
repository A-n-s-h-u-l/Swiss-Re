package com.example.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notify")
@RequiredArgsConstructor
public class NotifyController {

    @PostMapping
    public ResponseEntity<String> create(@RequestBody String request) {
        return ResponseEntity.ok("Notified successfully");
    }
}
