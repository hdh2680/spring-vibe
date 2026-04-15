package springVibe.dev.admin.boards.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class Board {
    private Long id;
    private String boardKey;
    private String name;
    private String description;
    private Boolean isEnabled;
    private Boolean isPublicRead;
    private Boolean isPublicWrite;
    private Integer sortOrder;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
