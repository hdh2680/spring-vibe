package springVibe.system.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application-level storage locations.
 *
 * <p>Keep this separate from Spring's multipart temp settings: this is where the app persists
 * attachments long-term.</p>
 */
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {
    /**
     * Directory where attachments are saved. Can be absolute or relative.
     */
    private String attachmentsDir = "C:/workspace/data/attachments";

    public String getAttachmentsDir() {
        return attachmentsDir;
    }

    public void setAttachmentsDir(String attachmentsDir) {
        this.attachmentsDir = attachmentsDir;
    }
}
