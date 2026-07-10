package ru.practicum.shareit.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.booking.Booking.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByBookerIdOrderByStartDesc(Long bookerId);

    List<Booking> findByItemIdOrderByStartDesc(Long itemId);

    List<Booking> findByBookerIdAndStatusOrderByStartDesc(Long bookerId, BookingStatus status);

    List<Booking> findByItemIdAndStatusOrderByStartDesc(Long itemId, BookingStatus status);

    List<Booking> findByBookerIdAndStartAfterOrderByStartDesc(Long bookerId, LocalDateTime start);

    List<Booking> findByBookerIdAndEndBeforeOrderByStartDesc(Long bookerId, LocalDateTime end);

    List<Booking> findByItemIdAndStartAfterOrderByStartDesc(Long itemId, LocalDateTime start);

    List<Booking> findByItemIdAndEndBeforeOrderByStartDesc(Long itemId, LocalDateTime end);

    @Query("SELECT b FROM Booking b JOIN Item i ON b.itemId = i.id WHERE i.ownerId = :ownerId ORDER BY b.start DESC")
    List<Booking> findAllByOwnerIdOrderByStartDesc(Long ownerId);

    @Query("SELECT b FROM Booking b JOIN Item i ON b.itemId = i.id WHERE i.ownerId = :ownerId AND b.start < :now AND b.end > :now ORDER BY b.start DESC")
    List<Booking> findAllByOwnerIdAndStartBeforeAndEndAfterOrderByStartDesc(Long ownerId, LocalDateTime now, LocalDateTime now2);

    @Query("SELECT b FROM Booking b JOIN Item i ON b.itemId = i.id WHERE i.ownerId = :ownerId AND b.end < :now ORDER BY b.start DESC")
    List<Booking> findAllByOwnerIdAndEndBeforeOrderByStartDesc(Long ownerId, LocalDateTime now);

    @Query("SELECT b FROM Booking b JOIN Item i ON b.itemId = i.id WHERE i.ownerId = :ownerId AND b.start > :now ORDER BY b.start DESC")
    List<Booking> findAllByOwnerIdAndStartAfterOrderByStartDesc(Long ownerId, LocalDateTime now);

    @Query("SELECT b FROM Booking b JOIN Item i ON b.itemId = i.id WHERE i.ownerId = :ownerId AND b.status = :status ORDER BY b.start DESC")
    List<Booking> findAllByOwnerIdAndStatusOrderByStartDesc(Long ownerId, BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.bookerId = :bookerId AND b.start < :now AND b.end > :now ORDER BY b.start DESC")
    List<Booking> findByBookerIdAndStartBeforeAndEndAfterOrderByStartDesc(Long bookerId, LocalDateTime now, LocalDateTime now2);

    @Query("SELECT b FROM Booking b WHERE b.bookerId = :bookerId AND b.itemId = :itemId AND b.status = :status AND b.end < :now ORDER BY b.start DESC")
    List<Booking> findByBookerIdAndItemIdAndStatusAndEndBeforeOrderByStartDesc(Long bookerId, Long itemId, BookingStatus status, LocalDateTime now);
}
