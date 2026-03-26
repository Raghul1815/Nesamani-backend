package com.nesamani.repository;
import com.nesamani.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(User.Role role);

    @Query("SELECT u FROM User u WHERE u.role = 'PROVIDER' AND " +
           "(:category IS NULL OR LOWER(u.category) LIKE LOWER(CONCAT('%',:category,'%'))) AND " +
           "(:location IS NULL OR LOWER(u.location) LIKE LOWER(CONCAT('%',:location,'%')))")
    List<User> searchProviders(String category, String location);
}
