package springVibe.dev.users.youtubeComment.sentiment;

import java.util.Map;

public final class SentimentLexicon {
    private final Map<String, Integer> unigrams;
    private final Map<Integer, Map<String, Integer>> ngramsByLen;
    private final int maxNgramLen;

    public SentimentLexicon(
        Map<String, Integer> unigrams,
        Map<Integer, Map<String, Integer>> ngramsByLen,
        int maxNgramLen
    ) {
        this.unigrams = unigrams;
        this.ngramsByLen = ngramsByLen;
        this.maxNgramLen = maxNgramLen;
    }

    public Map<String, Integer> getUnigrams() {
        return unigrams;
    }

    public Map<Integer, Map<String, Integer>> getNgramsByLen() {
        return ngramsByLen;
    }

    public int getMaxNgramLen() {
        return maxNgramLen;
    }
}

