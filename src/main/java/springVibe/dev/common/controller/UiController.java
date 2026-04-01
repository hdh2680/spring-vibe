package springVibe.dev.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Temporary UI routes until menu-driven routing is implemented.
 */
@Controller
public class UiController {
    private static String render(Model model, String pageTitle, String contentTemplate, String activeMenu) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("contentTemplate", contentTemplate);
        model.addAttribute("activeMenu", activeMenu);
        return "layout/app";
    }

    @GetMapping("/intro")
    public String intro(Model model) {
        return render(model, "Intro", "html/intro", "/intro");
    }
}
