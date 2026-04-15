package springVibe.dev.users.devSearch.service;

import java.util.List;
import java.util.Locale;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.nodes.Document;
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
        // Keep relative URLs (e.g. "/images/..." or "#anchor") instead of forcing absolute URLs.
        s.preserveRelativeLinks(true);
        // Allow code blocks well.
        s.addTags("pre", "code");
        // Images are common in docs; keep basic attributes.
        s.addTags("img");
        s.addAttributes("img", "src", "alt", "title");
        // IMPORTANT:
        // Safelist.relaxed() ships with protocol restrictions for some attributes (ex: a[href], img[src]).
        // Those restrictions can drop relative URLs (ex: "/images/...") before we get a chance to post-filter.
        // So we remove those protocol restrictions, then we harden URL schemes ourselves after cleaning
        // (blocks javascript:, vbscript:, etc.).
        s.removeProtocols("img", "src", "http", "https");
        s.removeProtocols("a", "href", "http", "https", "mailto");
        this.safelist = s;
    }

    public String renderToSafeHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        Node doc = parser.parse(markdown);
        String html = renderer.render(doc);
        String cleaned = Jsoup.clean(html, safelist);
        return sanitizeUrls(cleaned);
    }

    private static String sanitizeUrls(String html) {
        // Extra URL hardening to prevent XSS via "javascript:" etc.
        Document d = Jsoup.parseBodyFragment(html);

        d.select("a[href]").forEach(a -> {
            String href = a.attr("href");
            if (!isSafeHref(href)) {
                a.removeAttr("href");
            }
        });

        d.select("img[src]").forEach(img -> {
            String src = img.attr("src");
            if (!isSafeImgSrc(src)) {
                img.removeAttr("src");
            }
        });

        return d.body().html();
    }

    private static boolean isSafeHref(String href) {
        String v = normalizeUrl(href);
        if (v.isEmpty()) return false;

        // Relative URLs and anchors are ok.
        if (v.startsWith("#") || v.startsWith("/") || v.startsWith("./") || v.startsWith("../") || v.startsWith("?")) {
            return true;
        }

        // Allowed absolute schemes.
        return v.startsWith("http://") || v.startsWith("https://") || v.startsWith("mailto:");
    }

    private static boolean isSafeImgSrc(String src) {
        String v = normalizeUrl(src);
        if (v.isEmpty()) return false;

        // Relative URLs are ok (same-origin static assets).
        if (v.startsWith("/") || v.startsWith("./") || v.startsWith("../") || v.startsWith("?")) {
            return true;
        }

        // Allowed absolute schemes.
        if (v.startsWith("http://") || v.startsWith("https://")) return true;

        // Allow data images only (avoid data:text/html etc.).
        return v.startsWith("data:image/");
    }

    private static String normalizeUrl(String url) {
        if (url == null) return "";
        // Trim and lowercase for scheme checks; keep the original URL in the DOM as-is.
        return url.trim().toLowerCase(Locale.ROOT);
    }
}
