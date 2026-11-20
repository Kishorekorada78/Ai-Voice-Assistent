package com.example.voice.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatService {

    private static final String SYSTEM = """
            You are a friendly voice assistant. Your replies are read aloud, so:
            - Speak in plain, natural English, in one to three short sentences unless asked for more.
            - Never use markdown, bullet points, numbered lists, headings, tables, code blocks or emojis.
            - Keep every reply under 600 characters.
            - If you did not understand, ask one short clarifying question.
            """;

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final MessageChatMemoryAdvisor memoryAdvisor;

    public ChatService(ChatClient.Builder builder, ChatMemory chatMemory) {
        this.chatClient = builder.build();
        this.chatMemory = chatMemory;
        this.memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    public String reply(String conversationId, String userMessage) {
        String reply = chatClient.prompt()
                .system(SYSTEM)
                .user(userMessage)
                .advisors(memoryAdvisor)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        return (reply == null || reply.isBlank())
                ? "Sorry, I didn't catch that. Could you say it again?"
                : reply.trim();
    }

    public void reset(String conversationId) {
        chatMemory.clear(conversationId);
    }
    
    // Same prompt, system rules and memory as reply(), but the answer arrives in pieces
    public Flux<String> stream(String conversationId, String userMessage) {
        return chatClient.prompt()
                .system(SYSTEM)
                .user(userMessage)
                .advisors(memoryAdvisor)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}