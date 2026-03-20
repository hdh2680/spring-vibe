package springVibe.dev.users.youtubeComment.sentiment;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Set;

@Service
public class SentimentAnalyzer {
    public enum Label {
        POSITIVE,
        NEUTRAL,
        NEGATIVE
    }

    public record Result(int score, Label label, int matched) {
    }

    private final SentimentLexiconLoader lexiconLoader;

    // Heuristic helpers for MVP lexicon matching (Korean comments are highly inflected).
    private static final Pattern EDGE_PUNCT = Pattern.compile("^[\\p{IsPunctuation}\\p{Punct}]+|[\\p{IsPunctuation}\\p{Punct}]+$");
    private static final String[] KOR_FALLBACK_SUFFIXES = new String[]{
        // longer first
        "했습니다", "합니다", "합니", "습니다",
        "였습니다", "입니다", "이네요",
        "였어요", "었어요", "았어요", "어요", "아요",
        "군요", "지요", "죠", "네요",
        "임요", "임다", "임",
        // particles / short endings (keep last)
        "에서", "으로", "까지", "부터", "라도", "라도",
        "하고", "랑", "과", "와",
        "은", "는", "이", "가", "을", "를", "도", "만", "에", "로",
        "요", "다", "네", "음"
    };
    private static final Set<String> NEGATORS = Set.of(
        "안", "못", "전혀", "절대", "별로", "아니", "아니다", "없", "없다"
    );

    public SentimentAnalyzer(SentimentLexiconLoader lexiconLoader) {
        this.lexiconLoader = lexiconLoader;
    }

    public Result analyze(String text) {
        SentimentLexicon lex = lexiconLoader.get();
        if (text == null || text.isBlank()) {
            return new Result(0, Label.NEUTRAL, 0);
        }

        List<String> toks = tokenizeByWhitespace(text);
        if (toks.isEmpty()) {
            return new Result(0, Label.NEUTRAL, 0);
        }

        ScoreHit sh = scoreByMaxNgramMatch(toks, lex);
        return new Result(sh.score, labelOf(sh.score), sh.matched);
    }

    private record ScoreHit(int score, int matched) {
    }

    private static ScoreHit scoreByMaxNgramMatch(List<String> toks, SentimentLexicon lex) {
        Map<String, Integer> uni = lex.getUnigrams();
        Map<Integer, Map<String, Integer>> ngramsByLen = lex.getNgramsByLen();
        int maxN = Math.max(1, lex.getMaxNgramLen());

        int score = 0;
        int matched = 0;
        int i = 0;
        int negateTtl = 0; // one-shot negation: invert only the immediately following token/ngram
        while (i < toks.size()) {
            int matchedLen = 0;
            int matchedScore = 0;

            String ti = toks.get(i);
            if (ti != null && NEGATORS.contains(ti)) {
                negateTtl = 1;
                i++;
                continue;
            }
            boolean negate = negateTtl > 0;

            for (int n = Math.min(maxN, toks.size() - i); n >= 2; n--) {
                Map<String, Integer> bucket = ngramsByLen.get(n);
                if (bucket == null || bucket.isEmpty()) {
                    continue;
                }
                String phrase = join(toks, i, i + n);
                Integer s = bucket.get(phrase);
                if (s == null) {
                    s = bucket.get(phrase.toLowerCase(Locale.ROOT));
                }
                if (s != null) {
                    matchedLen = n;
                    matchedScore = s;
                    break;
                }
            }

            if (matchedLen > 0) {
                int add = matchedScore;
                if (negate && add != 0) {
                    add = -add;
                }
                score += add;
                matched++;
                if (negateTtl > 0) {
                    negateTtl--;
                }
                i += matchedLen;
                continue;
            }

            String t = toks.get(i);
            Integer s = lookupUnigramScoreWithNegPrefix(t, uni);
            if (s != null) {
                int add = s;
                if (negate && add != 0) {
                    add = -add;
                }
                score += add;
                matched++;
            }
            if (negateTtl > 0) {
                negateTtl--;
            }
            i++;
        }
        return new ScoreHit(score, matched);
    }

    private static String join(List<String> toks, int startInclusive, int endExclusive) {
        if (endExclusive - startInclusive == 1) {
            return toks.get(startInclusive);
        }
        StringBuilder sb = new StringBuilder(64);
        for (int i = startInclusive; i < endExclusive; i++) {
            if (i > startInclusive) {
                sb.append(' ');
            }
            sb.append(toks.get(i));
        }
        return sb.toString();
    }

    private static List<String> tokenizeByWhitespace(String text) {
        String normalized = text.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return List.of();
        }
        String[] parts = normalized.split(" ");
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            if (p == null) {
                continue;
            }
            String t = normalizeToken(p);
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    private static String normalizeToken(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return "";
        }

        // strip punctuation at edges (keeps inner punctuation like "ㅠㅠ", "ㅋㅋ" as-is)
        t = EDGE_PUNCT.matcher(t).replaceAll("");
        if (t.isEmpty()) {
            return "";
        }

        return t;
    }

    private static Integer lookupUnigramScoreWithNegPrefix(String token, Map<String, Integer> uni) {
        Integer direct = lookupUnigramScore(token, uni);
        if (direct != null) {
            return direct;
        }
        if (token == null || token.length() < 3) {
            return null;
        }

        // Handle "안좋다", "못한다" style 붙임표현 as a last resort.
        if (token.startsWith("안") || token.startsWith("못")) {
            String base = token.substring(1);
            Integer baseScore = lookupUnigramScore(base, uni);
            if (baseScore != null && baseScore != 0) {
                return -baseScore;
            }
        }
        return null;
    }

    private static Integer lookupUnigramScore(String token, Map<String, Integer> uni) {
        if (token == null || token.isBlank()) {
            return null;
        }

        for (String c : unigramCandidates(token)) {
            Integer s = uni.get(c);
            if (s != null) {
                return s;
            }
            String lower = c.toLowerCase(Locale.ROOT);
            s = uni.get(lower);
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    private static List<String> unigramCandidates(String token) {
        List<String> out = new ArrayList<>(10);
        addCandidate(out, token);

        String stripped = EDGE_PUNCT.matcher(token).replaceAll("");
        addCandidate(out, stripped);

        if (stripped.length() >= 3 && stripped.startsWith("개")) {
            addCandidate(out, stripped.substring(1));
        }

        for (String suf : KOR_FALLBACK_SUFFIXES) {
            if (stripped.length() <= suf.length() + 1) {
                continue;
            }
            if (stripped.endsWith(suf)) {
                addCandidate(out, stripped.substring(0, stripped.length() - suf.length()));
            }
        }

        return out;
    }

    private static void addCandidate(List<String> out, String s) {
        if (s == null) {
            return;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return;
        }
        for (String x : out) {
            if (x.equals(t)) {
                return;
            }
        }
        out.add(t);
    }

    private static Label labelOf(int score) {
        // MVP thresholds (tunable): treat 0 as neutral, positive if >= 1, negative if <= -1.
        if (score >= 1) {
            return Label.POSITIVE;
        }
        if (score <= -1) {
            return Label.NEGATIVE;
        }
        return Label.NEUTRAL;
    }
}
