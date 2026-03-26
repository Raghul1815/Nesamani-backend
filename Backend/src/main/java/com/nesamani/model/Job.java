package com.nesamani.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Flow A: NEEDER posts a Job → PROVIDERs see it and respond via JobResponse.
 */
@Entity
@Table(name = "jobs")
public class Job {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "needer_id", nullable = false)
    private User needer;              // Who posted the job

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;
    private String location;
    private String budget;
    private String duration;
    private LocalDate expectedDate;

    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.OPEN;

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum JobStatus { OPEN, IN_PROGRESS, COMPLETED, CANCELLED }

    // ── Constructors ───────────────────────────────────────────
    public Job() {}

    // ── Getters & Setters ──────────────────────────────────────
    public Long         getId()                    { return id; }
    public void         setId(Long id)             { this.id = id; }
    public User         getNeeder()                { return needer; }
    public void         setNeeder(User n)          { this.needer = n; }
    public String       getTitle()                 { return title; }
    public void         setTitle(String t)         { this.title = t; }
    public String       getDescription()           { return description; }
    public void         setDescription(String d)   { this.description = d; }
    public String       getCategory()              { return category; }
    public void         setCategory(String c)      { this.category = c; }
    public String       getLocation()              { return location; }
    public void         setLocation(String l)      { this.location = l; }
    public String       getBudget()                { return budget; }
    public void         setBudget(String b)        { this.budget = b; }
    public String       getDuration()              { return duration; }
    public void         setDuration(String d)      { this.duration = d; }
    public LocalDate    getExpectedDate()          { return expectedDate; }
    public void         setExpectedDate(LocalDate d){ this.expectedDate = d; }
    public JobStatus    getStatus()                { return status; }
    public void         setStatus(JobStatus s)     { this.status = s; }
    public LocalDateTime getCreatedAt()            { return createdAt; }
    public LocalDateTime getUpdatedAt()            { return updatedAt; }
}
