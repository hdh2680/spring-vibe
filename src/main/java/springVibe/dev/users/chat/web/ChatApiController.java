package springVibe.dev.users.chat.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import springVibe.dev.users.chat.service.OllamaChatService;
import springVibe.dev.users.chat.service.OllamaChatService.Message;

import java.util.ArrayList;
import java.util.List;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {
    private static final String SESSION_KEY = "chat.history.v1";
    private static final int MAX_HISTORY_MESSAGES = 20; // keep prompts bounded
    private static final Pattern TRANSLATE_TO_EN = Pattern.compile("(영어로\\s*번역|영문으로\\s*번역|translate\\s+to\\s+english)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASK_TODAY_DATE = Pattern.compile("(오늘\\s*몇\\s*일|오늘\\s*며\\s*칠|오늘\\s*날짜|오늘\\s*몇\\s*월\\s*몇\\s*일|what\\s*(day|date)\\s*is\\s*(it|today))", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASK_NOW_TIME = Pattern.compile("(지금\\s*몇\\s*시|현재\\s*시간|몇\\s*시\\s*야|what\\s*time\\s*is\\s*it)", Pattern.CASE_INSENSITIVE);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final OllamaChatService chat;

    public ChatApiController(OllamaChatService chat) {
        this.chat = chat;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse(chat.isHealthy());
    }

    @PostMapping
    public ChatReply chat(@RequestBody ChatRequest req, HttpSession session) {
        if (req == null || req.message() == null || req.message().isBlank()) {
            throw new BadRequestException("message is required");
        }

        String raw = req.message().trim();
        if (looksLikeTranslateToEnglish(raw)) {
            String source = extractTranslationSource(raw);
            String translated = translateToEnglishStrict(source);
            return new ChatReply(translated, 0);
        }
        if (looksLikeAskTodayDate(raw)) {
            return new ChatReply(formatTodayDateKst(), 0);
        }
        if (looksLikeAskNowTime(raw)) {
            return new ChatReply(formatNowTimeKst(), 0);
        }

        List<Message> history = getHistory(session);

        // System prompt (minimal, safe defaults).
        if (history.isEmpty()) {
            history.add(Message.system(
                "너는 내부 웹앱용 도우미 챗봇이다.\n" +
                "규칙:\n" +
                "- 기본 답변 언어는 한국어로 한다.\n" +
                "- 사용자가 특정 언어로 답변해달라고 요청한 경우에만 그 언어로 답한다.\n" +
                "- 번역 요청(예: \"OO를 영어로 번역해줘\")이면 설명을 붙이지 말고 번역 결과만 출력한다.\n" +
                "- 중국어/일본어 등 다른 언어로 섞어서 설명하지 않는다.\n" +
                "- 코드 요청이면 실용적인 코드/명령 위주로 간결하게 답한다."
            ));
        }

        history.add(Message.user(req.message().trim()));

        // Send last N messages (always keep system if present at index 0).
        List<Message> window = window(history, MAX_HISTORY_MESSAGES);
        String answer = chat.chat(window, pickOptionsFor(raw));
        history.add(Message.assistant(answer));

        // Persist trimmed history back to session.
        session.setAttribute(SESSION_KEY, trimForSession(history, 1 + MAX_HISTORY_MESSAGES * 2));

        return new ChatReply(answer, history.size());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
    }

    @SuppressWarnings("unchecked")
    private static List<Message> getHistory(HttpSession session) {
        Object v = session.getAttribute(SESSION_KEY);
        if (v instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Message) {
            return new ArrayList<>((List<Message>) list);
        }
        return new ArrayList<>();
    }

    private static List<Message> window(List<Message> history, int maxMessages) {
        if (history.size() <= maxMessages) return history;
        int start = Math.max(0, history.size() - maxMessages);
        // Keep the system message if it exists at index 0.
        if (!history.isEmpty() && "system".equals(history.get(0).role())) {
            if (start == 0) return history;
            List<Message> out = new ArrayList<>();
            out.add(history.get(0));
            out.addAll(history.subList(start, history.size()));
            return out;
        }
        return history.subList(start, history.size());
    }

    private static List<Message> trimForSession(List<Message> history, int maxSize) {
        if (history.size() <= maxSize) return history;
        int start = Math.max(0, history.size() - maxSize);
        if (!history.isEmpty() && "system".equals(history.get(0).role())) {
            if (start == 0) return history;
            List<Message> out = new ArrayList<>();
            out.add(history.get(0));
            out.addAll(history.subList(start, history.size()));
            return out;
        }
        return new ArrayList<>(history.subList(start, history.size()));
    }

    public record ChatRequest(String message) {}
    public record ChatReply(String reply, int historySize) {}
    public record HealthResponse(boolean ok) {}

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    private static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) {
            super(message);
        }
    }

    private static boolean looksLikeTranslateToEnglish(String msg) {
        return TRANSLATE_TO_EN.matcher(msg).find();
    }

    private static boolean looksLikeAskTodayDate(String msg) {
        return ASK_TODAY_DATE.matcher(msg == null ? "" : msg).find();
    }

    private static boolean looksLikeAskNowTime(String msg) {
        return ASK_NOW_TIME.matcher(msg == null ? "" : msg).find();
    }

    private static String formatTodayDateKst() {
        ZonedDateTime now = ZonedDateTime.now(KST);
        return String.format("오늘은 %d년 %d월 %d일입니다. (KST)", now.getYear(), now.getMonthValue(), now.getDayOfMonth());
    }

    private static String formatNowTimeKst() {
        ZonedDateTime now = ZonedDateTime.now(KST);
        String hhmm = now.format(DateTimeFormatter.ofPattern("HH:mm"));
        return String.format("현재 시간은 %s입니다. (KST)", hhmm);
    }

    private static String extractTranslationSource(String msg) {
        // Try quoted text first: "..." or '...'
        int dq1 = msg.indexOf('"');
        if (dq1 >= 0) {
            int dq2 = msg.indexOf('"', dq1 + 1);
            if (dq2 > dq1) {
                String inside = msg.substring(dq1 + 1, dq2).trim();
                if (!inside.isEmpty()) return inside;
            }
        }
        int sq1 = msg.indexOf('\'');
        if (sq1 >= 0) {
            int sq2 = msg.indexOf('\'', sq1 + 1);
            if (sq2 > sq1) {
                String inside = msg.substring(sq1 + 1, sq2).trim();
                if (!inside.isEmpty()) return inside;
            }
        }

        // Fallback: strip the "영어로 번역해줘" suffix-ish part and take the left side.
        Matcher m = TRANSLATE_TO_EN.matcher(msg);
        if (m.find()) {
            String left = msg.substring(0, m.start()).trim();
            if (!left.isEmpty()) return left;
        }
        return msg;
    }

    private String translateToEnglishStrict(String source) {
        List<Message> msgs = List.of(
            Message.system(
                "You are a translation engine.\n" +
                "Task: Translate the given Korean text to natural English.\n" +
                "Rules:\n" +
                "- Output ONLY the English translation.\n" +
                "- Do not output Chinese, Korean, or any other language.\n" +
                "- Keep it short (as a phrase), no explanations."
            ),
            Message.user(source)
        );

        String out = chat.chat(msgs, new OllamaChatService.Options(0.0, 0.9, 64));
        out = sanitizeSingleLine(out);
        if (containsCjkOrHangul(out)) {
            // One retry with even stricter constraint.
            List<Message> retry = List.of(
                Message.system(
                    "Translate to English.\n" +
                    "Output must be plain ASCII English words only.\n" +
                    "No Chinese/Korean characters. No explanations."
                ),
                Message.user(source)
            );
            out = chat.chat(retry, new OllamaChatService.Options(0.0, 0.9, 64));
            out = sanitizeSingleLine(out);
        }
        return out;
    }

    private static String sanitizeSingleLine(String s) {
        if (s == null) return "";
        String x = s.trim();
        // Drop surrounding quotes.
        if ((x.startsWith("\"") && x.endsWith("\"")) || (x.startsWith("'") && x.endsWith("'"))) {
            x = x.substring(1, x.length() - 1).trim();
        }
        // If model returned multiple lines, keep the first non-empty line.
        String[] lines = x.split("\\r?\\n");
        for (String ln : lines) {
            String t = ln.trim();
            if (!t.isEmpty()) return t;
        }
        return "";
    }

    private static boolean containsCjkOrHangul(String s) {
        if (s == null || s.isEmpty()) return false;
        // Hangul + CJK Unified Ideographs ranges.
        return s.matches(".*[\\uAC00-\\uD7A3\\u4E00-\\u9FFF].*");
    }

    private static OllamaChatService.Options pickOptionsFor(String userMsg) {
        String m = userMsg == null ? "" : userMsg;
        boolean looksLikeSummary = m.contains("요약") || m.contains("정리") || m.toLowerCase().contains("summar");
        int len = m.length();

        // For long pasted text + summary requests, allow a longer completion.
        if (looksLikeSummary || len >= 2000) {
            return new OllamaChatService.Options(0.2, 0.9, 900);
        }
        if (len >= 800) {
            return new OllamaChatService.Options(0.2, 0.9, 700);
        }
        return new OllamaChatService.Options(0.2, 0.9, 512);
    }
}
