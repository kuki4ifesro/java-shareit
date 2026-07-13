package ru.practicum.shareit.booking;

import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;

import java.util.List;

public interface BookingService {
    BookingResponseDto createBooking(BookingCreateDto bookingDto, Long bookerId);

    BookingResponseDto approveBooking(Long bookingId, Boolean approved, Long ownerId);

    BookingResponseDto getBookingById(Long bookingId, Long userId);

    List<BookingResponseDto> getBookingsByBooker(Long bookerId, String state);

    List<BookingResponseDto> getBookingsByOwner(Long ownerId, String state);
}
