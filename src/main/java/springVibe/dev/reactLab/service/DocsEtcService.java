package springVibe.dev.reactLab.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import springVibe.dev.users.devSearch.service.MarkdownRenderService;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads markdown docs from docs/etc and renders to safe HTML for the React docs viewer page.
 *
 * Security note: access is whitelist-only (no path traversal).
 */
@Service
public class DocsEtcService {
    private final MarkdownRenderService markdownRenderService;

    public DocsEtcService(MarkdownRenderService markdownRenderService) {
        this.markdownRenderService = markdownRenderService;
    }

    public record DocItem(String id, String title) {}

    public record Doc(String id, String title, String markdown, String html) {}

    private static final Map<String, DocDef> DOCS = new LinkedHashMap<>() {{
        put("react-vite-guide", new DocDef("Frontend (React + TypeScript, Vite) 병행 운영 가이드", "FRONTEND_REACT_TS_VITE.md"));
        put("app-runtime-flow", new DocDef("Frontend `/app` 구현 관점 실행 흐름", "FRONTEND_APP_SCREEN_FLOW.md"));
        put("app-files", new DocDef("Frontend(`/app`) 폴더 구조와 파일 역할", "FRONTEND_APP_FILES.md"));
        put("app-dev-origin", new DocDef("로컬 개발: Spring 메뉴에서 `/app/**`를 Vite(5173)로 열기", "FRONTEND_DEV_ORIGIN.md"));
        put("react-ts-docs-reader", new DocDef("React TS 문서 뷰어(ReactTsDocsPage.tsx) 소스 설명", "FRONTEND_REACT_TS_DOCS_READER.md"));
        put("react-hooks-top5", new DocDef("React Hooks Top 5 (useState/useEffect/useRef/useContext/useReducer) + 예제", "FRONTEND_REACT_HOOKS_TOP5.md"));
        put("es6-for-react", new DocDef("React를 위한 ES6: 자주 막히는 문법만 정리", "FRONTEND_ES6_FOR_REACT.md"));
        put("react-fundamentals", new DocDef("React 핵심 정리: 렌더링, state/props, hooks, 폼", "FRONTEND_REACT_FUNDAMENTALS.md"));
    }};

    private record DocDef(String title, String fileName) {}

    public List<DocItem> list() {
        return DOCS.entrySet().stream()
            .map(e -> new DocItem(e.getKey(), e.getValue().title))
            .toList();
    }

    public Doc get(String id) {
        DocDef def = DOCS.get(id);
        if (def == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다: " + id);
        }

        String md = readDocsEtcUtf8(def.fileName);
        String html = markdownRenderService.renderToSafeHtml(md);
        return new Doc(id, def.title, md, html);
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
