package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentResponseDto;

import java.time.LocalDateTime;

public final class CommentMapper {

    private CommentMapper() {
    }

    public static Comment toComment(CommentCreateDto dto, Long itemId, Long authorId) {
        Comment comment = new Comment();
        comment.setText(dto.getText());
        comment.setItemId(itemId);
        comment.setAuthorId(authorId);
        comment.setCreated(LocalDateTime.now());
        return comment;
    }

    public static CommentResponseDto toCommentResponseDto(Comment comment, String authorName) {
        return new CommentResponseDto(
                comment.getId(),
                comment.getText(),
                comment.getAuthorId(),
                authorName,
                comment.getCreated()
        );
    }
}
