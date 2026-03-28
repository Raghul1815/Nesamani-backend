package com.nesamani.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * Flow A: Provider responds to a Job posted by a Needer.
 * Needer sees all responses → accepts one → Booking is created.
 */
@Entity
@Table(name = "job_responses",
       uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "provider_id"}))
public class JobResponse {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private User provider;

    @Column(columnDefinition = "TEXT")
    private String message;

    private String quotedPrice;

    @Enumerated(EnumType.STRING)
    private ResponseStatus status = ResponseStatus.PENDING;

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime respondedAt;

    public enum ResponseStatus { PENDING, ACCEPTED, REJECTED, WITHDRAWN }

    // ── Constructors ───────────────────────────────────────────
    public JobResponse() {}

    public JobResponse(Job job, User provider) {
        this.job      = job;
        this.provider = provider;
        this.status   = ResponseStatus.PENDING;
    }

    // ── Getters & Setters ──────────────────────────────────────
    public Long           getId()                    { return id; }
    public void           setId(Long id)             { this.id = id; }
    public Job            getJob()                   { return job; }
    public void           setJob(Job j)              { this.job = j; }
    public User           getProvider()              { return provider; }
    public void           setProvider(User p)        { this.provider = p; }
    public String         getMessage()               { return message; }
    public void           setMessage(String m)       { this.message = m; }
    public String         getQuotedPrice()           { return quotedPrice; }
    public void           setQuotedPrice(String p)   { this.quotedPrice = p; }
    public ResponseStatus getStatus()                { return status; }
    public void           setStatus(ResponseStatus s){ this.status = s; }
    public LocalDateTime  getRespondedAt()           { return respondedAt; }
}
