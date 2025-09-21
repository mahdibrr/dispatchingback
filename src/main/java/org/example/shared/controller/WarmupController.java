package org.example.shared.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WarmupController {

    @GetMapping(path = "/warmup")
    public ResponseEntity<Void> warmup() {
        return ResponseEntity.noContent().build();
    }
}
