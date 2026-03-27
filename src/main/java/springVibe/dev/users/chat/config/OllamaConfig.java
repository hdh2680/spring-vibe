package springVibe.dev.users.chat.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OllamaProperties.class)
public class OllamaConfig {

    @Bean
    public RestClient ollamaRestClient(OllamaProperties props) {
        var rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(5_000);
        rf.setReadTimeout(120_000);

        return RestClient.builder()
            .baseUrl(props.baseUrl())
            .requestFactory(rf)
            .build();
    }
}
