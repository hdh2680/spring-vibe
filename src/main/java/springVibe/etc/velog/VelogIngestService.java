package springVibe.etc.velog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@EnableConfigurationProperties(VelogIngestProperties.class)
public class VelogIngestService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private record FetchResult(VelogPostEntity entity, boolean isPrivate) {}

    private final VelogIngestProperties props;
    private final VelogPostRepository repository;
    private final HttpClient httpClient;
    private final List<Pattern> blockPatterns;
    private final List<Pattern> allowPatterns;

    public VelogIngestService(VelogIngestProperties props, VelogPostRepository repository) {
        this.props = props;
        this.repository = repository;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.blockPatterns = compileBlockPatterns(props.getBlockRegexes());
        this.allowPatterns = compileBlockPatterns(props.getAllowRegexes());
    }

    public void ingest() throws Exception {
        URI endpoint = URI.create(props.getEndpoint());

        int ingested = 0;
        int skippedExisting = 0;
        int skippedPrivate = 0;
        int skippedSpam = 0;
        int errors = 0;
        int scanned = 0;

        Set<String> seenIds = new HashSet<>();
        String cursor = (props.getResumeCursor() == null || props.getResumeCursor().isBlank())
                ? null
                : props.getResumeCursor().trim();

        while (ingested < props.getMaxPosts() && scanned < props.getMaxScanned()) {
            List<String> pageIds = fetchPostIds(endpoint, cursor, props.getPageSize());
            if (pageIds.isEmpty()) break;

            // Defensive: avoid infinite loops if API repeats the same page.
            boolean anyNew = false;
            for (String id : pageIds) {
                if (id != null && seenIds.add(id)) {
                    anyNew = true;
                }
            }
            if (!anyNew) break;

            for (String id : pageIds) {
                if (id == null || id.isBlank()) continue;
                if (ingested >= props.getMaxPosts() || scanned >= props.getMaxScanned()) break;
                scanned++;

                if (props.isSkipExisting() && repository.existsById(id)) {
                    skippedExisting++;
                    continue;
                }

                try {
                    FetchResult res = fetchPostDetail(endpoint, id);
                    if (res == null) {
                        errors++;
                        continue;
                    }
                    if (res.isPrivate()) {
                        skippedPrivate++;
                        continue;
                    }
                    VelogPostEntity post = res.entity();

                    if (post == null
                            || post.getId() == null
                            || post.getUsername() == null
                            || post.getUrlSlug() == null
                            || post.getTitle() == null) {
                        errors++;
                        continue;
                    }

                    if (props.isSpamFilterEnabled() && isSpam(post)) {
                        skippedSpam++;
                        continue;
                    }

                    saveOne(post);
                    ingested++;
                } catch (Exception e) {
                    errors++;
                    // Keep going; one-shot ingestion should be best-effort.
                    e.printStackTrace(System.err);
                } finally {
                    sleepQuietly(props.getSleepMs());
                }
            }

            cursor = pageIds.get(pageIds.size() - 1);
        }

        System.out.printf(
                "Velog ingest finished. scanned=%d ingested=%d skippedExisting=%d skippedPrivate=%d skippedSpam=%d errors=%d%n",
                scanned, ingested, skippedExisting, skippedPrivate, skippedSpam, errors
        );
    }

    private List<String> fetchPostIds(URI endpoint, String cursor, int limit) throws Exception {
        String query = "query($cursor: ID, $limit: Int){ posts(cursor: $cursor, limit: $limit){ id } }";
        com.fasterxml.jackson.databind.node.ObjectNode variables = MAPPER.createObjectNode();
        variables.put("limit", limit);
        if (cursor == null) variables.putNull("cursor");
        else variables.put("cursor", cursor);

        JsonNode root = gql(endpoint, query, variables);
        JsonNode posts = root.path("data").path("posts");
        if (!posts.isArray()) return List.of();

        List<String> ids = new ArrayList<>(posts.size());
        for (JsonNode n : posts) {
            String id = n.path("id").asText(null);
            if (id != null) ids.add(id);
        }
        return ids;
    }

    private FetchResult fetchPostDetail(URI endpoint, String id) throws Exception {
        String query = "query($id: ID!){ post(id: $id){ id title url_slug short_description thumbnail released_at updated_at is_private likes comments_count tags user { username } body } }";
        JsonNode variables = MAPPER.createObjectNode().put("id", id);

        JsonNode root = gql(endpoint, query, variables);
        JsonNode post = root.path("data").path("post");
        if (post.isMissingNode() || post.isNull()) return null;

        boolean isPrivate = post.path("is_private").asBoolean(false);
        if (isPrivate) return new FetchResult(null, true);

        String postId = post.path("id").asText(null);
        String title = post.path("title").asText(null);
        String urlSlug = post.path("url_slug").asText(null);
        String shortDescription = post.path("short_description").asText(null);
        String thumbnail = post.path("thumbnail").asText(null);
        Instant releasedAt = parseInstant(post.path("released_at").asText(null));
        Instant updatedAt = parseInstant(post.path("updated_at").asText(null));
        Integer likes = post.path("likes").isNumber() ? post.path("likes").intValue() : null;
        Integer commentsCount = post.path("comments_count").isNumber() ? post.path("comments_count").intValue() : null;
        String username = post.path("user").path("username").asText(null);
        String body = post.path("body").asText(null);

        List<String> tags = new ArrayList<>();
        JsonNode tagsNode = post.path("tags");
        if (tagsNode.isArray()) {
            for (JsonNode t : tagsNode) {
                String tag = t.asText(null);
                if (tag != null && !tag.isBlank()) tags.add(tag);
            }
        }

        VelogPostEntity entity = new VelogPostEntity(
                postId,
                username,
                urlSlug,
                title,
                shortDescription,
                thumbnail,
                releasedAt,
                updatedAt,
                likes,
                commentsCount,
                tags,
                body,
                Instant.now()
        );
        return new FetchResult(entity, false);
    }

    private JsonNode gql(URI endpoint, String query, JsonNode variables) throws Exception {
        com.fasterxml.jackson.databind.node.ObjectNode body = MAPPER.createObjectNode();
        body.put("query", query);
        if (variables != null) body.set("variables", variables);

        HttpRequest req = HttpRequest.newBuilder(endpoint)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Accept-Charset", "utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                .build();

        // Be explicit about decoding. Some endpoints omit charset headers and
        // we don't want platform-default encoding to ever leak in.
        HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() / 100 != 2) {
            String raw = new String(resp.body(), StandardCharsets.UTF_8);
            throw new IllegalStateException("GraphQL HTTP " + resp.statusCode() + ": " + raw);
        }

        String raw = new String(resp.body(), StandardCharsets.UTF_8);
        JsonNode root = MAPPER.readTree(raw);
        JsonNode errors = root.path("errors");
        if (errors.isArray() && errors.size() > 0) {
            throw new IllegalStateException("GraphQL errors: " + errors.toString());
        }
        return root;
    }

    @Transactional
    public void saveOne(VelogPostEntity entity) {
        repository.save(entity);
    }

    private static Instant parseInstant(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return OffsetDateTime.parse(iso).toInstant();
        } catch (Exception e) {
            return null;
        }
    }

    private static void sleepQuietly(long ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isSpam(VelogPostEntity post) {
        String title = safe(post.getTitle());
        String slug = safe(post.getUrlSlug());
        String body = safe(post.getBody());

        // 1) Hard block patterns (keywords / fullwidth digits).
        String joined = title + "\n" + slug + "\n" + body;
        for (Pattern p : blockPatterns) {
            if (p.matcher(joined).find()) return true;
        }

        // 2) Body too short is usually low value for search dataset.
        if (body.length() < props.getMinBodyLength()) return true;

        // 3) Character ratio heuristic (Korean-focused dataset).
        ScriptRatio r = computeScriptRatio(title + "\n" + body);
        if (r.letterOrDigitCount == 0) return true;
        if (r.hangulRatio() < props.getMinHangulRatio()) {
            // Allow-list can bypass the hangul ratio filter (for English technical posts).
            for (Pattern p : allowPatterns) {
                if (p.matcher(joined).find()) return false;
            }
            // If it's mostly Han ideographs, treat as spam for this dataset.
            if (r.hanRatio() > props.getMaxHanRatio()) return true;
            // Otherwise (no Korean, not allow-listed): likely not the dataset we want.
            return true;
        }

        return false;
    }

    private static List<Pattern> compileBlockPatterns(List<String> regexes) {
        if (regexes == null || regexes.isEmpty()) return List.of();
        List<Pattern> out = new ArrayList<>(regexes.size());
        for (String r : regexes) {
            if (r == null || r.isBlank()) continue;
            out.add(Pattern.compile(r));
        }
        return out;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private record ScriptRatio(int letterOrDigitCount, int hangulCount, int hanCount) {
        double hangulRatio() {
            return letterOrDigitCount == 0 ? 0.0 : (double) hangulCount / (double) letterOrDigitCount;
        }

        double hanRatio() {
            return letterOrDigitCount == 0 ? 0.0 : (double) hanCount / (double) letterOrDigitCount;
        }
    }

    private static ScriptRatio computeScriptRatio(String s) {
        if (s == null || s.isEmpty()) return new ScriptRatio(0, 0, 0);
        int lod = 0;
        int hangul = 0;
        int han = 0;
        int i = 0;
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if (!Character.isLetterOrDigit(cp)) continue;
            lod++;
            if (cp >= 0xAC00 && cp <= 0xD7A3) hangul++;          // Hangul syllables
            if (cp >= 0x4E00 && cp <= 0x9FFF) han++;             // CJK Unified Ideographs
        }
        return new ScriptRatio(lod, hangul, han);
    }
}
