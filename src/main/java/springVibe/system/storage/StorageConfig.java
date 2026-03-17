package springVibe.system.storage;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    public ApplicationRunner ensureStorageDirectories(StorageProperties props) {
        return args -> {
            Path attachments = Path.of(props.getAttachmentsDir()).normalize();
            Files.createDirectories(attachments);
        };
    }
}

