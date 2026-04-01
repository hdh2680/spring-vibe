package springVibe.dev.users.youtubeComment.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import springVibe.dev.users.youtubeComment.dto.YoutubeCommentSentimentItemRow;

import java.util.List;

@Mapper
public interface YoutubeCommentAnalysisSentimentItemMapper {
    int deleteByHistoryId(@Param("historyId") Long historyId, @Param("userId") Long userId);

    int insertBatch(
        @Param("historyId") Long historyId,
        @Param("userId") Long userId,
        @Param("items") List<YoutubeCommentSentimentItemRow> items
    );

    long countByHistoryId(
        @Param("historyId") Long historyId,
        @Param("userId") Long userId,
        @Param("q") String q,
        @Param("label") String label
    );

    List<YoutubeCommentSentimentItemRow> selectPageByHistoryId(
        @Param("historyId") Long historyId,
        @Param("userId") Long userId,
        @Param("q") String q,
        @Param("label") String label,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    List<YoutubeCommentSentimentItemRow> selectByIds(
        @Param("historyId") Long historyId,
        @Param("userId") Long userId,
        @Param("ids") List<Long> ids
    );

    int updateLlmResult(
        @Param("historyId") Long historyId,
        @Param("userId") Long userId,
        @Param("id") Long id,
        @Param("llmLabel") String llmLabel,
        @Param("finalLabel") String finalLabel,
        @Param("corrected") int corrected,
        @Param("correctedReason") String correctedReason
    );

    List<YoutubeCommentSentimentItemRow> selectSummaryByHistoryId(
        @Param("historyId") Long historyId,
        @Param("userId") Long userId
    );
}
