package basemarkdown.dev.admin.menus.controller;

import basemarkdown.dev.admin.menus.domain.Menu;
import basemarkdown.dev.admin.menus.dto.MenuRegistForm;
import basemarkdown.dev.admin.menus.service.MenusService;
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

@Controller
@RequestMapping("/admin/menus")
public class MenusController {
    private final MenusService menusService;

    public MenusController(MenusService menusService) {
        this.menusService = menusService;
    }

    @GetMapping
    public String root() {
        return "redirect:/admin/menus/list";
    }

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("menus", menusService.findAll());
        return render(model, "메뉴관리", "admin/menus/list");
    }

    @GetMapping("/view")
    public String view(@RequestParam("id") Long id, Model model) {
        Menu menu = menusService.findById(id);
        if (menu == null) {
            throw new BaseException("MENU_NOT_FOUND", "메뉴를 찾을 수 없습니다.");
        }

        model.addAttribute("menu", menu);
        return render(model, "메뉴상세", "admin/menus/view");
    }

    @GetMapping("/regist")
    public String regist(@RequestParam(value = "id", required = false) Long id, Model model) {
        MenuRegistForm form = new MenuRegistForm();

        if (id != null) {
            Menu menu = menusService.findById(id);
            if (menu == null) {
                throw new BaseException("MENU_NOT_FOUND", "메뉴를 찾을 수 없습니다.");
            }
            form.setId(menu.getId());
            form.setParentId(menu.getParentId());
            form.setMenuKey(menu.getMenuKey());
            form.setMenuName(menu.getMenuName());
            form.setPath(menu.getPath());
            form.setIcon(menu.getIcon());
            form.setSortOrder(menu.getSortOrder());
            form.setIsEnabled(menu.getIsEnabled());
        }

        model.addAttribute("form", form);
        model.addAttribute("isEdit", id != null);
        model.addAttribute("parents", menusService.findAllPossibleParents(id));
        return render(model, (id == null ? "메뉴등록" : "메뉴수정"), "admin/menus/regist");
    }

    @PostMapping("/regist")
    public String submit(
        @Valid @ModelAttribute("form") MenuRegistForm form,
        BindingResult bindingResult,
        @RequestParam(value = "isEdit", required = false, defaultValue = "false") boolean isEdit,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", isEdit);
            model.addAttribute("parents", menusService.findAllPossibleParents(form.getId()));
            return render(model, (isEdit ? "메뉴수정" : "메뉴등록"), "admin/menus/regist");
        }

        if (isEdit) {
            menusService.update(form);
            return "redirect:/admin/menus/view?id=" + form.getId();
        }

        Long id = menusService.create(form);
        return "redirect:/admin/menus/view?id=" + id;
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("id") Long id) {
        menusService.delete(id);
        return "redirect:/admin/menus/list";
    }

    private static String render(Model model, String pageTitle, String contentTemplate) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("contentTemplate", contentTemplate);
        return "layout/app";
    }
}
