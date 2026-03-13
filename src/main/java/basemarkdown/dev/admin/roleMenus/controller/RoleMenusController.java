package basemarkdown.dev.admin.roleMenus.controller;

import basemarkdown.dev.admin.roleMenus.domain.Menu;
import basemarkdown.dev.admin.roleMenus.domain.Role;
import basemarkdown.dev.admin.roleMenus.dto.RoleMenusForm;
import basemarkdown.dev.admin.roleMenus.service.RoleMenusService;
import basemarkdown.system.exception.BaseException;
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
@RequestMapping("/admin/roleMenus")
public class RoleMenusController {
    private final RoleMenusService roleMenusService;

    public RoleMenusController(RoleMenusService roleMenusService) {
        this.roleMenusService = roleMenusService;
    }

    @GetMapping
    public String root() {
        return "redirect:/admin/roleMenus/manage";
    }

    @GetMapping("/manage")
    public String manage(@RequestParam(value = "roleId", required = false) Long roleId, Model model) {
        List<Role> roles = roleMenusService.findAllRoles();
        if (roles == null || roles.isEmpty()) {
            throw new BaseException("ROLE_EMPTY", "권한(role) 데이터가 없습니다.");
        }

        if (roleId == null) {
            roleId = roles.get(0).getId();
        }

        Role selected = roleMenusService.findRoleById(roleId);
        if (selected == null) {
            throw new BaseException("ROLE_NOT_FOUND", "권한(role)을 찾을 수 없습니다.");
        }

        List<Menu> menuTree = roleMenusService.findMenuTree();
        List<Long> grantedMenuIds = roleMenusService.findGrantedMenuIds(roleId);

        RoleMenusForm form = new RoleMenusForm();
        form.setRoleId(roleId);
        form.setMenuIds(grantedMenuIds);

        model.addAttribute("roles", roles);
        model.addAttribute("selectedRole", selected);
        model.addAttribute("menuTree", menuTree);
        model.addAttribute("form", form);

        return render(model, "권한메뉴관리", "html/admin/roleMenus/roleMenus");
    }

    @PostMapping("/manage")
    public String save(
        @Valid @ModelAttribute("form") RoleMenusForm form,
        BindingResult bindingResult,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            // Rehydrate view model.
            List<Role> roles = roleMenusService.findAllRoles();
            Role selected = roleMenusService.findRoleById(form.getRoleId());
            model.addAttribute("roles", roles);
            model.addAttribute("selectedRole", selected);
            model.addAttribute("menuTree", roleMenusService.findMenuTree());
            return render(model, "권한메뉴관리", "html/admin/roleMenus/roleMenus");
        }

        roleMenusService.save(form);
        return "redirect:/admin/roleMenus/manage?roleId=" + form.getRoleId();
    }

    private static String render(Model model, String pageTitle, String contentTemplate) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("contentTemplate", contentTemplate);
        return "layout/app";
    }
}
