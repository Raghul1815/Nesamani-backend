package com.nesamani.repository;
import com.nesamani.model.Booking;
import com.nesamani.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByNeederOrderByCreatedAtDesc(User needer);
    List<Booking> findByProviderOrderByCreatedAtDesc(User provider);
    List<Booking> findByNeederAndStatusOrderByCreatedAtDesc(User needer, Booking.BookingStatus status);
    List<Booking> findByProviderAndStatusOrderByCreatedAtDesc(User provider, Booking.BookingStatus status);
    long countByProviderAndStatus(User provider, Booking.BookingStatus status);
}
