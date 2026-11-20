# Spring AI Voice Assistant

A voice assistant built with Spring Boot, Spring AI and the Gemini API.
Speak to it, it understands you, answers with memory of the conversation, and speaks back.

```
Mic -> speech-to-text -> chat with memory (streamed) -> text-to-speech -> Speaker
```

## Features

- Dictation: tap the mic and the speech is turned into text
- Chat: replies stream in word by word, and the assistant remembers the conversation
- Read aloud: every reply has a listen button, with a choice of voices
- Voice mode: a hands-free loop that listens, thinks and speaks, and starts speaking before the full reply is written
- New chat button that clears the conversation memory

## Tech stack

| Part | Technology |
|---|---|
| Backend | Java, Spring Boot, Spring AI (`ChatClient`, chat memory advisor) |
| AI | Gemini API (chat, audio understanding, text-to-speech) |
| Frontend | One HTML file with plain JavaScript (Web Audio, `fetch` streaming) |

## Endpoints

| Method and path | Purpose |
|---|---|
| `POST /api/stt/transcribe` | Upload a WAV file (`file`), returns `{ "text": "..." }` |
| `POST /api/chat` | Form fields `message`, `conversationId`, returns `{ "reply": "..." }` |
| `POST /api/chat/stream` | Same input, streams the reply as server-sent events |
| `DELETE /api/chat` | Form field `conversationId`, forgets that conversation |
| `POST /api/tts` | Form fields `text`, `voice`, returns a WAV file |

## Run it

1. Get a Gemini API key from Google AI Studio.
2. Set it as an environment variable (never put it in a file you commit):
   - Windows PowerShell: `$env:GEMINI_API_KEY="your-key"`
   - Windows cmd: `set GEMINI_API_KEY=your-key`
   - macOS or Linux: `export GEMINI_API_KEY=your-key`
3. Start the app from your IDE, or build and run the jar:
   ```
   ./mvnw clean package
   java -jar target/<your-jar-name>.jar
   ```
4. Open http://localhost:8080 in Chrome or Edge and allow the microphone.

## Configuration

Main settings in `src/main/resources/application.properties`:

| Property | Meaning |
|---|---|
| `spring.ai.google.genai.api-key` | Read from `GEMINI_API_KEY` |
| `spring.ai.google.genai.chat.options.model` | Gemini model for chat and transcription |
| `app.tts.model` | Gemini model for speech generation |
| `server.port` | Port the app listens on |

## Project structure

```
src/main/java/com/example/voice
  VoiceTranscriberApplication.java
  config/ChatMemoryConfig.java       window of 20 messages per conversation
  controller/                        TranscriptionController, ChatController, TtsController
  service/                           TranscriptionService, ChatService, TtsService
src/main/resources
  application.properties
  static/index.html                  the web interface
```

## Known limits

- Conversation memory lives in RAM, so it is lost when the app restarts.
- There is no login. Anyone who can reach the app can use your Gemini quota, so do not expose it publicly as it is.
- The free Gemini tier has rate limits. A voice turn makes about three to four requests.
- Use headphones: speaker sound can leak into the microphone.

## Ideas for next steps

- Live word-by-word transcription while speaking (Gemini Live API over WebSocket)
- Tools or your own documents (RAG) so the assistant answers from real data
- Persistent memory in a database
- Login and per-user rate limits
