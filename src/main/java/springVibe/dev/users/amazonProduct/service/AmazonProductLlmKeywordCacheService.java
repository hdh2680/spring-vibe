package springVibe.dev.users.amazonProduct.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import springVibe.dev.users.chat.service.OllamaChatService;
import springVibe.system.cache.CacheNames;

import java.util.List;

@Service
public class AmazonProductLlmKeywordCacheService {
    private static final Logger log = LoggerFactory.getLogger(AmazonProductLlmKeywordCacheService.class);

    // Copy-friendly version: docs/prompts/amazon_product_keywords_system_prompt.txt
    private static final String AMAZON_KEYWORDS_SYSTEM_PROMPT = """
        You are an Amazon search keyword generator.

        If the input is Korean:
        - Translate it into natural English product terms.

        If the input is English:
        - Normalize spelling.

        Generate Amazon search keywords (4 to 6 phrases).

        STRICT OUTPUT FORMAT:
        - Output ONLY one line.
        - Format EXACTLY like this:
          keyword1, keyword2, keyword3, keyword4
        - Each keyword MUST be separated by ", " (comma + space).
        - NEVER omit the space after commas.
        - NEVER use hyphens (-).
        - NEVER merge words with symbols.

        TEXT RULES:
        - Use lowercase only.
        - Use natural space-separated phrases.
        - Use correct English grammar (e.g., "women's shoes", not "women s shoes").
        - Use plural product forms (e.g., shoes, not shoe).
        - Avoid returning only gender variants (e.g., just "women's shoes, men's shoes") unless the input explicitly asks for it.

        WORD ORDER:
        - If a brand/model is mentioned, place it first.
        - Otherwise, omit brand.
        - Recommended format: [brand] + modifier + product
          (e.g., "nike running shoes", "women's shoes")

        BRAND RULES:
        - If a brand/model is explicitly mentioned, keep it exactly as-is.
        - Do NOT invent brands.
        - Do NOT introduce other brands.

        CATEGORY RULES:
        - Stay within the same product category only.
        - Do NOT expand beyond the original product type.

        KEYWORD QUALITY:
        - Use common Amazon search terms.
        - Include high-intent modifiers (running, training, walking).
        - Allow men's, women's, kids.
        - Do NOT use weak modifiers (durable, best, cheap).
        - For generic inputs, include common category synonyms/types where appropriate.
          Example (신발): shoes, sneakers, running shoes, athletic shoes, walking shoes

        LANGUAGE:
        - English ONLY.
        - No mixed languages.

        FINAL CHECK:
        - If output contains hyphens, missing spaces after commas, or broken grammar -> regenerate.
        """;

    private final ObjectProvider<OllamaChatService> ollamaProvider;

    public AmazonProductLlmKeywordCacheService(ObjectProvider<OllamaChatService> ollamaProvider) {
        this.ollamaProvider = ollamaProvider;
    }

    @Cacheable(
        cacheNames = CacheNames.AMAZON_PRODUCT_LLM_KEYWORDS,
        key = "'v1:' + #q",
        sync = true
    )
    public String translateHangulQueryToEnglishKeywordsCached(String q) {
        if (q == null || q.isBlank()) return null;
        if (!containsHangul(q)) return null;

        OllamaChatService ollama = ollamaProvider.getIfAvailable();
        if (ollama == null || !ollama.isHealthy()) {
            // Do not cache fallback; allow re-try when Ollama comes back.
            return null;
        }

        try {
            List<OllamaChatService.Message> msgs = List.of(
                OllamaChatService.Message.system(AMAZON_KEYWORDS_SYSTEM_PROMPT),
                OllamaChatService.Message.user(q)
            );

            String out = ollama.chat(msgs, new OllamaChatService.Options(0.0, 0.9, 32));
            out = sanitizeSingleLine(out);
            out = postProcessOllamaKeywordLine(out);
            if (out.isBlank()) return null;
            if (containsHangulOrCjk(out)) return null;
            return out;
        } catch (Exception e) {
            // Treat any LLM/network/parse error as cache-miss and let caller fallback.
            log.warn("AmazonProduct LLM keyword generation failed (ignored). qLen={}", q == null ? 0 : q.length(), e);
            return null;
        }
    }

    private static boolean containsHangul(String s) {
        return s != null && s.matches(".*[\\uAC00-\\uD7A3].*");
    }

    private static boolean containsHangulOrCjk(String s) {
        if (s == null || s.isEmpty()) return false;
        return s.matches(".*[\\uAC00-\\uD7A3\\u4E00-\\u9FFF].*");
    }

    private static String sanitizeSingleLine(String s) {
        if (s == null) return "";
        String x = s.trim();
        if ((x.startsWith("\"") && x.endsWith("\"")) || (x.startsWith("'") && x.endsWith("'"))) {
            x = x.substring(1, x.length() - 1).trim();
        }
        String[] lines = x.split("\\r?\\n");
        for (String ln : lines) {
            String t = ln.trim();
            if (!t.isEmpty()) return t;
        }
        return "";
    }

    private static String postProcessOllamaKeywordLine(String output) {
        if (output == null) return "";
        String out = output;

        // 1. hyphen -> space
        out = out.replace("-", " ");

        // 2. women s -> women's (same for men)
        out = out.replaceAll("\\bwomen s\\b", "women's");
        out = out.replaceAll("\\bmen s\\b", "men's");

        // 3. fix comma spacing
        out = out.replaceAll(",\\s*", ", ");

        // 4. collapse spaces
        out = out.replaceAll("\\s+", " ").trim();

        return out;
    }
}
