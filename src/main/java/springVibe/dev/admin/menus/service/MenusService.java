package springVibe.dev.admin.menus.service;

import springVibe.dev.admin.menus.domain.Menu;
import springVibe.dev.admin.menus.dto.MenuRegistForm;
import springVibe.dev.admin.menus.mapper.MenusMapper;
import springVibe.system.exception.BaseException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class MenusService {
    private final MenusMapper menusMapper;

    public MenusService(MenusMapper menusMapper) {
        this.menusMapper = menusMapper;
    }

    public List<Menu> findAll() {
        return menusMapper.findAll();
    }

    public Menu findById(Long id) {
        return menusMapper.findById(id);
    }

    public List<Menu> findAllPossibleParents(Long selfId) {
        List<Menu> all = menusMapper.findAllParents();
        if (all == null) {
            return List.of();
        }
        if (selfId == null) {
            return all;
        }
        List<Menu> filtered = new ArrayList<>();
        for (Menu m : all) {
            if (m != null && m.getId() != null && !m.getId().equals(selfId)) {
                filtered.add(m);
            }
        }
        return filtered;
    }

    @Transactional
    public Long create(MenuRegistForm form) {
        if (menusMapper.findByMenuKey(form.getMenuKey()) != null) {
            throw new BaseException("MENU_DUP_KEY", "이미 존재하는 menuKey 입니다.");
        }

        Menu m = new Menu();
        m.setParentId(form.getParentId());
        m.setMenuKey(form.getMenuKey());
        m.setMenuName(form.getMenuName());
        m.setPath(blankToNull(form.getPath()));
        m.setIcon(blankToNull(form.getIcon()));
        m.setSortOrder(form.getSortOrder());
        m.setIsEnabled(form.getIsEnabled());

        try {
            menusMapper.insert(m);
        } catch (DuplicateKeyException e) {
            throw new BaseException("MENU_DUPLICATE", "중복 데이터가 존재합니다(menu_key).", e);
        }
        return m.getId();
    }

    @Transactional
    public void update(MenuRegistForm form) {
        if (form.getId() == null) {
            throw new BaseException("MENU_ID_REQUIRED", "수정 시 메뉴 ID는 필수입니다.");
        }
        Menu current = menusMapper.findById(form.getId());
        if (current == null) {
            throw new BaseException("MENU_NOT_FOUND", "메뉴를 찾을 수 없습니다.");
        }

        // Prevent self parenting.
        if (form.getParentId() != null && form.getParentId().equals(form.getId())) {
            throw new BaseException("MENU_INVALID_PARENT", "자기 자신을 부모로 지정할 수 없습니다.");
        }

        // menuKey uniqueness check on update when changed
        if (form.getMenuKey() != null && !form.getMenuKey().equals(current.getMenuKey())) {
            if (menusMapper.findByMenuKey(form.getMenuKey()) != null) {
                throw new BaseException("MENU_DUP_KEY", "이미 존재하는 menuKey 입니다.");
            }
        }

        Menu m = new Menu();
        m.setId(form.getId());
        m.setParentId(form.getParentId());
        m.setMenuKey(form.getMenuKey());
        m.setMenuName(form.getMenuName());
        m.setPath(blankToNull(form.getPath()));
        m.setIcon(blankToNull(form.getIcon()));
        m.setSortOrder(form.getSortOrder());
        m.setIsEnabled(form.getIsEnabled());

        try {
            menusMapper.update(m);
        } catch (DuplicateKeyException e) {
            throw new BaseException("MENU_DUPLICATE", "중복 데이터가 존재합니다(menu_key).", e);
        }
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) {
            return;
        }
        menusMapper.delete(id);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
