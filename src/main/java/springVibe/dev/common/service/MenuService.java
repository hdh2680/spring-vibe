package springVibe.dev.common.service;

import springVibe.dev.common.domain.Menu;
import springVibe.dev.common.mapper.MenuMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MenuService {
    private final MenuMapper menuMapper;

    public MenuService(MenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    public List<Menu> findLeftMenusByUsername(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }

        List<Menu> accessible = menuMapper.findAccessibleMenusByUsername(username);
        if (accessible == null || accessible.isEmpty()) {
            return List.of();
        }

        // Ensure parent menus exist in the render tree even if not explicitly granted.
        Map<Long, Menu> byId = new HashMap<>();
        for (Menu m : accessible) {
            if (m != null && m.getId() != null) {
                byId.put(m.getId(), m);
            }
        }

        Set<Long> toFetch = new HashSet<>();
        for (Menu m : byId.values()) {
            Long pid = m.getParentId();
            if (pid != null && !byId.containsKey(pid)) {
                toFetch.add(pid);
            }
        }

        while (!toFetch.isEmpty()) {
            List<Long> batch = new ArrayList<>(toFetch);
            toFetch.clear();

            List<Menu> parents = menuMapper.findEnabledMenusByIds(batch);
            if (parents == null || parents.isEmpty()) {
                break;
            }
            for (Menu p : parents) {
                if (p == null || p.getId() == null) {
                    continue;
                }
                if (byId.putIfAbsent(p.getId(), p) == null) {
                    Long nextPid = p.getParentId();
                    if (nextPid != null && !byId.containsKey(nextPid)) {
                        toFetch.add(nextPid);
                    }
                }
            }
        }

        // Build tree.
        for (Menu m : byId.values()) {
            Long pid = m.getParentId();
            if (pid != null) {
                Menu parent = byId.get(pid);
                if (parent != null) {
                    parent.getChildren().add(m);
                }
            }
        }

        Comparator<Menu> bySort = Comparator
            .comparing((Menu m) -> m.getSortOrder() == null ? 0 : m.getSortOrder())
            .thenComparing(m -> m.getId() == null ? 0L : m.getId());

        // Collect roots and sort.
        List<Menu> roots = new ArrayList<>();
        for (Menu m : byId.values()) {
            if (m.getParentId() == null || !byId.containsKey(m.getParentId())) {
                roots.add(m);
            }
        }
        roots.sort(bySort);
        for (Menu r : roots) {
            r.getChildren().sort(bySort);
        }

        return roots;
    }
}

