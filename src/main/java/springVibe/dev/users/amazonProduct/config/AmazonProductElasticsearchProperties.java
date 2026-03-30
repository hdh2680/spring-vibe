package springVibe.dev.users.amazonProduct.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "amazon-product.elasticsearch")
public class AmazonProductElasticsearchProperties {
    private boolean enabled;
    private Index index = new Index();
    private Reindex reindex = new Reindex();

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

    public Reindex getReindex() {
        return reindex;
    }

    public void setReindex(Reindex reindex) {
        this.reindex = reindex;
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

    public static class Reindex {
        /**
         * Batch size for DB -> ES reindexing. Higher is faster but uses more memory.
         */
        private int batchSize = 2000;

        /**
         * Log progress every N indexed docs.
         */
        private int logEvery = 20000;

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getLogEvery() {
            return logEvery;
        }

        public void setLogEvery(int logEvery) {
            this.logEvery = logEvery;
        }
    }
}
