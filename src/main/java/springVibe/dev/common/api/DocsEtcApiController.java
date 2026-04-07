package springVibe.dev.common.api;

import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springVibe.dev.users.devSearch.service.MarkdownRenderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Expose selected docs/etc markdown files for the React /app reader page.
 *
 * Security note: file access is whitelist-only (no path traversal).
 */
@RestController
@RequestMapping("/api/docs/etc")
public class DocsEtcApiController {
    private final MarkdownRenderService markdownRenderService;

    public DocsEtcApiController(MarkdownRenderService markdownRenderService) {
        this.markdownRenderService = markdownRenderService;
    }

    public record DocItem(String id, String title) {}

    public record DocResponse(String id, String title, String markdown, String html) {}

    private static final Map<String, DocDef> DOCS = new LinkedHashMap<>() {{
        put("react-vite-guide", new DocDef("Frontend (React + TypeScript, Vite) 병행 운영 가이드", "FRONTEND_REACT_TS_VITE.md"));
        put("app-runtime-flow", new DocDef("Frontend `/app` 구현 관점 실행 흐름", "FRONTEND_APP_SCREEN_FLOW.md"));
        put("app-files", new DocDef("Frontend(`/app`) 폴더 구조와 파일 역할", "FRONTEND_APP_FILES.md"));
        put("app-dev-origin", new DocDef("로컬 개발: Spring 메뉴에서 `/app/**`를 Vite(5173)로 열기", "FRONTEND_DEV_ORIGIN.md"));
    }};

    private record DocDef(String title, String fileName) {}

    @GetMapping
    public List<DocItem> list() {
        return DOCS.entrySet().stream()
            .map(e -> new DocItem(e.getKey(), e.getValue().title))
            .toList();
    }

    @GetMapping("/{id}")
    public DocResponse get(@PathVariable String id) {
        DocDef def = DOCS.get(id);
        if (def == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다: " + id);
        }

        String md = readDocsEtcUtf8(def.fileName);
        String html = markdownRenderService.renderToSafeHtml(md);
        return new DocResponse(id, def.title, md, html);
    }

    private static String readDocsEtcUtf8(String fileName) {
        // Prefer repo filesystem (keeps docs in sync during development).
        Path fs = Path.of("docs", "etc", fileName);
        try {
            if (Files.exists(fs)) {
                return Files.readString(fs, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            // fall through to classpath fallback
        }

        // Fallback: try classpath (optional; helps when docs are packaged under resources).
        String cpPath = "static/docs/etc/" + fileName;
        try {
            ClassPathResource r = new ClassPathResource(cpPath);
            if (!r.exists()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "문서를 읽을 수 없습니다: " + fileName);
            }
            try (InputStream in = r.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "문서를 읽는 중 오류가 발생했습니다: " + fileName, e);
        }
    }
}
