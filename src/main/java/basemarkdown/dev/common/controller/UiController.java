package basemarkdown.dev.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Temporary UI routes until menu-driven routing is implemented.
 */
@Controller
public class UiController {
    @GetMapping("/")
    public String dashboard(Model model) {
        return render(model, "Dashboard", "pages/dashboard", "dashboard");
    }

    @GetMapping("/ui/tables")
    public String tables(Model model) {
        return render(model, "Tables", "pages/tables", "tables");
    }

    @GetMapping("/ui/data-quality")
    public String dataQuality(Model model) {
        return render(model, "Data Quality", "pages/data-quality", "data-quality");
    }

    @GetMapping("/ui/reports")
    public String reports(Model model) {
        return render(model, "Reports", "pages/reports", "reports");
    }

    @GetMapping("/ui/users")
    public String users(Model model) {
        return render(model, "Users & Roles", "pages/users", "users");
    }

    private static String render(Model model, String pageTitle, String contentTemplate, String activeMenu) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("contentTemplate", contentTemplate);
        model.addAttribute("activeMenu", activeMenu);
        return "layout/app";
    }
}

