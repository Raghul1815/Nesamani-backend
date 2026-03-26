package com.nesamani.dto;

/**
 * All DTOs used by controllers.
 * Role in all responses is lowercase: "needer" or "provider"
 */
public class Dto {

    // ── AUTH ──────────────────────────────────────────────────

    public static class RegisterRequest {
        private String name, email, phone, password;
        private String role;   // "needer" or "provider"
        public String getName()     { return name; }     public void setName(String v)     { name = v; }
        public String getEmail()    { return email; }    public void setEmail(String v)    { email = v; }
        public String getPhone()    { return phone; }    public void setPhone(String v)    { phone = v; }
        public String getPassword() { return password; } public void setPassword(String v) { password = v; }
        public String getRole()     { return role; }     public void setRole(String v)     { role = v; }
    }

    public static class LoginRequest {
        private String email, password;
        public String getEmail()    { return email; }    public void setEmail(String v)    { email = v; }
        public String getPassword() { return password; } public void setPassword(String v) { password = v; }
    }

    /** Login response — role MUST be lowercase "needer" or "provider" for frontend auth.js */
    public static class AuthResponse {
        private final String token, role, name, email, phone;
        private final Long   userId;
        public AuthResponse(String token, String role, String name, Long userId, String email, String phone) {
            this.token=token; this.role=role; this.name=name; this.userId=userId; this.email=email; this.phone=phone;
        }
        public String getToken()   { return token; }
        public String getRole()    { return role; }   // "needer" or "provider"
        public String getName()    { return name; }
        public Long   getUserId()  { return userId; }
        public String getEmail()   { return email; }
        public String getPhone()   { return phone; }
    }

    // ── JOB ───────────────────────────────────────────────────

    public static class JobRequest {
        private String title, category, location, budget, duration, description, date;
        public String getTitle()       { return title; }       public void setTitle(String v)       { title=v; }
        public String getCategory()    { return category; }    public void setCategory(String v)    { category=v; }
        public String getLocation()    { return location; }    public void setLocation(String v)    { location=v; }
        public String getBudget()      { return budget; }      public void setBudget(String v)      { budget=v; }
        public String getDuration()    { return duration; }    public void setDuration(String v)    { duration=v; }
        public String getDescription() { return description; } public void setDescription(String v) { description=v; }
        public String getDate()        { return date; }        public void setDate(String v)        { date=v; }
    }

    // ── SERVICE ───────────────────────────────────────────────

    public static class ServiceRequest {
        private String title, category, price, priceType, location, description;
        private Boolean isAvailable;
        public String  getTitle()       { return title; }       public void setTitle(String v)       { title=v; }
        public String  getCategory()    { return category; }    public void setCategory(String v)    { category=v; }
        public String  getPrice()       { return price; }       public void setPrice(String v)       { price=v; }
        public String  getPriceType()   { return priceType; }   public void setPriceType(String v)   { priceType=v; }
        public String  getLocation()    { return location; }    public void setLocation(String v)    { location=v; }
        public String  getDescription() { return description; } public void setDescription(String v) { description=v; }
        public Boolean getIsAvailable() { return isAvailable; } public void setIsAvailable(Boolean v){ isAvailable=v; }
    }

    // ── JOB RESPONSE ─────────────────────────────────────────

    public static class JobResponseRequest {
        private String message, quotedPrice;
        public String getMessage()     { return message; }     public void setMessage(String v)     { message=v; }
        public String getQuotedPrice() { return quotedPrice; } public void setQuotedPrice(String v) { quotedPrice=v; }
    }

    // ── BOOKING ───────────────────────────────────────────────

    public static class BookingRequest {
        private Long   providerId, serviceId, jobId;
        private String notes, scheduledAt;
        public Long   getProviderId()   { return providerId; }  public void setProviderId(Long v)    { providerId=v; }
        public Long   getServiceId()    { return serviceId; }   public void setServiceId(Long v)     { serviceId=v; }
        public Long   getJobId()        { return jobId; }       public void setJobId(Long v)         { jobId=v; }
        public String getNotes()        { return notes; }       public void setNotes(String v)       { notes=v; }
        public String getScheduledAt()  { return scheduledAt; } public void setScheduledAt(String v) { scheduledAt=v; }
    }

    // ── PROFILE ───────────────────────────────────────────────

    public static class ProfileUpdateRequest {
        private String name, phone, location, bio, category, email;
        public String getName()     { return name; }     public void setName(String v)     { name=v; }
        public String getPhone()    { return phone; }    public void setPhone(String v)    { phone=v; }
        public String getLocation() { return location; } public void setLocation(String v) { location=v; }
        public String getBio()      { return bio; }      public void setBio(String v)      { bio=v; }
        public String getCategory() { return category; } public void setCategory(String v) { category=v; }
        public String getEmail()    { return email; }    public void setEmail(String v)    { email=v; }
    }

    // ── GENERIC ───────────────────────────────────────────────

    public static class MessageResponse {
        private final String message;
        public MessageResponse(String m) { message=m; }
        public String getMessage() { return message; }
    }

    public static class ErrorResponse {
        private final String error;
        private final int    status;
        public ErrorResponse(String e, int s) { error=e; status=s; }
        public String getError()  { return error; }
        public int    getStatus() { return status; }
    }
}
