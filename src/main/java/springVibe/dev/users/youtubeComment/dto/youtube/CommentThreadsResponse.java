package springVibe.dev.users.youtubeComment.dto.youtube;

import java.util.List;

/**
 * Minimal DTO for YouTube Data API: commentThreads.list response (part=snippet).
 */
public class CommentThreadsResponse {
    private String nextPageToken;
    private PageInfo pageInfo;
    private List<Item> items;

    public String getNextPageToken() {
        return nextPageToken;
    }

    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }

    public PageInfo getPageInfo() {
        return pageInfo;
    }

    public void setPageInfo(PageInfo pageInfo) {
        this.pageInfo = pageInfo;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public static class PageInfo {
        private Integer totalResults;
        private Integer resultsPerPage;

        public Integer getTotalResults() {
            return totalResults;
        }

        public void setTotalResults(Integer totalResults) {
            this.totalResults = totalResults;
        }

        public Integer getResultsPerPage() {
            return resultsPerPage;
        }

        public void setResultsPerPage(Integer resultsPerPage) {
            this.resultsPerPage = resultsPerPage;
        }
    }

    public static class Item {
        private String id;
        private Snippet snippet;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Snippet getSnippet() {
            return snippet;
        }

        public void setSnippet(Snippet snippet) {
            this.snippet = snippet;
        }
    }

    public static class Snippet {
        private TopLevelComment topLevelComment;

        public TopLevelComment getTopLevelComment() {
            return topLevelComment;
        }

        public void setTopLevelComment(TopLevelComment topLevelComment) {
            this.topLevelComment = topLevelComment;
        }
    }

    public static class TopLevelComment {
        private String id;
        private CommentSnippet snippet;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public CommentSnippet getSnippet() {
            return snippet;
        }

        public void setSnippet(CommentSnippet snippet) {
            this.snippet = snippet;
        }
    }

    public static class CommentSnippet {
        private String authorDisplayName;
        private String textOriginal;
        private Long likeCount;
        private String publishedAt;

        public String getAuthorDisplayName() {
            return authorDisplayName;
        }

        public void setAuthorDisplayName(String authorDisplayName) {
            this.authorDisplayName = authorDisplayName;
        }

        public String getTextOriginal() {
            return textOriginal;
        }

        public void setTextOriginal(String textOriginal) {
            this.textOriginal = textOriginal;
        }

        public Long getLikeCount() {
            return likeCount;
        }

        public void setLikeCount(Long likeCount) {
            this.likeCount = likeCount;
        }

        public String getPublishedAt() {
            return publishedAt;
        }

        public void setPublishedAt(String publishedAt) {
            this.publishedAt = publishedAt;
        }
    }
}

