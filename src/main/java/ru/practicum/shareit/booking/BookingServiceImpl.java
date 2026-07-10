package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.Booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.UserService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
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

        Booking booking = new Booking();
        booking.setStart(bookingDto.getStart());
        booking.setEnd(bookingDto.getEnd());
        booking.setStatus(BookingStatus.WAITING);
        booking.setItemId(bookingDto.getItemId());
        booking.setBookerId(bookerId);

        booking = bookingRepository.save(booking);
        return toResponseDto(booking);
    }

    @Override
    public BookingResponseDto approveBooking(Long bookingId, Boolean approved, Long ownerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + bookingId));

        Item item = itemRepository.findById(booking.getItemId())
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + booking.getItemId()));

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
        BookingStatus status = parseState(state);
        LocalDateTime now = LocalDateTime.now();

        return switch (status) {
            case ALL -> bookingRepository.findByBookerIdOrderByStartDesc(bookerId);
            case CURRENT -> bookingRepository.findByBookerIdAndStartBeforeAndEndAfterOrderByStartDesc(bookerId, now, now);
            case PAST -> bookingRepository.findByBookerIdAndEndBeforeOrderByStartDesc(bookerId, now);
            case FUTURE -> bookingRepository.findByBookerIdAndStartAfterOrderByStartDesc(bookerId, now);
            case WAITING -> bookingRepository.findByBookerIdAndStatusOrderByStartDesc(bookerId, BookingStatus.WAITING);
            case REJECTED -> bookingRepository.findByBookerIdAndStatusOrderByStartDesc(bookerId, BookingStatus.REJECTED);
            default -> bookingRepository.findByBookerIdOrderByStartDesc(bookerId);
        }.stream().map(this::toResponseDto).toList();
    }

    @Override
    public List<BookingResponseDto> getBookingsByOwner(Long ownerId, String state) {
        userService.getUserById(ownerId);
        BookingStatus status = parseState(state);
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (status) {
            case ALL -> bookingRepository.findAllByOwnerIdOrderByStartDesc(ownerId);
            case CURRENT -> bookingRepository.findAllByOwnerIdAndStartBeforeAndEndAfterOrderByStartDesc(ownerId, now, now);
            case PAST -> bookingRepository.findAllByOwnerIdAndEndBeforeOrderByStartDesc(ownerId, now);
            case FUTURE -> bookingRepository.findAllByOwnerIdAndStartAfterOrderByStartDesc(ownerId, now);
            case WAITING -> bookingRepository.findAllByOwnerIdAndStatusOrderByStartDesc(ownerId, BookingStatus.WAITING);
            case REJECTED -> bookingRepository.findAllByOwnerIdAndStatusOrderByStartDesc(ownerId, BookingStatus.REJECTED);
            default -> bookingRepository.findAllByOwnerIdOrderByStartDesc(ownerId);
        };

        return bookings.stream().map(this::toResponseDto).toList();
    }

    private BookingStatus parseState(String state) {
        if (state == null) {
            return BookingStatus.ALL;
        }
        try {
            return BookingStatus.valueOf(state.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unknown state: " + state);
        }
    }

    private BookingResponseDto toResponseDto(Booking booking) {
        return new BookingResponseDto(
                booking.getId(),
                booking.getStart(),
                booking.getEnd(),
                booking.getStatus().name(),
                booking.getItemId(),
                booking.getBookerId()
        );
    }
}
