package com.example.voice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.example.voice.service.TtsService;

@RestController
@RequestMapping("/api/tts")
public class TtsController {

    private static final int MAX_CHARS = 1000;            // protects your free-tier quota

    private final TtsService service;

    public TtsController(TtsService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<byte[]> speak(@RequestParam String text,
                                        @RequestParam(defaultValue = "Kore") String voice) {
        if (text == null || text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Text is required");
        }
        if (text.length() > MAX_CHARS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Text too long (max " + MAX_CHARS + ")");
        }
        if (!voice.matches("[A-Za-z]{2,20}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid voice name");
        }
        try {
            byte[] wav = service.speak(text, voice);
            return ResponseEntity.ok().contentType(MediaType.valueOf("audio/wav")).body(wav);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
    }
}