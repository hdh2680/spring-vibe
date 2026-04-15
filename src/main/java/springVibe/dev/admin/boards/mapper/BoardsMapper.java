package springVibe.dev.admin.boards.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import springVibe.dev.admin.boards.domain.Board;

import java.util.List;

@Mapper
public interface BoardsMapper {
    List<Board> findAll();

    long count(@Param("q") String q, @Param("enabled") Integer enabled);

    List<Board> findPage(
        @Param("q") String q,
        @Param("enabled") Integer enabled,
        @Param("offset") int offset,
        @Param("size") int size
    );

    List<Board> findAllEnabledPublic();

    Board findById(@Param("id") Long id);

    Board findByBoardKey(@Param("boardKey") String boardKey);

    int insert(Board board);

    int update(Board board);

    int delete(@Param("id") Long id);
}
