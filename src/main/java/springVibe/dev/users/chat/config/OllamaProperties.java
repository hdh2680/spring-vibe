package springVibe.dev.users.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ollama")
public record OllamaProperties(
    String baseUrl,
    String model
) {
}

