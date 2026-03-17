package springVibe.dev.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Login page mapping (per docs/PRD.md).
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String view() {
        // templates/html/login.html
        return "html/login";
    }
}

