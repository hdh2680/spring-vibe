package springVibe.dev.users.devSearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dev-search.elasticsearch")
public class DevSearchElasticsearchProperties {
    private boolean enabled;
    private Index index = new Index();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Index getIndex() {
        return index;
    }

    public void setIndex(Index index) {
        this.index = index;
    }

    public static class Index {
        // Elasticsearch index names must be lowercase.
        private String posts = "dev-search-posts";

        public String getPosts() {
            return posts;
        }

        public void setPosts(String posts) {
            this.posts = posts;
        }
    }
}
