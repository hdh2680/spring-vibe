package springVibe.dev.users.devSearch.service;

import java.util.List;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

/**
 * Render Markdown to safe HTML for UI display.
 *
 * Security notes:
 * - Raw HTML in Markdown is escaped.
 * - Output HTML is additionally sanitized to prevent XSS via links, images, etc.
 */
@Service
public class MarkdownRenderService {
    private final Parser parser;
    private final HtmlRenderer renderer;
    private final Safelist safelist;

    public MarkdownRenderService() {
        List<Extension> extensions = List.of(TablesExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        this.renderer = HtmlRenderer.builder()
            .extensions(extensions)
            .escapeHtml(true)
            .build();

        Safelist s = Safelist.relaxed();
        // Allow code blocks well.
        s.addTags("pre", "code");
        // Images are common in blog posts; keep but restrict protocols.
        s.addTags("img");
        s.addAttributes("img", "src", "alt", "title");
        s.addProtocols("img", "src", "http", "https", "data");
        // Links: only http/https/mailto.
        s.addProtocols("a", "href", "http", "https", "mailto");
        this.safelist = s;
    }

    public String renderToSafeHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        Node doc = parser.parse(markdown);
        String html = renderer.render(doc);
        return Jsoup.clean(html, safelist);
    }
}

