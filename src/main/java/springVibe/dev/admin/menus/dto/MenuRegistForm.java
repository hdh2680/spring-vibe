package springVibe.dev.admin.menus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class MenuRegistForm {
    private Long id;
    private Long parentId;

    @NotBlank(message = "menuKey는 필수입니다.")
    @Size(max = 100, message = "menuKey는 100자 이하여야 합니다.")
    private String menuKey;

    @NotBlank(message = "menuName은 필수입니다.")
    @Size(max = 100, message = "menuName은 100자 이하여야 합니다.")
    private String menuName;

    @Size(max = 255, message = "path는 255자 이하여야 합니다.")
    private String path;

    @Size(max = 100, message = "icon은 100자 이하여야 합니다.")
    private String icon;

    @NotNull(message = "sortOrder는 필수입니다.")
    private Integer sortOrder = 0;

    @NotNull(message = "isEnabled는 필수입니다.")
    private Boolean isEnabled = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getMenuKey() {
        return menuKey;
    }

    public void setMenuKey(String menuKey) {
        this.menuKey = menuKey;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean enabled) {
        isEnabled = enabled;
    }
}
