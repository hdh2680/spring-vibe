package springVibe.dev.admin.users.controller;

import springVibe.dev.admin.users.domain.AdminUser;
import springVibe.dev.admin.users.domain.Role;
import springVibe.dev.admin.users.dto.UserRegistForm;
import springVibe.dev.admin.users.service.UsersService;
import springVibe.system.exception.BaseException;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
public class UsersController {
    private final UsersService usersService;

    public UsersController(UsersService usersService) {
        this.usersService = usersService;
    }

    @GetMapping
    public String root() {
        return "redirect:/admin/users/list";
    }

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("users", usersService.findAll());
        return render(model, "사용자관리", "html/admin/users/list");
    }

    @GetMapping("/view")
    public String view(@RequestParam("id") Long id, Model model) {
        AdminUser user = usersService.findById(id);
        if (user == null) {
            throw new BaseException("USR_NOT_FOUND", "사용자를 찾을 수 없습니다.");
        }

        List<Role> roles = usersService.findAllRoles();
        List<Long> roleIds = usersService.findRoleIdsByUserId(id);

        model.addAttribute("user", user);
        model.addAttribute("roles", roles);
        model.addAttribute("roleIds", roleIds);
        return render(model, "사용자상세", "html/admin/users/view");
    }

    @GetMapping("/regist")
    public String regist(@RequestParam(value = "id", required = false) Long id, Model model) {
        UserRegistForm form = new UserRegistForm();
        if (id != null) {
            AdminUser user = usersService.findById(id);
            if (user == null) {
                throw new BaseException("USR_NOT_FOUND", "사용자를 찾을 수 없습니다.");
            }
            form.setId(user.getId());
            form.setUsername(user.getUsername());
            form.setName(user.getName());
            form.setEmail(user.getEmail());
            form.setPhone(user.getPhone());
            form.setStatus(user.getStatus());
            form.setRoleIds(usersService.findRoleIdsByUserId(id));
        }

        model.addAttribute("form", form);
        model.addAttribute("roles", usersService.findAllRoles());
        model.addAttribute("isEdit", id != null);
        return render(model, (id == null ? "사용자등록" : "사용자수정"), "html/admin/users/regist");
    }

    @PostMapping("/regist")
    public String submit(
        @Valid @ModelAttribute("form") UserRegistForm form,
        BindingResult bindingResult,
        @RequestParam(value = "isEdit", required = false, defaultValue = "false") boolean isEdit,
        Model model
    ) {
        // Additional validation: password required only for create.
        if (!isEdit && (form.getPassword() == null || form.getPassword().isBlank())) {
            bindingResult.rejectValue("password", "USR_PASSWORD_REQUIRED", "신규 등록 시 비밀번호는 필수입니다.");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", usersService.findAllRoles());
            model.addAttribute("isEdit", isEdit);
            return render(model, (isEdit ? "사용자수정" : "사용자등록"), "html/admin/users/regist");
        }

        if (isEdit) {
            usersService.update(form);
            return "redirect:/admin/users/view?id=" + form.getId();
        }

        Long id = usersService.create(form);
        return "redirect:/admin/users/view?id=" + id;
    }

    private static String render(Model model, String pageTitle, String contentTemplate) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("contentTemplate", contentTemplate);
        return "layout/app";
    }
}
