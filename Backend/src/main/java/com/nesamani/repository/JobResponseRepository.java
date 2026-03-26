package com.nesamani.repository;
import com.nesamani.model.Job;
import com.nesamani.model.JobResponse;
import com.nesamani.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobResponseRepository extends JpaRepository<JobResponse, Long> {
    List<JobResponse> findByJobOrderByRespondedAtDesc(Job job);
    List<JobResponse> findByProviderOrderByRespondedAtDesc(User provider);
    boolean existsByJobAndProvider(Job job, User provider);
    Optional<JobResponse> findByJobAndProvider(Job job, User provider);
    List<JobResponse> findByProviderAndStatusOrderByRespondedAtDesc(User provider, JobResponse.ResponseStatus status);
    long countByJob(Job job);
}
