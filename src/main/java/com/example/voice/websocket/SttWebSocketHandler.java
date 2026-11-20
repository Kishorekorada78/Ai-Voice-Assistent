package com.example.voice.websocket;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import com.example.voice.service.TranscriptionService;

@Component    // @Component annotation makes this handler a Spring bean, so the constructor can receive TranscriptionService automatically.
public class SttWebSocketHandler extends BinaryWebSocketHandler{
	
	private static final Logger log=LoggerFactory.getLogger(SttWebSocketHandler.class);
	
	private final TranscriptionService transcriptionService;
	
	 public SttWebSocketHandler(TranscriptionService transcriptionService) {
	        this.transcriptionService = transcriptionService;
	    }
	 
	 // First message from the browser : th eaudio mime type , e.g "audio/webm"
	 protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		 session.getAttributes().put("mime", message.getPayload());
	 }
	 
	 // Every later message: one complete spoken sentence (audio bytes)
	 protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws IOException {
	        byte[] audio = new byte[message.getPayload().remaining()];
	        message.getPayload().get(audio);
	        String mime = (String) session.getAttributes().getOrDefault("mime", "audio/webm");

	        try {
	            String text = transcriptionService.transcribe(audio, mime);
	            session.sendMessage(new TextMessage(text));          // empty string = no speech found
	        } catch (Exception e) {
	            log.error("Transcription failed", e);
	            session.sendMessage(new TextMessage("ERROR:" + e.getMessage()));
	        }
	    }
}
