package springVibe.dev.users.youtubeComment.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import springVibe.dev.users.youtubeComment.dto.YoutubeCommentLexiconSuggestionRow;

import java.util.List;

@Mapper
public interface YoutubeCommentLexiconSuggestionMapper {
    int insertBatch(
        @Param("historyId") Long historyId,
        @Param("userId") Long userId,
        @Param("sentimentItemId") Long sentimentItemId,
        @Param("items") List<YoutubeCommentLexiconSuggestionRow> items
    );

    List<YoutubeCommentLexiconSuggestionRow> selectPendingByHistoryId(
        @Param("historyId") Long historyId,
        @Param("userId") Long userId,
        @Param("limit") int limit
    );

    List<YoutubeCommentLexiconSuggestionRow> selectByIds(
        @Param("historyId") Long historyId,
        @Param("userId") Long userId,
        @Param("ids") List<Long> ids
    );

    int markAppliedByIds(
        @Param("ids") List<Long> ids,
        @Param("historyId") Long historyId,
        @Param("userId") Long userId
    );

    int markRejectedByIds(
        @Param("ids") List<Long> ids,
        @Param("historyId") Long historyId,
        @Param("userId") Long userId
    );
}
