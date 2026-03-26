package springVibe.etc.velog;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "velog_post")
public class VelogPostEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(length = 100, nullable = false)
    private String username;

    @Column(length = 400, nullable = false)
    private String urlSlug;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String shortDescription;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String thumbnail;

    private Instant releasedAt;

    private Instant updatedAt;

    private Integer likes;

    private Integer commentsCount;

    @Convert(converter = StringListJsonConverter.class)
    @Lob
    @Column(columnDefinition = "TEXT")
    private List<String> tags;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String body;

    private Instant fetchedAt;

    protected VelogPostEntity() {
    }

    public VelogPostEntity(
            String id,
            String username,
            String urlSlug,
            String title,
            String shortDescription,
            String thumbnail,
            Instant releasedAt,
            Instant updatedAt,
            Integer likes,
            Integer commentsCount,
            List<String> tags,
            String body,
            Instant fetchedAt
    ) {
        this.id = id;
        this.username = username;
        this.urlSlug = urlSlug;
        this.title = title;
        this.shortDescription = shortDescription;
        this.thumbnail = thumbnail;
        this.releasedAt = releasedAt;
        this.updatedAt = updatedAt;
        this.likes = likes;
        this.commentsCount = commentsCount;
        this.tags = tags;
        this.body = body;
        this.fetchedAt = fetchedAt;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getUrlSlug() {
        return urlSlug;
    }

    public String getTitle() {
        return title;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Integer getLikes() {
        return likes;
    }

    public Integer getCommentsCount() {
        return commentsCount;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getBody() {
        return body;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }
}

