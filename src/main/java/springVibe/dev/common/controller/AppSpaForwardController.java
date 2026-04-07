package springVibe.dev.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * React SPA entrypoint forwarding for /app/**.
 *
 * Production assumption:
 * - React build output is served by Spring under: /app/index.html, /app/assets/** ...
 *
 * Deep links (e.g. /app/reports/123) must be forwarded to /app/index.html so the client router can handle them.
 */
@Controller
public class AppSpaForwardController {

    // Spring Boot 3 PathPatternParser note:
    // - You cannot have additional pattern data after a "**" element.
    // - Use "{*rest}" to capture the remainder, and exclude "/app/assets/**" so Vite build assets are served as static files.
    @GetMapping({
        "/app",
        "/app/",
        "/app/{path:(?!assets$)[^\\.]+}",
        "/app/{path:(?!assets$)[^\\.]+}/{*rest}"
    })
    public String forwardToApp() {
        return "forward:/app/index.html";
    }
}
