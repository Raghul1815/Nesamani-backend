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
 * All endpoints for the PROVIDER role.
 * Requires: Bearer token with role = PROVIDER (ROLE_PROVIDER in Spring Security)
 *
 * GET  /api/provider/dashboard
 * POST /api/provider/services
 * GET  /api/provider/services
 * PUT  /api/provider/services/{id}
 * DEL  /api/provider/services/{id}
 * GET  /api/provider/jobs
 * POST /api/provider/jobs/{id}/respond
 * GET  /api/provider/responses
 * PUT  /api/provider/responses/{id}/withdraw
 * GET  /api/provider/bookings
 * PUT  /api/provider/bookings/{id}/accept
 * PUT  /api/provider/bookings/{id}/start
 * PUT  /api/provider/bookings/{id}/complete
 * PUT  /api/provider/profile
 *
 * PUBLIC (no token):
 * GET  /api/services?category=&location=
 * GET  /api/providers?category=&location=
 */
@RestController
@CrossOrigin(origins = "*")
public class ProviderController {

    @Autowired private JobService  jobService;
    @Autowired private UserService userService;

    // ══════════════════════════════════════════════════════════
    //  PUBLIC — no token needed
    // ══════════════════════════════════════════════════════════

    /** Browse all available services (used by needers in Browse Services page) */
    @GetMapping("/api/services")
    public ResponseEntity<?> browseServices(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location) {
        List<Service> services = jobService.getAvailableServices(category, location);
        return ResponseEntity.ok(services.stream().map(this::serviceMap).collect(Collectors.toList()));
    }

    /** Browse all providers (public directory) */
    @GetMapping("/api/providers")
    public ResponseEntity<?> browseProviders(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location) {
        List<User> providers = userService.searchProviders(category, location);
        return ResponseEntity.ok(providers.stream().map(this::providerMap).collect(Collectors.toList()));
    }

    /** Browse open jobs (public — providers use this to find work) */
    @GetMapping("/api/jobs/open")
    public ResponseEntity<?> getOpenJobs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location) {
        List<Job> jobs = jobService.getOpenJobs(category, location);
        return ResponseEntity.ok(jobs.stream().map(this::jobMap).collect(Collectors.toList()));
    }

    // ══════════════════════════════════════════════════════════
    //  PROVIDER — DASHBOARD
    // ══════════════════════════════════════════════════════════

    @GetMapping("/api/provider/dashboard")
    public ResponseEntity<?> dashboard(Authentication auth) {
        try {
            String email    = auth.getName();
            User   provider = userService.findByEmail(email);

            List<Service>     services  = jobService.getProviderServices(email);
            List<JobResponse> responses = jobService.getProviderResponses(email);
            List<Booking>     bookings  = jobService.getProviderBookings(email);
            List<Job>         openJobs  = jobService.getOpenJobs(null, null);

            long accepted   = responses.stream().filter(r -> r.getStatus() == JobResponse.ResponseStatus.ACCEPTED).count();
            long completed  = bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.COMPLETED).count();
            long activeBook = bookings.stream().filter(b ->
                b.getStatus() == Booking.BookingStatus.ACCEPTED ||
                b.getStatus() == Booking.BookingStatus.IN_PROGRESS).count();

            Map<String, Object> data = new HashMap<>();
            data.put("name",           provider.getName());
            data.put("email",          provider.getEmail());
            data.put("rating",         provider.getRating());
            data.put("jobsCompleted",  provider.getJobsCompleted());
            data.put("serviceCount",   services.size());
            data.put("responsesTotal", responses.size());
            data.put("accepted",       accepted);
            data.put("completed",      completed);
            data.put("activeBookings", activeBook);
            data.put("openJobsCount",  openJobs.size());
            data.put("services",       services.stream().limit(5).map(this::serviceMap).collect(Collectors.toList()));
            data.put("responses",      responses.stream().limit(5).map(this::responseMap).collect(Collectors.toList()));
            data.put("bookings",       bookings.stream().limit(5).map(this::bookingMap).collect(Collectors.toList()));
            data.put("notifications",  List.of(
                Map.of("id",1,"icon","📋","msg","You have " + openJobs.size() + " open jobs near you.","time","Now","read",false)
            ));

            return ResponseEntity.ok(data);
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════
    //  PROVIDER — SERVICES  (Flow B upload)
    // ══════════════════════════════════════════════════════════

    /** Get my services */
    @GetMapping("/api/provider/services")
    public ResponseEntity<?> getMyServices(Authentication auth) {
        List<Service> services = jobService.getProviderServices(auth.getName());
        return ResponseEntity.ok(services.stream().map(this::serviceMap).collect(Collectors.toList()));
    }

    /** Upload a new service */
    @PostMapping("/api/provider/services")
    public ResponseEntity<?> addService(@RequestBody Dto.ServiceRequest req, Authentication auth) {
        if (blank(req.getTitle()))    return error("Service title is required.");
        if (blank(req.getCategory())) return error("Category is required.");
        try {
            Service svc = jobService.addService(auth.getName(), req);
            return ResponseEntity.ok(serviceMap(svc));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    /** Update a service (e.g. pause/resume, change price) */
    @PutMapping("/api/provider/services/{id}")
    public ResponseEntity<?> updateService(@PathVariable Long id,
                                           @RequestBody Dto.ServiceRequest req,
                                           Authentication auth) {
        try {
            Service svc = jobService.updateService(id, auth.getName(), req);
            return ResponseEntity.ok(serviceMap(svc));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    /** Delete (soft-deactivate) a service */
    @DeleteMapping("/api/provider/services/{id}")
    public ResponseEntity<?> deleteService(@PathVariable Long id, Authentication auth) {
        try {
            jobService.deleteService(id, auth.getName());
            return ResponseEntity.ok(new Dto.MessageResponse("Service removed."));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════
    //  PROVIDER — JOBS (Flow A: provider browses and responds)
    // ══════════════════════════════════════════════════════════

    /** Browse open jobs posted by needers */
    @GetMapping("/api/provider/jobs")
    public ResponseEntity<?> browseJobs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            Authentication auth) {
        List<Job> jobs = jobService.getOpenJobs(category, location);
        return ResponseEntity.ok(jobs.stream().map(this::jobMap).collect(Collectors.toList()));
    }

    /** Respond to a job */
    @PostMapping("/api/provider/jobs/{id}/respond")
    public ResponseEntity<?> respondToJob(@PathVariable Long id,
                                          @RequestBody Dto.JobResponseRequest req,
                                          Authentication auth) {
        try {
            JobResponse resp = jobService.respondToJob(id, auth.getName(), req);
            return ResponseEntity.ok(responseMap(resp));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════
    //  PROVIDER — RESPONSES
    // ══════════════════════════════════════════════════════════

    /** Get all my responses, optionally filtered by status */
    @GetMapping("/api/provider/responses")
    public ResponseEntity<?> getMyResponses(
            @RequestParam(required = false) String status,
            Authentication auth) {
        List<JobResponse> responses = jobService.getProviderResponses(auth.getName());
        if (status != null && !status.isBlank()) {
            try {
                JobResponse.ResponseStatus s = JobResponse.ResponseStatus.valueOf(status.toUpperCase());
                responses = responses.stream().filter(r -> r.getStatus() == s).collect(Collectors.toList());
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(responses.stream().map(this::responseMap).collect(Collectors.toList()));
    }

    /** Withdraw a pending response */
    @PutMapping("/api/provider/responses/{id}/withdraw")
    public ResponseEntity<?> withdrawResponse(@PathVariable Long id, Authentication auth) {
        try {
            JobResponse resp = jobService.withdrawResponse(id, auth.getName());
            return ResponseEntity.ok(responseMap(resp));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════
    //  PROVIDER — BOOKINGS
    // ══════════════════════════════════════════════════════════

    /** Get all bookings received, optionally filtered by status */
    @GetMapping("/api/provider/bookings")
    public ResponseEntity<?> getMyBookings(
            @RequestParam(required = false) String status,
            Authentication auth) {
        List<Booking> bookings = jobService.getProviderBookings(auth.getName());
        if (status != null && !status.isBlank()) {
            try {
                Booking.BookingStatus s = Booking.BookingStatus.valueOf(status.toUpperCase());
                bookings = bookings.stream().filter(b -> b.getStatus() == s).collect(Collectors.toList());
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(bookings.stream().map(this::bookingMap).collect(Collectors.toList()));
    }

    /** Accept a booking (PENDING → ACCEPTED) */
    @PutMapping("/api/provider/bookings/{id}/accept")
    public ResponseEntity<?> acceptBooking(@PathVariable Long id, Authentication auth) {
        try {
            Booking b = jobService.updateBookingStatus(id, auth.getName(), Booking.BookingStatus.ACCEPTED);
            return ResponseEntity.ok(bookingMap(b));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    /** Start a booking (ACCEPTED → IN_PROGRESS) */
    @PutMapping("/api/provider/bookings/{id}/start")
    public ResponseEntity<?> startBooking(@PathVariable Long id, Authentication auth) {
        try {
            Booking b = jobService.updateBookingStatus(id, auth.getName(), Booking.BookingStatus.IN_PROGRESS);
            return ResponseEntity.ok(bookingMap(b));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    /** Complete a booking (IN_PROGRESS → COMPLETED) */
    @PutMapping("/api/provider/bookings/{id}/complete")
    public ResponseEntity<?> completeBooking(@PathVariable Long id, Authentication auth) {
        try {
            Booking b = jobService.updateBookingStatus(id, auth.getName(), Booking.BookingStatus.COMPLETED);
            return ResponseEntity.ok(bookingMap(b));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════
    //  PROVIDER — PROFILE
    // ══════════════════════════════════════════════════════════

    @PutMapping("/api/provider/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Dto.ProfileUpdateRequest req,
                                           Authentication auth) {
        try {
            User u = userService.updateProfile(auth.getName(), req);
            return ResponseEntity.ok(Map.of(
                "name",     u.getName(),
                "email",    u.getEmail(),
                "phone",    safe(u.getPhone()),
                "location", safe(u.getLocation()),
                "bio",      safe(u.getBio()),
                "category", safe(u.getCategory())
            ));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════
    //  SERIALISATION HELPERS
    // ══════════════════════════════════════════════════════════

    private Map<String, Object> jobMap(Job j) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",           j.getId());
        m.put("title",        j.getTitle());
        m.put("category",     safe(j.getCategory()));
        m.put("location",     safe(j.getLocation()));
        m.put("budget",       safe(j.getBudget()));
        m.put("duration",     safe(j.getDuration()));
        m.put("description",  safe(j.getDescription()));
        m.put("status",       j.getStatus().name().toLowerCase());
        m.put("date",         j.getCreatedAt() != null ? j.getCreatedAt().toLocalDate().toString() : "");
        m.put("icon",         icon(j.getCategory()));
        if (j.getNeeder() != null) {
            m.put("neederName", j.getNeeder().getName());
            m.put("neederId",   j.getNeeder().getId());
        }
        return m;
    }

    private Map<String, Object> serviceMap(Service s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          s.getId());
        m.put("title",       s.getTitle());
        m.put("category",    s.getCategory());
        m.put("price",       safe(s.getPrice()));
        m.put("priceType",   s.getPriceType() != null ? s.getPriceType().name() : "NEGOTIABLE");
        m.put("location",    safe(s.getLocation()));
        m.put("description", safe(s.getDescription()));
        m.put("isAvailable", s.getIsAvailable() != null ? s.getIsAvailable() : true);
        m.put("icon",        icon(s.getCategory()));
        if (s.getProvider() != null) {
            m.put("providerName",   s.getProvider().getName());
            m.put("providerId",     s.getProvider().getId());
            m.put("providerRating", s.getProvider().getRating() != null ? s.getProvider().getRating() : 0.0);
            m.put("providerLoc",    safe(s.getProvider().getLocation()));
        }
        return m;
    }

    private Map<String, Object> responseMap(JobResponse r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          r.getId());
        m.put("status",      r.getStatus().name().toLowerCase());
        m.put("message",     safe(r.getMessage()));
        m.put("quotedPrice", safe(r.getQuotedPrice()));
        m.put("date",        r.getRespondedAt() != null ? r.getRespondedAt().toLocalDate().toString() : "");
        if (r.getJob() != null) {
            m.put("jobId",      r.getJob().getId());
            m.put("jobTitle",   r.getJob().getTitle());
            m.put("jobBudget",  safe(r.getJob().getBudget()));
            m.put("icon",       icon(r.getJob().getCategory()));
            if (r.getJob().getNeeder() != null) {
                m.put("neederName", r.getJob().getNeeder().getName());
                m.put("neederId",   r.getJob().getNeeder().getId());
            }
        }
        if (r.getProvider() != null) {
            m.put("providerName", r.getProvider().getName());
            m.put("providerId",   r.getProvider().getId());
        }
        return m;
    }

    private Map<String, Object> bookingMap(Booking b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          b.getId());
        m.put("status",      b.getStatus().name().toLowerCase());
        m.put("agreedPrice", safe(b.getAgreedPrice()));
        m.put("notes",       safe(b.getNotes()));
        m.put("date",        b.getCreatedAt() != null ? b.getCreatedAt().toLocalDate().toString() : "");
        if (b.getNeeder() != null) {
            m.put("neederName", b.getNeeder().getName());
            m.put("neederId",   b.getNeeder().getId());
        }
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

    private Map<String, Object> providerMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",           u.getId());
        m.put("name",         u.getName());
        m.put("category",     safe(u.getCategory()));
        m.put("location",     safe(u.getLocation()));
        m.put("bio",          safe(u.getBio()));
        m.put("rating",       u.getRating() != null ? u.getRating() : 0.0);
        m.put("jobsCompleted",u.getJobsCompleted() != null ? u.getJobsCompleted() : 0);
        m.put("availability", u.getAvailability() != null ? u.getAvailability().name().toLowerCase() : "available");
        m.put("icon",         icon(u.getCategory()));
        return m;
    }

    // ── Utilities ──────────────────────────────────────────────
    private ResponseEntity<Dto.ErrorResponse> error(String msg) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Dto.ErrorResponse(msg, 400));
    }
    private boolean blank(String s) { return s == null || s.isBlank(); }
    private String  safe(String s)  { return s != null ? s : ""; }

    private String icon(String cat) {
        if (cat == null) return "🛠️";
        return switch (cat.toLowerCase()) {
            case "plumbing"                          -> "🔧";
            case "electrical"                        -> "⚡";
            case "painting"                          -> "🎨";
            case "driving"                           -> "🚗";
            case "maid / cleaning","maid","cleaning" -> "🏠";
            case "carpentry"                         -> "🪵";
            case "gardening"                         -> "🌿";
            case "cooking"                           -> "🍳";
            default                                  -> "🛠️";
        };
    }
}
