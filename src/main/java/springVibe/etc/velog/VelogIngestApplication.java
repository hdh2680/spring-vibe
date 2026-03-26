package springVibe.etc.velog;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;

import java.util.Map;

/**
 * One-shot Velog ingestion app.
 *
 * Run example (Maven):
 *   mvn -DskipTests spring-boot:run -Dspring-boot.run.mainClass=springVibe.etc.velog.VelogIngestApplication ^
 *     -Dspring-boot.run.arguments="--velog.ingest.max-posts=5000 --velog.ingest.page-size=50"
 */
@SpringBootApplication(
        // Keep scanning narrow so we don't accidentally pick up other @SpringBootApplication classes
        // (which can lead to duplicate repository registrations).
        scanBasePackages = "springVibe.etc.velog",
        excludeName = {
                // Avoid requiring Elasticsearch to be up when this is a one-shot DB loader.
                "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration",
                "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration",
                "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration"
        }
)
@Profile("velog-ingest")
public class VelogIngestApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(VelogIngestApplication.class)
                .web(WebApplicationType.NONE)
                // Prevent this one-shot tool from being picked up when the main web app starts.
                .profiles("velog-ingest")
                .properties(Map.of(
                        "spring.main.banner-mode", "off",
                        "logging.level.root", "INFO",
                        "logging.level.springVibe.etc.velog", "INFO"
                ))
                .run(args);

        int exitCode = 0;
        try {
            ctx.getBean(VelogIngestService.class).ingest();
        } catch (Exception e) {
            exitCode = 1;
            // Keep a single place to report failures; stack trace is fine for a one-shot tool.
            e.printStackTrace(System.err);
        } finally {
            ctx.close();
        }

        System.exit(exitCode);
    }
}
