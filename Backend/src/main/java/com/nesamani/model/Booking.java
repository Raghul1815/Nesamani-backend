package com.nesamani.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * Booking = confirmed work agreement.
 * Created in TWO ways:
 *   Flow A: Needer accepts a JobResponse  → job_id set,     service_id null
 *   Flow B: Needer books a Service        → job_id null,     service_id set
 *
 * Status path: PENDING → ACCEPTED → IN_PROGRESS → COMPLETED
 */
@Entity
@Table(name = "bookings")
public class Booking {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "needer_id", nullable = false)
    private User needer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private User provider;

    /** Flow A — nullable for Flow B */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    /** Flow B — nullable for Flow A */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private Service service;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private String agreedPrice;

    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.PENDING;

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum BookingStatus { PENDING, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED }

    // ── Constructors ───────────────────────────────────────────
    public Booking() {}

    // ── Getters & Setters ──────────────────────────────────────
    public Long          getId()                     { return id; }
    public void          setId(Long id)              { this.id = id; }
    public User          getNeeder()                 { return needer; }
    public void          setNeeder(User n)           { this.needer = n; }
    public User          getProvider()               { return provider; }
    public void          setProvider(User p)         { this.provider = p; }
    public Job           getJob()                    { return job; }
    public void          setJob(Job j)               { this.job = j; }
    public Service       getService()                { return service; }
    public void          setService(Service s)       { this.service = s; }
    public String        getNotes()                  { return notes; }
    public void          setNotes(String n)          { this.notes = n; }
    public String        getAgreedPrice()            { return agreedPrice; }
    public void          setAgreedPrice(String p)    { this.agreedPrice = p; }
    public LocalDateTime getScheduledAt()            { return scheduledAt; }
    public void          setScheduledAt(LocalDateTime t) { this.scheduledAt = t; }
    public BookingStatus getStatus()                 { return status; }
    public void          setStatus(BookingStatus s)  { this.status = s; }
    public LocalDateTime getCreatedAt()              { return createdAt; }
    public LocalDateTime getUpdatedAt()              { return updatedAt; }
}
