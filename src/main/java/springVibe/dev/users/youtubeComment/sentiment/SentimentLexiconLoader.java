package springVibe.dev.users.youtubeComment.sentiment;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import springVibe.system.exception.BaseException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class SentimentLexiconLoader {
    private static final String DEFAULT_LEXICON_PATH = "static/docs/youtubeComment/sentiment_lexicon.tsv";
    private static final String CUSTOM_LEXICON_PATH = "static/docs/youtubeComment/sentiment_custom.tsv";

    private volatile SentimentLexicon lexicon;

    public SentimentLexicon get() {
        SentimentLexicon l = lexicon;
        if (l == null) {
            synchronized (this) {
                if (lexicon == null) {
                    lexicon = loadWithOptionalCustom(DEFAULT_LEXICON_PATH, CUSTOM_LEXICON_PATH);
                }
                l = lexicon;
            }
        }
        return l;
    }

    private static SentimentLexicon loadWithOptionalCustom(String basePath, String customPath) {
        ClassPathResource base = new ClassPathResource(basePath);
        if (!base.exists()) {
            throw new BaseException("SENTIMENT_LEXICON_MISSING", "Sentiment lexicon not found on classpath: " + basePath);
        }

        Map<String, Integer> unigrams = new HashMap<>(16_384);
        Map<Integer, Map<String, Integer>> ngramsByLen = new HashMap<>();
        int[] maxN = new int[]{1};

        loadInto(basePath, base, unigrams, ngramsByLen, maxN);

        if (customPath != null && !customPath.isBlank()) {
            ClassPathResource custom = new ClassPathResource(customPath);
            if (custom.exists()) {
                // Overlay overrides to allow iterative domain tuning without rebuilding base lexicon.
                loadInto(customPath, custom, unigrams, ngramsByLen, maxN);
            }
        }

        return new SentimentLexicon(unigrams, ngramsByLen, maxN[0]);
    }

    private static void loadInto(String path, ClassPathResource res,
                                Map<String, Integer> unigrams,
                                Map<Integer, Map<String, Integer>> ngramsByLen,
                                int[] maxN) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\t");
                if (parts.length < 2) {
                    continue;
                }
                String term = parts[0].trim();
                if (term.isEmpty()) {
                    continue;
                }
                int score;
                try {
                    score = Integer.parseInt(parts[1].trim());
                } catch (Exception ignored) {
                    continue;
                }

                int n = countWhitespaceSeparatedTokens(term);
                maxN[0] = Math.max(maxN[0], n);
                if (n <= 1) {
                    unigrams.put(term, score);
                    unigrams.put(term.toLowerCase(Locale.ROOT), score);
                } else {
                    Map<String, Integer> bucket = ngramsByLen.computeIfAbsent(n, k -> new HashMap<>(2048));
                    bucket.put(term, score);
                    bucket.put(term.toLowerCase(Locale.ROOT), score);
                }
            }
        } catch (Exception e) {
            throw new BaseException("SENTIMENT_LEXICON_LOAD_FAILED", "Failed to load sentiment lexicon: " + path, e);
        }
    }

    private static int countWhitespaceSeparatedTokens(String s) {
        int n = 0;
        boolean inToken = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ws = Character.isWhitespace(c);
            if (ws) {
                if (inToken) {
                    inToken = false;
                }
            } else {
                if (!inToken) {
                    inToken = true;
                    n++;
                }
            }
        }
        return Math.max(1, n);
    }
}
