package com.nesamani.repository;
import com.nesamani.model.Job;
import com.nesamani.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByNeederOrderByCreatedAtDesc(User needer);
    List<Job> findByStatusOrderByCreatedAtDesc(Job.JobStatus status);

    @Query("SELECT j FROM Job j WHERE j.status = 'OPEN' AND " +
           "(:category IS NULL OR LOWER(j.category) LIKE LOWER(CONCAT('%',:category,'%'))) AND " +
           "(:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%',:location,'%'))) " +
           "ORDER BY j.createdAt DESC")
    List<Job> searchOpenJobs(String category, String location);

    long countByNeederAndStatus(User needer, Job.JobStatus status);
}
