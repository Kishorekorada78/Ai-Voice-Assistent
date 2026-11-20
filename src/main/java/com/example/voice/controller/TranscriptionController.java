package com.example.voice.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.example.voice.service.TranscriptionService;

@RestController
@RequestMapping("/api/stt")
public class TranscriptionController {
	
	private final TranscriptionService service;
	
	public TranscriptionController(TranscriptionService service ) {
		this.service=service;
	}

	@PostMapping(value="/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Map<String,String> transcribe(@RequestParam("file") MultipartFile file) throws IOException{
	 if (file.isEmpty()) {
         throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Audio file is required");
     }
     return Map.of("text", service.transcribe(file));
  }
}
