package org.example.shared.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthzController {

    @GetMapping(path = "/healthz")
    public ResponseEntity<String> healthz() {
        return ResponseEntity.ok("ok");
    }
}
