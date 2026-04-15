package springVibe.dev.admin.boards.service;

import org.springframework.stereotype.Service;
import springVibe.dev.admin.boards.domain.Board;
import springVibe.dev.admin.boards.dto.BoardsPageResponse;
import springVibe.dev.admin.boards.dto.BoardUpsertRequest;
import springVibe.dev.admin.boards.mapper.BoardsMapper;
import springVibe.system.exception.BaseException;

import java.util.List;

@Service
public class BoardsService {
    private final BoardsMapper boardsMapper;

    public BoardsService(BoardsMapper boardsMapper) {
        this.boardsMapper = boardsMapper;
    }

    public BoardsPageResponse findPage(String q, Integer enabled, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = safePage * safeSize;

        String qq = q == null ? "" : q.trim();
        if (qq.isBlank()) qq = "";

        long total = boardsMapper.count(qq, enabled);
        int totalPages = computeTotalPages(total, safeSize);

        // Clamp to last page if the requested page is out of range.
        if (totalPages > 0 && safePage >= totalPages) {
            safePage = totalPages - 1;
            offset = safePage * safeSize;
        }

        List<Board> items = total == 0 ? List.of() : boardsMapper.findPage(qq, enabled, offset, safeSize);
        return new BoardsPageResponse(items, total, safePage, safeSize, totalPages);
    }

    public List<Board> findAll() {
        return boardsMapper.findAll();
    }

    public List<Board> findAllEnabledPublic() {
        return boardsMapper.findAllEnabledPublic();
    }

    public Board findById(Long id) {
        return boardsMapper.findById(id);
    }

    public Board findByBoardKey(String boardKey) {
        return boardsMapper.findByBoardKey(boardKey);
    }

    public Long create(BoardUpsertRequest req) {
        Board existing = boardsMapper.findByBoardKey(req.getBoardKey());
        if (existing != null) {
            throw new BaseException("BOARD_KEY_DUP", "이미 존재하는 boardKey 입니다.");
        }

        Board b = new Board();
        b.setBoardKey(req.getBoardKey());
        b.setName(req.getName());
        b.setDescription(req.getDescription());
        b.setIsEnabled(req.getIsEnabled());
        b.setIsPublicRead(req.getIsPublicRead());
        b.setIsPublicWrite(req.getIsPublicWrite());
        b.setSortOrder(req.getSortOrder());

        boardsMapper.insert(b);
        return b.getId();
    }

    public void update(Long id, BoardUpsertRequest req) {
        Board cur = boardsMapper.findById(id);
        if (cur == null) {
            throw new BaseException("BOARD_NOT_FOUND", "게시판을 찾을 수 없습니다.");
        }

        Board byKey = boardsMapper.findByBoardKey(req.getBoardKey());
        if (byKey != null && !byKey.getId().equals(id)) {
            throw new BaseException("BOARD_KEY_DUP", "이미 존재하는 boardKey 입니다.");
        }

        cur.setBoardKey(req.getBoardKey());
        cur.setName(req.getName());
        cur.setDescription(req.getDescription());
        cur.setIsEnabled(req.getIsEnabled());
        cur.setIsPublicRead(req.getIsPublicRead());
        cur.setIsPublicWrite(req.getIsPublicWrite());
        cur.setSortOrder(req.getSortOrder());

        boardsMapper.update(cur);
    }

    public void delete(Long id) {
        boardsMapper.delete(id);
    }

    private static int computeTotalPages(long total, int size) {
        if (total <= 0L) return 0;
        long pages = (total + size - 1L) / (long) size;
        return pages > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) pages;
    }
}
