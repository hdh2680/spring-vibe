package springVibe.dev.users.devSearch.search;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Search index document for posts stored in DB table {@code velog_post}.
 *
 * Notes:
 * - createIndex=false so the application can boot even when Elasticsearch is down.
 * - Index is created lazily by service logic when search/index endpoints are called.
 */
@Document(indexName = "dev-search-posts", createIndex = false)
public class VelogPostDocument {
    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String body;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
