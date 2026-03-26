package springVibe.etc.velog;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for one-shot ingestion.
 *
 * Override via args, e.g.
 *   --velog.ingest.max-posts=10000 --velog.ingest.page-size=100 --velog.ingest.sleep-ms=150
 */
@ConfigurationProperties(prefix = "velog.ingest")
public class VelogIngestProperties {

    /**
     * GraphQL endpoint.
     */
    private String endpoint = "https://v2.velog.io/graphql";

    /**
     * Page size for posts() listing.
     */
    private int pageSize = 50;

    /**
     * Maximum number of posts to ingest in this run (safety cap).
     */
    private int maxPosts = 5000;

    /**
     * Maximum number of post IDs to scan (list items) in this run.
     * This prevents extremely long runs when spam filtering skips most items.
     */
    private int maxScanned = 50000;

    /**
     * Sleep between GraphQL calls (ms).
     */
    private long sleepMs = 150;

    /**
     * If true, skip fetching details for posts that already exist in DB.
     */
    private boolean skipExisting = true;

    /**
     * Enable basic spam filtering (heuristics).
     */
    private boolean spamFilterEnabled = true;

    /**
     * Minimum hangul ratio among letter/digit characters in (title + body).
     * Useful to exclude obvious non-Korean spam when you want a Korean-focused dataset.
     */
    private double minHangulRatio = 0.03;

    /**
     * Maximum CJK (Han) ratio among letter/digit characters in (title + body).
     * If content is mostly Chinese characters, it's likely spam for this use case.
     */
    private double maxHanRatio = 0.70;

    /**
     * Minimum body length (characters) to accept. Helps exclude ultra-short spam.
     */
    private int minBodyLength = 80;

    /**
     * Regexes; if any matches title/url/body, the post is skipped.
     */
    private java.util.List<String> blockRegexes = new java.util.ArrayList<>(java.util.List.of(
            "微信",
            "[\\uFF10-\\uFF19]{6,}",       // fullwidth digits, e.g. １００８６０８２
            "高仿|精仿|一比一|A货|复刻|代购|官网",
            // English SEO/report spam patterns frequently seen in open feeds.
            "(?i)market\\s*size|market\\s*share|cagr|forecast|opportunity\\s*analysis|industry\\s*(report|overview)|key\\s*drivers|usd\\s*\\d"
    ));

    /**
     * Regexes; if matches, bypasses hangul-ratio filter even when content has little Korean.
     * (Useful if you still want English technical posts.)
     */
    private java.util.List<String> allowRegexes = new java.util.ArrayList<>(java.util.List.of(
            "(?i)\\bjava\\b",
            "(?i)\\bspring\\b",
            "(?i)\\bbackend\\b",
            "(?i)\\bapi\\b",
            "(?i)\\bdocker\\b",
            "(?i)\\bkubernetes\\b",
            "백준|코딩테스트|알고리즘|자료구조|네트워크|정처기"
    ));

    /**
     * Optional cursor (post id) to resume pagination from.
     * If empty, starts from the newest page.
     */
    private String resumeCursor = null;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getMaxPosts() {
        return maxPosts;
    }

    public void setMaxPosts(int maxPosts) {
        this.maxPosts = maxPosts;
    }

    public int getMaxScanned() {
        return maxScanned;
    }

    public void setMaxScanned(int maxScanned) {
        this.maxScanned = maxScanned;
    }

    public long getSleepMs() {
        return sleepMs;
    }

    public void setSleepMs(long sleepMs) {
        this.sleepMs = sleepMs;
    }

    public boolean isSkipExisting() {
        return skipExisting;
    }

    public void setSkipExisting(boolean skipExisting) {
        this.skipExisting = skipExisting;
    }

    public boolean isSpamFilterEnabled() {
        return spamFilterEnabled;
    }

    public void setSpamFilterEnabled(boolean spamFilterEnabled) {
        this.spamFilterEnabled = spamFilterEnabled;
    }

    public double getMinHangulRatio() {
        return minHangulRatio;
    }

    public void setMinHangulRatio(double minHangulRatio) {
        this.minHangulRatio = minHangulRatio;
    }

    public double getMaxHanRatio() {
        return maxHanRatio;
    }

    public void setMaxHanRatio(double maxHanRatio) {
        this.maxHanRatio = maxHanRatio;
    }

    public int getMinBodyLength() {
        return minBodyLength;
    }

    public void setMinBodyLength(int minBodyLength) {
        this.minBodyLength = minBodyLength;
    }

    public java.util.List<String> getBlockRegexes() {
        return blockRegexes;
    }

    public void setBlockRegexes(java.util.List<String> blockRegexes) {
        this.blockRegexes = blockRegexes;
    }

    public java.util.List<String> getAllowRegexes() {
        return allowRegexes;
    }

    public void setAllowRegexes(java.util.List<String> allowRegexes) {
        this.allowRegexes = allowRegexes;
    }

    public String getResumeCursor() {
        return resumeCursor;
    }

    public void setResumeCursor(String resumeCursor) {
        this.resumeCursor = resumeCursor;
    }
}
