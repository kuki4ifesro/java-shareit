package ru.practicum.shareit.booking;

import ru.practicum.shareit.exception.ValidationException;

/**
 * Состояние бронирования для фильтрации в параметре запроса {@code state}.
 * Не путать с {@link BookingStatus} — статусом самой сущности бронирования,
 * который хранится в базе данных.
 */
public enum BookingState {
    ALL,
    CURRENT,
    PAST,
    FUTURE,
    WAITING,
    REJECTED;

    public static BookingState fromString(String state) {
        if (state == null) {
            return ALL;
        }
        try {
            return BookingState.valueOf(state.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unknown state: " + state);
        }
    }
}
