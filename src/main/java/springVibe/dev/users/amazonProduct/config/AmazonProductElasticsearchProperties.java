package springVibe.dev.users.amazonProduct.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "amazon-product.elasticsearch")
public class AmazonProductElasticsearchProperties {
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
        private String products = "amazon-products";

        public String getProducts() {
            return products;
        }

        public void setProducts(String products) {
            this.products = products;
        }
    }
}

