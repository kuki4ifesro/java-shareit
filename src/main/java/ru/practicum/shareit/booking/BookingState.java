package ru.practicum.shareit.booking;

/**
 * Состояние бронирования для фильтрации в параметре запроса {@code state}.
 * Не путать с {@link Booking.BookingStatus} — статусом самой сущности бронирования,
 * который хранится в базе данных.
 */
public enum BookingState {
    ALL,
    CURRENT,
    PAST,
    FUTURE,
    WAITING,
    REJECTED
}
