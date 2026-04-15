package springVibe.dev.admin.boards.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import springVibe.dev.admin.boards.domain.Board;
import springVibe.dev.admin.boards.dto.BoardUpsertRequest;
import springVibe.dev.admin.boards.dto.BoardsPageResponse;
import springVibe.dev.admin.boards.service.BoardsService;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/boards")
public class BoardsAdminApiController {
    private final BoardsService boardsService;

    public BoardsAdminApiController(BoardsService boardsService) {
        this.boardsService = boardsService;
    }

    @GetMapping
    public BoardsPageResponse list(
        @RequestParam(value = "q", required = false) String q,
        @RequestParam(value = "enabled", required = false) Integer enabled,
        @RequestParam(value = "page", required = false, defaultValue = "0") int page,
        @RequestParam(value = "size", required = false, defaultValue = "20") int size,
        Authentication authentication
    ) {
        requireAdmin(authentication);
        return boardsService.findPage(q, enabled, page, size);
    }

    @GetMapping("/{id}")
    public Board get(@PathVariable Long id, Authentication authentication) {
        requireAdmin(authentication);
        Board b = boardsService.findById(id);
        if (b == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "게시판을 찾을 수 없습니다.");
        return b;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody BoardUpsertRequest req, Authentication authentication) {
        requireAdmin(authentication);
        Long id = boardsService.create(req);
        return Map.of("id", id);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @Valid @RequestBody BoardUpsertRequest req, Authentication authentication) {
        requireAdmin(authentication);
        boardsService.update(id, req);
        return Map.of("success", true);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id, Authentication authentication) {
        requireAdmin(authentication);
        boardsService.delete(id);
        return Map.of("success", true);
    }

    private static void requireAdmin(Authentication authentication) {
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        boolean isAdmin = authentication.getAuthorities() != null
            && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }
    }
}
