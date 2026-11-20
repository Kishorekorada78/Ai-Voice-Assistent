package com.example.voice.controller;

import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.voice.service.ChatService;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Pattern ID = Pattern.compile("[A-Za-z0-9-]{8,64}");
    private static final int MAX_CHARS = 2000;

    private final ChatService service;

    public ChatController(ChatService service) {
        this.service = service;
    }

    @PostMapping
    public Map<String, String> chat(@RequestParam String message,
                                    @RequestParam String conversationId) {
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is required");
        }
        if (message.length() > MAX_CHARS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message too long (max " + MAX_CHARS + ")");
        }
        checkId(conversationId);

        try {
            return Map.of("reply", service.reply(conversationId, message));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, rootMessage(e));
        }
    }
    
    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String message,
                             @RequestParam String conversationId) {
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is required");
        }
        if (message.length() > MAX_CHARS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message too long (max " + MAX_CHARS + ")");
        }
        checkId(conversationId);

        SseEmitter emitter = new SseEmitter(120_000L);          // give up after 2 minutes

        Disposable subscription = service.stream(conversationId, message).subscribe(
                chunk -> send(emitter, null, Map.of("t", chunk)),          // each piece of text
                err -> {                                                    // something failed mid-stream
                    send(emitter, "error", Map.of("message", rootMessage(err)));
                    emitter.complete();
                },
                emitter::complete);                                         // finished normally

        // If the browser leaves or the request times out, stop asking Gemini for more
        emitter.onCompletion(subscription::dispose);
        emitter.onTimeout(subscription::dispose);
        emitter.onError(e -> subscription.dispose());
        return emitter;
    }

    private static void send(SseEmitter emitter, String eventName, Map<String, String> data) {
        try {
            SseEmitter.SseEventBuilder event = SseEmitter.event();
            if (eventName != null) event.name(eventName);
            emitter.send(event.data(data, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    // Start a fresh conversation: forget everything for this id
    @DeleteMapping
    public void reset(@RequestParam String conversationId) {
        checkId(conversationId);
        service.reset(conversationId);
    }

    private static void checkId(String id) {
        if (id == null || !ID.matcher(id).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid conversationId");
        }
    }

    // "Failed to generate content" hides the real reason, so dig down to the root cause
    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage() != null ? t.getMessage() : e.getClass().getSimpleName();
    }
}