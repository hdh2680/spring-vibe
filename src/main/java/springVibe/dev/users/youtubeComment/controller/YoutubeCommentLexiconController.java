package springVibe.dev.users.youtubeComment.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import springVibe.dev.users.youtubeComment.service.SentimentCustomLexiconFileService;
import springVibe.system.exception.BaseException;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/users/youtubeComment/lexicon")
public class YoutubeCommentLexiconController {
    private final SentimentCustomLexiconFileService fileService;

    public YoutubeCommentLexiconController(SentimentCustomLexiconFileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping(value = "/custom.tsv", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String customTsv() {
        return fileService.readRawTsv();
    }

    @PostMapping(value = "/custom/add", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> add(@RequestParam("term") String term, @RequestParam("score") int score) {
        try {
            fileService.upsert(term, score);
            return ResponseEntity.ok(ok("ok"));
        } catch (BaseException e) {
            return ResponseEntity.badRequest().body(err(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(err("ERR001", e.getMessage()));
        }
    }

    @PostMapping(value = "/custom/delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> delete(@RequestParam("term") String term) {
        try {
            fileService.delete(term);
            return ResponseEntity.ok(ok("ok"));
        } catch (BaseException e) {
            return ResponseEntity.badRequest().body(err(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(err("ERR001", e.getMessage()));
        }
    }

    private static Map<String, Object> ok(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", true);
        m.put("message", message);
        return m;
    }

    private static Map<String, Object> err(String code, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", false);
        m.put("errorCode", code);
        m.put("message", message);
        return m;
    }
}
