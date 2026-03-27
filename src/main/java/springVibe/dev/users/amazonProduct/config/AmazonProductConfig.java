package springVibe.dev.users.amazonProduct.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AmazonProductElasticsearchProperties.class)
public class AmazonProductConfig {
}

