package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentResponseDto;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemUpdateDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.UserService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserService userService;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Override
    public ItemDto createItem(ItemCreateDto itemDto, Long ownerId) {
        userService.getUserById(ownerId);
        Item item = ItemMapper.toItem(itemDto, ownerId);
        item = itemRepository.save(item);
        return ItemMapper.toItemDto(item);
    }

    @Override
    public ItemDto updateItem(Long itemId, ItemUpdateDto itemDto, Long ownerId) {
        Item existingItem = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + itemId));

        if (!existingItem.getOwnerId().equals(ownerId)) {
            throw new NotFoundException("User is not the owner of this item");
        }

        if (itemDto.getName() != null && !itemDto.getName().isBlank()) {
            existingItem.setName(itemDto.getName());
        }

        if (itemDto.getDescription() != null && !itemDto.getDescription().isBlank()) {
            existingItem.setDescription(itemDto.getDescription());
        }

        if (itemDto.getAvailable() != null) {
            existingItem.setAvailable(itemDto.getAvailable());
        }

        existingItem = itemRepository.save(existingItem);
        return ItemMapper.toItemDto(existingItem);
    }

    @Override
    public ItemDto getItemById(Long itemId, Long userId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + itemId));
        ItemDto itemDto = ItemMapper.toItemDto(item);

        List<Comment> comments = commentRepository.findByItemIdOrderByCreatedDesc(itemId);
        itemDto.setComments(toCommentResponseDtos(comments));

        if (item.getOwnerId().equals(userId)) {
            List<Booking> itemBookings = bookingRepository.findByItemIdInAndStatusOrderByStartAsc(
                    List.of(itemId), BookingStatus.APPROVED);
            applyBookingDates(itemDto, itemBookings, LocalDateTime.now());
        }

        return itemDto;
    }

    @Override
    public List<ItemDto> getItemsByOwner(Long ownerId) {
        List<Item> items = itemRepository.findByOwnerId(ownerId);
        if (items.isEmpty()) {
            return List.of();
        }

        List<Long> itemIds = items.stream().map(Item::getId).toList();

        Map<Long, List<CommentResponseDto>> commentsByItem = groupCommentsByItem(
                commentRepository.findByItemIdInOrderByCreatedDesc(itemIds));

        Map<Long, List<Booking>> bookingsByItem = bookingRepository
                .findByItemIdInAndStatusOrderByStartAsc(itemIds, BookingStatus.APPROVED).stream()
                .collect(Collectors.groupingBy(Booking::getItemId));

        LocalDateTime now = LocalDateTime.now();

        return items.stream()
                .map(item -> {
                    ItemDto itemDto = ItemMapper.toItemDto(item);
                    itemDto.setComments(commentsByItem.getOrDefault(item.getId(), List.of()));
                    applyBookingDates(itemDto, bookingsByItem.getOrDefault(item.getId(), List.of()), now);
                    return itemDto;
                })
                .toList();
    }

    private void applyBookingDates(ItemDto itemDto, List<Booking> itemBookings, LocalDateTime now) {
        itemBookings.stream()
                .filter(b -> b.getEnd().isBefore(now))
                .max(Comparator.comparing(Booking::getEnd))
                .ifPresent(last -> itemDto.setLastBooking(new ItemDto.BookingShort(
                        last.getId(), last.getBookerId(), last.getStart(), last.getEnd())));

        itemBookings.stream()
                .filter(b -> b.getStart().isAfter(now))
                .min(Comparator.comparing(Booking::getStart))
                .ifPresent(next -> itemDto.setNextBooking(new ItemDto.BookingShort(
                        next.getId(), next.getBookerId(), next.getStart(), next.getEnd())));
    }

    @Override
    public List<ItemDto> searchItems(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        return itemRepository.searchAvailableByText(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    @Override
    public CommentResponseDto addComment(Long itemId, CommentCreateDto commentDto, Long userId) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
        itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + itemId));

        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = bookingRepository.findByBookerIdAndItemIdAndStatusAndEndBeforeOrderByStartDesc(
                userId, itemId, BookingStatus.APPROVED, now);

        if (bookings.isEmpty()) {
            throw new ValidationException("User has not completed a booking for this item");
        }

        Comment comment = CommentMapper.toComment(commentDto, itemId, userId);
        comment = commentRepository.save(comment);

        return CommentMapper.toCommentResponseDto(comment, author.getName());
    }

    private List<CommentResponseDto> toCommentResponseDtos(List<Comment> comments) {
        if (comments.isEmpty()) {
            return List.of();
        }
        Map<Long, String> authorNames = resolveAuthorNames(comments);
        return comments.stream()
                .map(comment -> CommentMapper.toCommentResponseDto(comment, authorNames.get(comment.getAuthorId())))
                .toList();
    }

    private Map<Long, List<CommentResponseDto>> groupCommentsByItem(List<Comment> comments) {
        if (comments.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> authorNames = resolveAuthorNames(comments);
        return comments.stream()
                .collect(Collectors.groupingBy(Comment::getItemId,
                        Collectors.mapping(comment -> CommentMapper.toCommentResponseDto(
                                comment, authorNames.get(comment.getAuthorId())), Collectors.toList())));
    }

    private Map<Long, String> resolveAuthorNames(List<Comment> comments) {
        List<Long> authorIds = comments.stream().map(Comment::getAuthorId).distinct().toList();
        return userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));
    }
}
