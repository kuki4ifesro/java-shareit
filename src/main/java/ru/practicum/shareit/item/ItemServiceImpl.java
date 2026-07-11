package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.Booking.BookingStatus;
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
import java.util.List;

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
        itemDto.setComments(getCommentsForItem(itemId));

        if (item.getOwnerId().equals(userId)) {
            setBookingDates(itemDto, itemId);
        }

        return itemDto;
    }

    @Override
    public List<ItemDto> getItemsByOwner(Long ownerId) {
        return itemRepository.findByOwnerId(ownerId).stream()
                .map(item -> {
                    ItemDto itemDto = ItemMapper.toItemDto(item);
                    itemDto.setComments(getCommentsForItem(item.getId()));
                    setBookingDates(itemDto, item.getId());
                    return itemDto;
                })
                .toList();
    }

    private void setBookingDates(ItemDto itemDto, Long itemId) {
        LocalDateTime now = LocalDateTime.now();

        List<Booking> lastBookings = bookingRepository.findLastBookingByItemId(itemId, now);
        if (!lastBookings.isEmpty()) {
            Booking last = lastBookings.get(0);
            itemDto.setLastBooking(new ItemDto.BookingShort(
                    last.getId(),
                    last.getBookerId(),
                    last.getStart(),
                    last.getEnd()
            ));
        }

        List<Booking> nextBookings = bookingRepository.findNextBookingByItemId(itemId, now);
        if (!nextBookings.isEmpty()) {
            Booking next = nextBookings.get(0);
            itemDto.setNextBooking(new ItemDto.BookingShort(
                    next.getId(),
                    next.getBookerId(),
                    next.getStart(),
                    next.getEnd()
            ));
        }
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
        userService.getUserById(userId);
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + itemId));

        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = bookingRepository.findByBookerIdAndItemIdAndStatusAndEndBeforeOrderByStartDesc(
                userId, itemId, BookingStatus.APPROVED, now);

        if (bookings.isEmpty()) {
            throw new ValidationException("User has not completed a booking for this item");
        }

        Comment comment = new Comment();
        comment.setText(commentDto.getText());
        comment.setItemId(itemId);
        comment.setAuthorId(userId);
        comment.setCreated(LocalDateTime.now());

        comment = commentRepository.save(comment);

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        return new CommentResponseDto(
                comment.getId(),
                comment.getText(),
                comment.getAuthorId(),
                author.getName(),
                comment.getCreated()
        );
    }

    private List<CommentResponseDto> getCommentsForItem(Long itemId) {
        return commentRepository.findByItemIdOrderByCreatedDesc(itemId).stream()
                .map(comment -> {
                    User author = userRepository.findById(comment.getAuthorId())
                            .orElseThrow(() -> new NotFoundException("User not found with id: " + comment.getAuthorId()));
                    return new CommentResponseDto(
                            comment.getId(),
                            comment.getText(),
                            comment.getAuthorId(),
                            author.getName(),
                            comment.getCreated()
                    );
                })
                .toList();
    }
}
