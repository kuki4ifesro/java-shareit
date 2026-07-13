package ru.practicum.shareit.booking;

import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;

public final class BookingMapper {

    private BookingMapper() {
    }

    public static Booking toBooking(BookingCreateDto dto, Long bookerId) {
        Booking booking = new Booking();
        booking.setStart(dto.getStart());
        booking.setEnd(dto.getEnd());
        booking.setStatus(BookingStatus.WAITING);
        booking.setItemId(dto.getItemId());
        booking.setBookerId(bookerId);
        return booking;
    }

    public static BookingResponseDto toResponseDto(Booking booking, String itemName, String bookerName) {
        return new BookingResponseDto(
                booking.getId(),
                booking.getStart(),
                booking.getEnd(),
                booking.getStatus().name(),
                new BookingResponseDto.BookerItem(booking.getBookerId(), bookerName),
                new BookingResponseDto.BookerItem(booking.getItemId(), itemName)
        );
    }
}
