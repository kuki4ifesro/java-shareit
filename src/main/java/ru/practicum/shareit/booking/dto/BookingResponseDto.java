package ru.practicum.shareit.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDto {
    private Long id;
    private LocalDateTime start;
    private LocalDateTime end;
    private String status;
    private BookerItem booker;
    private BookerItem item;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookerItem {
        private Long id;
        private String name;
    }
}
