package springVibe.etc.velog;

import java.time.Instant;
import java.util.List;

/**
 * Lightweight projection for list/search results so we don't always load LONGTEXT body.
 */
public interface VelogPostSummary {
    String getId();

    String getUsername();

    String getUrlSlug();

    String getTitle();

    String getShortDescription();

    String getThumbnail();

    Instant getReleasedAt();

    Instant getUpdatedAt();

    Integer getLikes();

    Integer getCommentsCount();

    List<String> getTags();
}
