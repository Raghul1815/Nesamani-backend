package com.nesamani.controller;

import com.nesamani.dto.Dto;
import com.nesamani.model.*;
import com.nesamani.service.JobService;
import com.nesamani.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * All endpoints for the NEEDER role.
 * Requires: Bearer token with role = NEEDER  (ROLE_NEEDER in Spring Security)
 *
 * GET  /api/needer/dashboard
 * POST /api/needer/jobs
 * GET  /api/needer/jobs
 * PUT  /api/needer/jobs/{id}/status
 * DEL  /api/needer/jobs/{id}
 * GET  /api/needer/jobs/{id}/responses
 * PUT  /api/needer/responses/{id}/accept   → creates booking
 * PUT  /api/needer/responses/{id}/reject
 * POST /api/needer/bookings                → Flow B: book a service directly
 * GET  /api/needer/bookings
 * PUT  /api/needer/profile
 */
@RestController
@RequestMapping("/api/needer")
@CrossOrigin(origins = "*")
public class NeederController {

    @Autowired private JobService  jobService;
    @Autowired private UserService userService;

    // ── Dashboard ─────────────────────────────────────────────
    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(Authentication auth) {
        try {
            String email = auth.getName();
            User   needer = userService.findByEmail(email);

            List<Job>      jobs      = jobService.getNeederJobs(email);
            List<Booking>  bookings  = jobService.getNeederBookings(email);

            long active    = jobs.stream().filter(j -> j.getStatus() == Job.JobStatus.OPEN || j.getStatus() == Job.JobStatus.IN_PROGRESS).count();
            long completed = jobs.stream().filter(j -> j.getStatus() == Job.JobStatus.COMPLETED).count();
            long pending   = bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.PENDING).count();

            Map<String, Object> data = new HashMap<>();
            data.put("name",       needer.getName());
            data.put("email",      needer.getEmail());
            data.put("totalJobs",  jobs.size());
            data.put("active",     active);
            data.put("completed",  completed);
            data.put("pendingBookings", pending);
            data.put("jobs",       jobs.stream().limit(5).map(this::jobMap).collect(Collectors.toList()));
            data.put("bookings",   bookings.stream().limit(5).map(this::bookingMap).collect(Collectors.toList()));

            return ResponseEntity.ok(data);
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ── Post a job ────────────────────────────────────────────
    @PostMapping("/jobs")
    public ResponseEntity<?> postJob(@RequestBody Dto.JobRequest req, Authentication auth) {
        if (blank(req.getTitle()))    return error("Job title is required.");
        if (blank(req.getCategory())) return error("Category is required.");
        if (blank(req.getLocation())) return error("Location is required.");
        try {
            Job job = jobService.postJob(auth.getName(), req);
            return ResponseEntity.ok(jobMap(job));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ── My jobs ───────────────────────────────────────────────
    @GetMapping("/jobs")
    public ResponseEntity<?> getMyJobs(@RequestParam(required=false) String status, Authentication auth) {
        List<Job> jobs = jobService.getNeederJobs(auth.getName());
        if (status != null && !status.isBlank()) {
            try {
                Job.JobStatus s = Job.JobStatus.valueOf(status.toUpperCase());
                jobs = jobs.stream().filter(j -> j.getStatus() == s).collect(Collectors.toList());
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(jobs.stream().map(this::jobMap).collect(Collectors.toList()));
    }

    // ── Update job status ─────────────────────────────────────
    @PutMapping("/jobs/{id}/status")
    public ResponseEntity<?> updateJobStatus(@PathVariable Long id, @RequestParam String status, Authentication auth) {
        try {
            Job job = jobService.updateJobStatus(id, auth.getName(), status);
            return ResponseEntity.ok(jobMap(job));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ── Delete job ────────────────────────────────────────────
    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id, Authentication auth) {
        try {
            jobService.deleteJob(id, auth.getName());
            return ResponseEntity.ok(new Dto.MessageResponse("Job deleted."));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ── Get responses for my job ──────────────────────────────
    @GetMapping("/jobs/{id}/responses")
    public ResponseEntity<?> getResponses(@PathVariable Long id, Authentication auth) {
        try {
            List<JobResponse> responses = jobService.getResponsesForJob(id, auth.getName());
            return ResponseEntity.ok(responses.stream().map(this::responseMap).collect(Collectors.toList()));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ── Accept a response (creates booking) ───────────────────
    @PutMapping("/responses/{id}/accept")
    public ResponseEntity<?> acceptResponse(@PathVariable Long id, Authentication auth) {
        try {
            Booking booking = jobService.acceptResponse(id, auth.getName());
            return ResponseEntity.ok(bookingMap(booking));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ── Reject a response ─────────────────────────────────────
    @PutMapping("/responses/{id}/reject")
    public ResponseEntity<?> rejectResponse(@PathVariable Long id, Authentication auth) {
        try {
            JobResponse resp = jobService.rejectResponse(id, auth.getName());
            return ResponseEntity.ok(responseMap(resp));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ── Book a service directly (Flow B) ──────────────────────
    @PostMapping("/bookings")
    public ResponseEntity<?> bookService(@RequestBody Dto.BookingRequest req, Authentication auth) {
        if (req.getProviderId() == null) return error("Provider ID is required.");
        try {
            Booking booking = jobService.bookService(auth.getName(), req);
            return ResponseEntity.ok(bookingMap(booking));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ── My bookings ───────────────────────────────────────────
    @GetMapping("/bookings")
    public ResponseEntity<?> getMyBookings(@RequestParam(required=false) String status, Authentication auth) {
        List<Booking> bookings = jobService.getNeederBookings(auth.getName());
        if (status != null && !status.isBlank()) {
            try {
                Booking.BookingStatus s = Booking.BookingStatus.valueOf(status.toUpperCase());
                bookings = bookings.stream().filter(b -> b.getStatus() == s).collect(Collectors.toList());
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(bookings.stream().map(this::bookingMap).collect(Collectors.toList()));
    }

    // ── Update profile ────────────────────────────────────────
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Dto.ProfileUpdateRequest req, Authentication auth) {
        try {
            User u = userService.updateProfile(auth.getName(), req);
            return ResponseEntity.ok(Map.of("name", u.getName(), "email", u.getEmail(),
                "phone", safe(u.getPhone()), "location", safe(u.getLocation()), "bio", safe(u.getBio())));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ── Serialisation helpers ──────────────────────────────────
    private Map<String,Object> jobMap(Job j) {
        Map<String,Object> m = new HashMap<>();
        m.put("id",       j.getId());
        m.put("title",    j.getTitle());
        m.put("category", safe(j.getCategory()));
        m.put("location", safe(j.getLocation()));
        m.put("budget",   safe(j.getBudget()));
        m.put("duration", safe(j.getDuration()));
        m.put("status",   j.getStatus().name().toLowerCase());
        m.put("date",     j.getCreatedAt() != null ? j.getCreatedAt().toLocalDate().toString() : "");
        m.put("icon",     icon(j.getCategory()));
        m.put("neederName", j.getNeeder() != null ? j.getNeeder().getName() : "");
        return m;
    }

    private Map<String,Object> responseMap(JobResponse r) {
        Map<String,Object> m = new HashMap<>();
        m.put("id",           r.getId());
        m.put("status",       r.getStatus().name().toLowerCase());
        m.put("message",      safe(r.getMessage()));
        m.put("quotedPrice",  safe(r.getQuotedPrice()));
        m.put("respondedAt",  r.getRespondedAt() != null ? r.getRespondedAt().toLocalDate().toString() : "");
        if (r.getJob() != null) {
            m.put("jobId",    r.getJob().getId());
            m.put("jobTitle", r.getJob().getTitle());
            m.put("icon",     icon(r.getJob().getCategory()));
        }
        if (r.getProvider() != null) {
            m.put("providerName", r.getProvider().getName());
            m.put("providerId",   r.getProvider().getId());
            m.put("providerIcon", icon(r.getProvider().getCategory()));
        }
        return m;
    }

    private Map<String,Object> bookingMap(Booking b) {
        Map<String,Object> m = new HashMap<>();
        m.put("id",           b.getId());
        m.put("status",       b.getStatus().name().toLowerCase());
        m.put("agreedPrice",  safe(b.getAgreedPrice()));
        m.put("notes",        safe(b.getNotes()));
        m.put("date",         b.getCreatedAt() != null ? b.getCreatedAt().toLocalDate().toString() : "");
        if (b.getProvider() != null) {
            m.put("providerName", b.getProvider().getName());
            m.put("providerId",   b.getProvider().getId());
        }
        if (b.getJob() != null) {
            m.put("title", b.getJob().getTitle());
            m.put("icon",  icon(b.getJob().getCategory()));
        } else if (b.getService() != null) {
            m.put("title", b.getService().getTitle());
            m.put("icon",  icon(b.getService().getCategory()));
        }
        return m;
    }

    private ResponseEntity<Dto.ErrorResponse> error(String msg) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Dto.ErrorResponse(msg, 400));
    }
    private boolean blank(String s) { return s == null || s.isBlank(); }
    private String  safe(String s)  { return s != null ? s : ""; }
    private String  icon(String cat) {
        if (cat == null) return "🛠️";
        return switch (cat.toLowerCase()) {
            case "plumbing"              -> "🔧";
            case "electrical"            -> "⚡";
            case "painting"              -> "🎨";
            case "driving"               -> "🚗";
            case "maid / cleaning","maid","cleaning" -> "🏠";
            case "carpentry"             -> "🪵";
            case "gardening"             -> "🌿";
            case "cooking"               -> "🍳";
            default                      -> "🛠️";
        };
    }
}
