package springVibe.dev.users.chat.service;

import springVibe.dev.users.chat.config.OllamaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Qualifier;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class OllamaChatService {
    private final RestClient ollama;
    private final OllamaProperties props;
    private final ObjectMapper objectMapper;

    public OllamaChatService(
        @Qualifier("ollamaRestClient") RestClient ollamaRestClient,
        OllamaProperties props,
        ObjectMapper objectMapper
    ) {
        this.ollama = ollamaRestClient;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public boolean isHealthy() {
        try {
            // Will throw on connection errors.
            ollama.get()
                .uri("/api/tags")
                .retrieve()
                .body(Map.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String chat(List<Message> messages) {
        // Default a bit higher to avoid "cut off" answers on summarization / longer replies.
        return chat(messages, new Options(0.2, 0.9, 512));
    }

    public String chat(List<Message> messages, Options options) {
        var req = new ChatRequest(
            props.model(),
            messages,
            false,
            options
        );

        // Ollama can respond with content-type "application/octet-stream" even when `stream=false`.
        // Parse as bytes first, then decode & map JSON ourselves for robustness.
        byte[] bodyBytes = ollama.post()
            .uri("/api/chat")
            .contentType(MediaType.APPLICATION_JSON)
            .body(req)
            .retrieve()
            .body(byte[].class);

        if (bodyBytes == null || bodyBytes.length == 0) {
            return "";
        }

        String body = new String(bodyBytes, StandardCharsets.UTF_8).trim();
        if (body.isEmpty()) return "";

        // Defensive: if server ever returns NDJSON, keep the last JSON object.
        int lastNewline = Math.max(body.lastIndexOf('\n'), body.lastIndexOf('\r'));
        String json = lastNewline >= 0 ? body.substring(lastNewline + 1).trim() : body;
        if (json.isEmpty()) json = body;

        ChatResponse resp;
        try {
            resp = objectMapper.readValue(json, ChatResponse.class);
        } catch (Exception e) {
            // Fallback: try parsing the whole body (could include whitespace/extra lines)
            try {
                resp = objectMapper.readValue(body, ChatResponse.class);
            } catch (Exception e2) {
                throw new IllegalStateException("Failed to parse Ollama response as JSON: " + body, e2);
            }
        }

        if (resp == null || resp.message == null || resp.message.content == null) {
            return "";
        }
        return resp.message.content.trim();
    }

    public record Message(String role, String content) {
        public static Message system(String content) {
            return new Message("system", content);
        }

        public static Message user(String content) {
            return new Message("user", content);
        }

        public static Message assistant(String content) {
            return new Message("assistant", content);
        }
    }

    public record ChatRequest(
        String model,
        List<Message> messages,
        boolean stream,
        Options options
    ) {}

    public record Options(
        Double temperature,
        Double top_p,
        Integer num_predict
    ) {}

    public record ChatResponse(
        Message message,
        Boolean done
    ) {}
}
