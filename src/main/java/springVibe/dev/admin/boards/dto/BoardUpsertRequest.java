package springVibe.dev.admin.boards.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class BoardUpsertRequest {
    @NotBlank
    @Size(max = 64)
    private String boardKey;

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;

    @NotNull
    private Boolean isEnabled;

    @NotNull
    private Boolean isPublicRead;

    @NotNull
    private Boolean isPublicWrite;

    @NotNull
    @Max(1_000_000)
    private Integer sortOrder;

    public String getBoardKey() {
        return boardKey;
    }

    public void setBoardKey(String boardKey) {
        this.boardKey = boardKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(Boolean enabled) {
        isEnabled = enabled;
    }

    public Boolean getIsPublicRead() {
        return isPublicRead;
    }

    public void setIsPublicRead(Boolean publicRead) {
        isPublicRead = publicRead;
    }

    public Boolean getIsPublicWrite() {
        return isPublicWrite;
    }

    public void setIsPublicWrite(Boolean publicWrite) {
        isPublicWrite = publicWrite;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}

