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

    int insertSentiments(
        @Param("historyId") Long historyId,
        @Param("userId") Long userId,
        @Param("requestedAt") LocalDateTime requestedAt,
        @Param("paramsJson") String paramsJson,
        @Param("resultJson") String resultJson
    );

    int insertWordNetworks(
        @Param("historyId") Long historyId,
        @Param("userId") Long userId,
        @Param("requestedAt") LocalDateTime requestedAt,
        @Param("paramsJson") String paramsJson,
        @Param("resultJson") String resultJson
    );

    int deleteTopKeywordsByHistoryId(@Param("historyId") Long historyId, @Param("userId") Long userId);

    int deleteTopicGroupsByHistoryId(@Param("historyId") Long historyId, @Param("userId") Long userId);

    int deleteSentimentsByHistoryId(@Param("historyId") Long historyId, @Param("userId") Long userId);

    int deleteWordNetworksByHistoryId(@Param("historyId") Long historyId, @Param("userId") Long userId);

    String selectLatestTopKeywordsResultJson(@Param("historyId") Long historyId, @Param("userId") Long userId);

    String selectLatestTopicGroupsResultJson(@Param("historyId") Long historyId, @Param("userId") Long userId);

    String selectLatestSentimentsResultJson(@Param("historyId") Long historyId, @Param("userId") Long userId);

    String selectLatestWordNetworksResultJson(@Param("historyId") Long historyId, @Param("userId") Long userId);

}

