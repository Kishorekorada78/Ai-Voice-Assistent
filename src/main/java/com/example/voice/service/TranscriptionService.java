package com.example.voice.service;

import java.io.IOException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TranscriptionService {

    private static final String PROMPT = """
            Transcribe the speech in this audio exactly as spoken.
            Add correct punctuation and capitalization.
            Return ONLY the transcript text, with no explanation or extra words.
            If there is no clear human speech, return an empty string.
            """;

    private final ChatClient chatClient;

    public TranscriptionService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    // Used by the REST controller (file upload)
    public String transcribe(MultipartFile file) throws IOException {
        return transcribe(file.getBytes(), file.getContentType());
    }

    // Used by REST and WebSocket (raw audio bytes)
    public String transcribe(byte[] audioBytes, String contentType) {
        MimeType mimeType = toCleanMimeType(contentType);
        String text = chatClient.prompt()
                .user(u -> u.text(PROMPT).media(mimeType, new ByteArrayResource(audioBytes)))   // Attaches the audio
                .call()
                .content();
        return text == null ? "" : text.trim();
    }

    private MimeType toCleanMimeType(String contentType) {
        if (contentType == null) {
            return MimeTypeUtils.parseMimeType("audio/mpeg");
        }
        MimeType parsed = MimeTypeUtils.parseMimeType(contentType);
        return new MimeType(parsed.getType(), parsed.getSubtype());
    }
}