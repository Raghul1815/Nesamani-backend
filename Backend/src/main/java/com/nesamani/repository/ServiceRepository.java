package com.nesamani.repository;
import com.nesamani.model.Service;
import com.nesamani.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {
    List<Service> findByProviderOrderByCreatedAtDesc(User provider);
    List<Service> findByIsAvailableTrueOrderByCreatedAtDesc();

    @Query("SELECT s FROM Service s WHERE s.isAvailable = true AND " +
           "(:category IS NULL OR LOWER(s.category) LIKE LOWER(CONCAT('%',:category,'%'))) AND " +
           "(:location IS NULL OR LOWER(s.location) LIKE LOWER(CONCAT('%',:location,'%'))) " +
           "ORDER BY s.createdAt DESC")
    List<Service> searchServices(String category, String location);
}
