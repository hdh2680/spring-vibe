package springVibe.dev.users.youtubeComment.mapper;

import springVibe.dev.users.youtubeComment.domain.YoutubeCommentAnalysisHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface YoutubeCommentAnalysisHistoryMapper {
    int insert(YoutubeCommentAnalysisHistory history);

    List<YoutubeCommentAnalysisHistory> selectList(@Param("userId") Long userId);

    YoutubeCommentAnalysisHistory selectById(@Param("id") Long id, @Param("userId") Long userId);

    int updatePreprocessed(
        @Param("id") Long id,
        @Param("userId") Long userId,
        @Param("preprocessedFilePath") String preprocessedFilePath,
        @Param("preprocessedSavedAt") LocalDateTime preprocessedSavedAt
    );
}
