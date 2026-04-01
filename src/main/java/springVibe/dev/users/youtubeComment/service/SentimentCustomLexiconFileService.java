package springVibe.dev.users.youtubeComment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import springVibe.dev.users.youtubeComment.sentiment.SentimentLexiconLoader;
import springVibe.system.exception.BaseException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.ClassPathResource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class SentimentCustomLexiconFileService {
    private static final String REL_PATH = "youtubeComment/sentiment_custom.tsv";

    private final Path customPath;
    private final SentimentLexiconLoader lexiconLoader;

    public SentimentCustomLexiconFileService(
        @Value("${app.storage.attachments-dir:}") String attachmentsDir,
        SentimentLexiconLoader lexiconLoader
    ) {
        if (attachmentsDir == null || attachmentsDir.isBlank()) {
            // Keep consistent with app config; this should be set in application.yml already.
            throw new BaseException("ATTACHMENTS_DIR_MISSING", "app.storage.attachments-dir is required for custom lexicon editing.");
        }
        this.customPath = Path.of(attachmentsDir).resolve(REL_PATH);
        this.lexiconLoader = lexiconLoader;
    }

    public Path getCustomPath() {
        return customPath;
    }

    public String readRawTsv() {
        ensureFileExists();
        try {
            return Files.readString(customPath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BaseException("CUSTOM_LEXICON_READ_FAILED", "Failed to read custom lexicon.", e);
        }
    }

    public void upsert(String term, int score) {
        String t = term == null ? "" : term.trim();
        if (t.isBlank()) {
            throw new BaseException("CUSTOM_LEXICON_TERM_REQUIRED", "term is required.");
        }
        if (t.contains("\t") || t.contains("\r") || t.contains("\n")) {
            throw new BaseException("CUSTOM_LEXICON_TERM_INVALID", "term must not contain tabs or newlines.");
        }
        if (score < -10 || score > 10) {
            throw new BaseException("CUSTOM_LEXICON_SCORE_INVALID", "score must be between -10 and 10.");
        }

        ensureFileExists();
        List<String> lines = readLines();

        boolean updated = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isBlank() || line.startsWith("#")) {
                continue;
            }
            int tab = line.indexOf('\t');
            if (tab < 0) {
                continue;
            }
            String existing = line.substring(0, tab).trim();
            if (existing.equals(t)) {
                lines.set(i, t + "\t" + score);
                updated = true;
            }
        }

        if (!updated) {
            // Append at end (keep comments and previous ordering).
            if (!lines.isEmpty() && !lines.get(lines.size() - 1).isBlank()) {
                lines.add("");
            }
            lines.add(t + "\t" + score);
        }

        writeLines(lines);
        lexiconLoader.reload();
    }

    public void delete(String term) {
        String t = term == null ? "" : term.trim();
        if (t.isBlank()) {
            throw new BaseException("CUSTOM_LEXICON_TERM_REQUIRED", "term is required.");
        }

        ensureFileExists();
        List<String> lines = readLines();
        List<String> out = new ArrayList<>(lines.size());
        boolean removed = false;

        for (String line : lines) {
            if (line == null || line.isBlank() || line.startsWith("#")) {
                out.add(line);
                continue;
            }
            int tab = line.indexOf('\t');
            if (tab < 0) {
                out.add(line);
                continue;
            }
            String existing = line.substring(0, tab).trim();
            if (existing.equals(t)) {
                removed = true;
                continue;
            }
            out.add(line);
        }

        if (!removed) {
            return;
        }
        writeLines(out);
        lexiconLoader.reload();
    }

    public boolean containsTerm(String term) {
        String t = term == null ? "" : term.trim();
        if (t.isBlank()) {
            return false;
        }
        ensureFileExists();
        List<String> lines = readLines();
        for (String line : lines) {
            if (line == null || line.isBlank() || line.startsWith("#")) {
                continue;
            }
            int tab = line.indexOf('\t');
            if (tab < 0) {
                continue;
            }
            String existing = line.substring(0, tab).trim();
            if (existing.equals(t)) {
                return true;
            }
        }
        return false;
    }

    private void ensureFileExists() {
        try {
            Files.createDirectories(customPath.getParent());
            if (!Files.exists(customPath)) {
                // Seed from classpath custom lexicon if present, so existing tuning isn't lost.
                ClassPathResource cp = new ClassPathResource("static/docs/youtubeComment/sentiment_custom.tsv");
                if (cp.exists()) {
                    Files.copy(cp.getInputStream(), customPath);
                } else {
                    String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    String seed = ""
                        + "# term\tscore\n"
                        + "# generated_at=" + now + "\n"
                        + "# Use integer scores (recommended: -1/0/+1).\n"
                        + "# Whitespace-separated multi-word phrases are supported (n-grams).\n\n";
                    Files.writeString(customPath, seed, StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            throw new BaseException("CUSTOM_LEXICON_INIT_FAILED", "Failed to initialize custom lexicon file.", e);
        }
    }

    private List<String> readLines() {
        try {
            return Files.readAllLines(customPath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BaseException("CUSTOM_LEXICON_READ_FAILED", "Failed to read custom lexicon.", e);
        }
    }

    private void writeLines(List<String> lines) {
        try {
            Files.write(customPath, lines, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BaseException("CUSTOM_LEXICON_WRITE_FAILED", "Failed to write custom lexicon.", e);
        }
    }
}
