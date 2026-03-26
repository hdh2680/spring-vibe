package springVibe.dev.users.devSearch.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DevSearchElasticsearchProperties.class)
public class DevSearchConfig {
}

