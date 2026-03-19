package springVibe.dev.users.youtubeComment.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface YoutubeCommentAnalysisResultMapper {
    int insertTopKeywords(
        @Param("historyId") Long historyId,
        @Param("userId") Long userId,
        @Param("requestedAt") LocalDateTime requestedAt,
        @Param("paramsJson") String paramsJson,
        @Param("resultJson") String resultJson
    );

    int insertTopicGroups(
        @Param("historyId") Long historyId,
        @Param("userId") Long userId,
        @Param("requestedAt") LocalDateTime requestedAt,
        @Param("paramsJson") String paramsJson,
        @Param("resultJson") String resultJson
    );

    String selectLatestTopKeywordsResultJson(@Param("historyId") Long historyId, @Param("userId") Long userId);

    String selectLatestTopicGroupsResultJson(@Param("historyId") Long historyId, @Param("userId") Long userId);

}

