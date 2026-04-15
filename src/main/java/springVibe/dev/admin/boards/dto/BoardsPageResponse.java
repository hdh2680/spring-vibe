package springVibe.dev.admin.boards.dto;

import springVibe.dev.admin.boards.domain.Board;

import java.util.List;

public record BoardsPageResponse(
    List<Board> items,
    long total,
    int page,
    int size,
    int totalPages
) {}

