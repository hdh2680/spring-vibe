package springVibe.dev.users.chat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/users/chat")
public class ChatPagesController {

    @GetMapping
    public String view(Model model) {
        return render(model, "Chat", "html/users/chat/chat");
    }

    private static String render(Model model, String pageTitle, String contentTemplate) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("contentTemplate", contentTemplate);
        return "layout/app";
    }
}

