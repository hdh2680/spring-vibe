package springVibe.dev.admin.boards.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import springVibe.dev.admin.boards.domain.Board;
import springVibe.dev.admin.boards.service.BoardsService;

import java.util.List;

/**
 * Public (or app-facing) boards endpoints used by /app pages.
 */
@RestController
@RequestMapping("/api/boards")
public class BoardsApiController {
    private final BoardsService boardsService;

    public BoardsApiController(BoardsService boardsService) {
        this.boardsService = boardsService;
    }

    @GetMapping
    public List<Board> listEnabledPublic() {
        return boardsService.findAllEnabledPublic();
    }

    @GetMapping("/{boardKey}")
    public Board getByKey(@PathVariable String boardKey) {
        Board b = boardsService.findByBoardKey(boardKey);
        if (b == null || Boolean.FALSE.equals(b.getIsEnabled()) || Boolean.FALSE.equals(b.getIsPublicRead())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "게시판을 찾을 수 없습니다.");
        }
        return b;
    }
}

