package com.example.voice.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Service;

@Service
public class TtsService {

    private static final String URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private final HttpClient http = HttpClient.newHttpClient();
    private final String apiKey;
    private final String model;

    public TtsService(@Value("${spring.ai.google.genai.api-key}") String apiKey,
                      @Value("${app.tts.model:gemini-3.8-flash-lite-tts}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    /** Returns a playable WAV file for the given text. */
    public byte[] speak(String text, String voice) throws IOException, InterruptedException {

        // Gemini 3.8 models use "voice"; older TTS models use "prebuiltVoiceConfig"
        String voiceConfig = model.contains("3.8")
                ? "{\"voice\":" + jsonString(voice) + "}"
                : "{\"prebuiltVoiceConfig\":{\"voiceName\":" + jsonString(voice) + "}}";

        String body = "{\"contents\":[{\"role\":\"user\",\"parts\":[{\"text\":" + jsonString(text) + "}]}],"
                + "\"generationConfig\":{\"responseModalities\":[\"AUDIO\"],"
                + "\"speechConfig\":{\"voiceConfig\":" + voiceConfig + "}}}";

        HttpRequest request = HttpRequest.newBuilder(URI.create(URL.formatted(model)))
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String err = response.body();
            throw new IllegalStateException("Gemini TTS error " + response.statusCode() + ": "
                    + err.substring(0, Math.min(err.length(), 300)));
        }

        byte[] pcm = extractAudio(response.body());
        return pcmToWav(pcm, 24000, 1, 16);                 // Gemini TTS output: 24 kHz, 16-bit, mono
    }

    // Walk the JSON: candidates[0].content.parts[0].inlineData.data
    private byte[] extractAudio(String json) {
        try {
            Map<String, Object> root = JsonParserFactory.getJsonParser().parseMap(json);
            List<?> candidates = (List<?>) root.get("candidates");
            Map<?, ?> content = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
            List<?> parts = (List<?>) content.get("parts");
            Map<?, ?> inline = (Map<?, ?>) ((Map<?, ?>) parts.get(0)).get("inlineData");
            return Base64.getDecoder().decode((String) inline.get("data"));
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected response from Gemini (no audio found)");
        }
    }

    // Raw PCM has no header, so browsers can't play it. A 44-byte WAV header fixes that.
    private static byte[] pcmToWav(byte[] pcm, int sampleRate, int channels, int bits) {
        int byteRate = sampleRate * channels * bits / 8;
        ByteBuffer b = ByteBuffer.allocate(44 + pcm.length).order(ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36 + pcm.length)
         .put("WAVE".getBytes(StandardCharsets.US_ASCII))
         .put("fmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16)
         .putShort((short) 1).putShort((short) channels)
         .putInt(sampleRate).putInt(byteRate)
         .putShort((short) (channels * bits / 8)).putShort((short) bits)
         .put("data".getBytes(StandardCharsets.US_ASCII)).putInt(pcm.length)
         .put(pcm);
        return b.array();
    }

    // Safely turn any text into a JSON string literal
    private static String jsonString(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }
}