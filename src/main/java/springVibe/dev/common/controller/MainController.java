package springVibe.dev.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Main page mapping.
 *
 * Resolves to: src/main/resources/templates/html/main.html
 */
@Controller
public class MainController {

    @GetMapping("/main")
    public String view(Model model) {
        model.addAttribute("pageTitle", "Main");
        model.addAttribute("contentTemplate", "html/main");
        return "layout/app";
    }
}
