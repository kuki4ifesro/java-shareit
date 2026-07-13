package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;
import ru.practicum.shareit.user.UserService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public BookingResponseDto createBooking(BookingCreateDto bookingDto, Long bookerId) {
        userService.getUserById(bookerId);
        Item item = itemRepository.findById(bookingDto.getItemId())
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + bookingDto.getItemId()));

        if (item.getOwnerId().equals(bookerId)) {
            throw new ValidationException("Owner cannot book their own item");
        }

        if (!item.getAvailable()) {
            throw new ValidationException("Item is not available for booking");
        }

        if (bookingDto.getEnd().isBefore(bookingDto.getStart()) || bookingDto.getEnd().equals(bookingDto.getStart())) {
            throw new ValidationException("End date must be after start date");
        }

        boolean hasOverlap = bookingRepository.existsOverlappingBooking(
                item.getId(), bookingDto.getStart(), bookingDto.getEnd(),
                List.of(BookingStatus.WAITING, BookingStatus.APPROVED));
        if (hasOverlap) {
            throw new ValidationException("Item is already booked for the selected period");
        }

        Booking booking = BookingMapper.toBooking(bookingDto, bookerId);
        booking = bookingRepository.save(booking);
        return toResponseDto(booking);
    }

    @Override
    public BookingResponseDto approveBooking(Long bookingId, Boolean approved, Long ownerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + bookingId));

        Long itemId = booking.getItemId();
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + itemId));

        if (!item.getOwnerId().equals(ownerId)) {
            throw new ValidationException("Only owner can approve booking");
        }

        if (!booking.getStatus().equals(BookingStatus.WAITING)) {
            throw new ValidationException("Booking is not in WAITING status");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        booking = bookingRepository.save(booking);
        return toResponseDto(booking);
    }

    @Override
    public BookingResponseDto getBookingById(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + bookingId));

        Item item = itemRepository.findById(booking.getItemId())
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + booking.getItemId()));

        if (!booking.getBookerId().equals(userId) && !item.getOwnerId().equals(userId)) {
            throw new ValidationException("User is not the booker or owner");
        }

        return toResponseDto(booking);
    }

    @Override
    public List<BookingResponseDto> getBookingsByBooker(Long bookerId, String state) {
        userService.getUserById(bookerId);
        BookingState bookingState = BookingState.fromString(state);
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (bookingState) {
            case CURRENT -> bookingRepository.findByBookerIdAndStartBeforeAndEndAfterOrderByStartDesc(bookerId, now);
            case PAST -> bookingRepository.findByBookerIdAndEndBeforeOrderByStartDesc(bookerId, now);
            case FUTURE -> bookingRepository.findByBookerIdAndStartAfterOrderByStartDesc(bookerId, now);
            case WAITING -> bookingRepository.findByBookerIdAndStatusOrderByStartDesc(bookerId, BookingStatus.WAITING);
            case REJECTED -> bookingRepository.findByBookerIdAndStatusOrderByStartDesc(bookerId, BookingStatus.REJECTED);
            case ALL -> bookingRepository.findByBookerIdOrderByStartDesc(bookerId);
        };

        return toResponseDtos(bookings);
    }

    @Override
    public List<BookingResponseDto> getBookingsByOwner(Long ownerId, String state) {
        userService.getUserById(ownerId);
        BookingState bookingState = BookingState.fromString(state);
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (bookingState) {
            case CURRENT -> bookingRepository.findAllByOwnerIdAndStartBeforeAndEndAfterOrderByStartDesc(ownerId, now);
            case PAST -> bookingRepository.findAllByOwnerIdAndEndBeforeOrderByStartDesc(ownerId, now);
            case FUTURE -> bookingRepository.findAllByOwnerIdAndStartAfterOrderByStartDesc(ownerId, now);
            case WAITING -> bookingRepository.findAllByOwnerIdAndStatusOrderByStartDesc(ownerId, BookingStatus.WAITING);
            case REJECTED -> bookingRepository.findAllByOwnerIdAndStatusOrderByStartDesc(ownerId, BookingStatus.REJECTED);
            case ALL -> bookingRepository.findAllByOwnerIdOrderByStartDesc(ownerId);
        };

        return toResponseDtos(bookings);
    }

    private BookingResponseDto toResponseDto(Booking booking) {
        Item item = itemRepository.findById(booking.getItemId())
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + booking.getItemId()));
        User booker = userRepository.findById(booking.getBookerId())
                .orElseThrow(() -> new NotFoundException("User not found with id: " + booking.getBookerId()));
        return BookingMapper.toResponseDto(booking, item.getName(), booker.getName());
    }

    private List<BookingResponseDto> toResponseDtos(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return List.of();
        }

        List<Long> itemIds = bookings.stream().map(Booking::getItemId).distinct().toList();
        List<Long> bookerIds = bookings.stream().map(Booking::getBookerId).distinct().toList();

        Map<Long, String> itemNames = itemRepository.findAllById(itemIds).stream()
                .collect(Collectors.toMap(Item::getId, Item::getName));
        Map<Long, String> bookerNames = userRepository.findAllById(bookerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        return bookings.stream()
                .map(booking -> BookingMapper.toResponseDto(
                        booking,
                        itemNames.get(booking.getItemId()),
                        bookerNames.get(booking.getBookerId())))
                .toList();
    }
}
