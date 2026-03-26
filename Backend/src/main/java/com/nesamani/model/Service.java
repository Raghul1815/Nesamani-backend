package com.nesamani.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * Flow B: PROVIDER uploads a Service → NEEDERs browse and book directly.
 */
@Entity
@Table(name = "services")
public class Service {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private User provider;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String category;

    private String price;

    @Enumerated(EnumType.STRING)
    private PriceType priceType = PriceType.NEGOTIABLE;

    private String location;

    private Boolean isAvailable = true;

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum PriceType { FIXED, PER_HOUR, PER_DAY, PER_MONTH, NEGOTIABLE }

    // ── Constructors ───────────────────────────────────────────
    public Service() {}

    // ── Getters & Setters ──────────────────────────────────────
    public Long      getId()                   { return id; }
    public void      setId(Long id)            { this.id = id; }
    public User      getProvider()             { return provider; }
    public void      setProvider(User p)       { this.provider = p; }
    public String    getTitle()                { return title; }
    public void      setTitle(String t)        { this.title = t; }
    public String    getDescription()          { return description; }
    public void      setDescription(String d)  { this.description = d; }
    public String    getCategory()             { return category; }
    public void      setCategory(String c)     { this.category = c; }
    public String    getPrice()                { return price; }
    public void      setPrice(String p)        { this.price = p; }
    public PriceType getPriceType()            { return priceType; }
    public void      setPriceType(PriceType t) { this.priceType = t; }
    public String    getLocation()             { return location; }
    public void      setLocation(String l)     { this.location = l; }
    public Boolean   getIsAvailable()          { return isAvailable; }
    public void      setIsAvailable(Boolean a) { this.isAvailable = a; }
    public LocalDateTime getCreatedAt()        { return createdAt; }
    public LocalDateTime getUpdatedAt()        { return updatedAt; }
}
