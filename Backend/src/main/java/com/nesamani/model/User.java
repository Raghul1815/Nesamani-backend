package com.nesamani.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * Shared user entity.
 * NEEDER   → posts jobs, browses services, books providers  → needer-dashboard.html
 * PROVIDER → uploads services, responds to jobs, gets booked → provider-dashboard.html
 *
 * Role is stored as NEEDER/PROVIDER in DB but sent to frontend as lowercase "needer"/"provider".
 */
@Entity
@Table(name = "users")
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;          // BCrypt hashed

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;                // NEEDER or PROVIDER

    private String location;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String category;          // Provider's main skill category

    private Double  rating       = 0.0;
    private Integer jobsCompleted = 0;

    @Enumerated(EnumType.STRING)
    private Availability availability = Availability.AVAILABLE;

    private Boolean isActive = true;

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ── Enums ─────────────────────────────────────────────────
    public enum Role {
        NEEDER,    // Work giver  — lowercase "needer" sent to frontend
        PROVIDER   // Work doer   — lowercase "provider" sent to frontend
    }

    public enum Availability { AVAILABLE, BUSY, OFFLINE }

    // ── Constructors ───────────────────────────────────────────
    public User() {}

    // ── Getters & Setters ──────────────────────────────────────
    public Long   getId()                        { return id; }
    public void   setId(Long id)                 { this.id = id; }
    public String getName()                      { return name; }
    public void   setName(String n)              { this.name = n; }
    public String getEmail()                     { return email; }
    public void   setEmail(String e)             { this.email = e; }
    public String getPassword()                  { return password; }
    public void   setPassword(String p)          { this.password = p; }
    public String getPhone()                     { return phone; }
    public void   setPhone(String p)             { this.phone = p; }
    public Role   getRole()                      { return role; }
    public void   setRole(Role r)                { this.role = r; }
    public String getLocation()                  { return location; }
    public void   setLocation(String l)          { this.location = l; }
    public String getBio()                       { return bio; }
    public void   setBio(String b)               { this.bio = b; }
    public String getCategory()                  { return category; }
    public void   setCategory(String c)          { this.category = c; }
    public Double getRating()                    { return rating; }
    public void   setRating(Double r)            { this.rating = r; }
    public Integer getJobsCompleted()            { return jobsCompleted; }
    public void    setJobsCompleted(Integer j)   { this.jobsCompleted = j; }
    public Availability getAvailability()        { return availability; }
    public void setAvailability(Availability a)  { this.availability = a; }
    public Boolean getIsActive()                 { return isActive; }
    public void    setIsActive(Boolean a)        { this.isActive = a; }
    public LocalDateTime getCreatedAt()          { return createdAt; }
    public LocalDateTime getUpdatedAt()          { return updatedAt; }

    /** Convenience: returns lowercase role string for frontend */
    public String getRoleName() {
        return role == null ? "" : role.name().toLowerCase();
    }
}
