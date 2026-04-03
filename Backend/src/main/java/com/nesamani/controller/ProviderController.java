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

@RestController
@CrossOrigin(origins = "*")
public class ProviderController {

    @Autowired private JobService  jobService;
    @Autowired private UserService userService;

    // ── PUBLIC ────────────────────────────────────────────────
    @GetMapping("/api/services")
    public ResponseEntity<?> browseServices(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location) {
        return ResponseEntity.ok(jobService.getAvailableServices(category, location)
                .stream().map(this::serviceMap).collect(Collectors.toList()));
    }

    @GetMapping("/api/providers")
    public ResponseEntity<?> browseProviders(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location) {
        return ResponseEntity.ok(userService.searchProviders(category, location)
                .stream().map(this::providerMap).collect(Collectors.toList()));
    }

    @GetMapping("/api/jobs/open")
    public ResponseEntity<?> getOpenJobs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location) {
        return ResponseEntity.ok(jobService.getOpenJobs(category, location)
                .stream().map(this::jobMap).collect(Collectors.toList()));
    }

    // ── PROVIDER DASHBOARD ────────────────────────────────────
    @GetMapping("/api/provider/dashboard")
    public ResponseEntity<?> dashboard(Authentication auth) {
        try {
            String email    = auth.getName();
            User   provider = userService.findByEmail(email);

            List<Service>     services  = jobService.getProviderServices(email);
            List<JobResponse> responses = jobService.getProviderResponses(email);
            List<Booking>     bookings  = jobService.getProviderBookings(email);
            List<Job>         openJobs  = jobService.getOpenJobs(null, null);  // ✅ full list

            long accepted   = responses.stream().filter(r -> r.getStatus() == JobResponse.ResponseStatus.ACCEPTED).count();
            long completed  = bookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.COMPLETED).count();
            long activeBook = bookings.stream().filter(b ->
                b.getStatus() == Booking.BookingStatus.ACCEPTED ||
                b.getStatus() == Booking.BookingStatus.IN_PROGRESS).count();

            // Build real notifications
            List<Map<String, Object>> notifications = new ArrayList<>();
            int notifId = 1;
            for (JobResponse r : responses) {
                if (r.getStatus() == JobResponse.ResponseStatus.ACCEPTED) {
                    Map<String, Object> n = new HashMap<>();
                    n.put("id",   notifId++);
                    n.put("icon", "✅");
                    n.put("msg",  "<strong>" + (r.getJob().getNeeder() != null ? r.getJob().getNeeder().getName() : "Needer") + "</strong> accepted your response for <strong>" + r.getJob().getTitle() + "</strong>");
                    n.put("time", r.getRespondedAt() != null ? r.getRespondedAt().toLocalDate().toString() : "");
                    n.put("read", false);
                    notifications.add(n);
                }
            }
            for (Booking b : bookings) {
                if (b.getStatus() == Booking.BookingStatus.PENDING) {
                    Map<String, Object> n = new HashMap<>();
                    n.put("id",   notifId++);
                    n.put("icon", "📅");
                    n.put("msg",  "<strong>" + b.getNeeder().getName() + "</strong> sent you a new booking request.");
                    n.put("time", b.getCreatedAt() != null ? b.getCreatedAt().toLocalDate().toString() : "");
                    n.put("read", false);
                    notifications.add(n);
                }
            }
            if (!openJobs.isEmpty()) {
                Map<String, Object> n = new HashMap<>();
                n.put("id",   notifId++);
                n.put("icon", "📋");
                n.put("msg",  "There are <strong>" + openJobs.size() + " open jobs</strong> available near you.");
                n.put("time", "Now");
                n.put("read", false);
                notifications.add(n);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("name",           provider.getName());
            data.put("email",          provider.getEmail());
            data.put("phone",          safe(provider.getPhone()));
            data.put("location",       safe(provider.getLocation()));
            data.put("bio",            safe(provider.getBio()));
            data.put("rating",         provider.getRating() != null ? provider.getRating() : 0.0);
            data.put("jobsCompleted",  provider.getJobsCompleted() != null ? provider.getJobsCompleted() : 0);
            data.put("serviceCount",   services.size());
            data.put("responsesTotal", responses.size());
            data.put("accepted",       accepted);
            data.put("completed",      completed);
            data.put("activeBookings", activeBook);
            data.put("services",       services.stream().map(this::serviceMap).collect(Collectors.toList()));
            data.put("responses",      responses.stream().map(this::responseMap).collect(Collectors.toList()));
            data.put("bookings",       bookings.stream().map(this::bookingMap).collect(Collectors.toList()));
            data.put("openJobs",       openJobs.stream().map(this::jobMap).collect(Collectors.toList())); // ✅ full list
            data.put("notifications",  notifications);

            return ResponseEntity.ok(data);
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ── SERVICES ──────────────────────────────────────────────
    @GetMapping("/api/provider/services")
    public ResponseEntity<?> getMyServices(Authentication auth) {
        return ResponseEntity.ok(jobService.getProviderServices(auth.getName())
                .stream().map(this::serviceMap).collect(Collectors.toList()));
    }

    @PostMapping("/api/provider/services")
    public ResponseEntity<?> addService(@RequestBody Dto.ServiceRequest req, Authentication auth) {
        if (blank(req.getTitle()))    return error("Service title is required.");
        if (blank(req.getCategory())) return error("Category is required.");
        try {
            Service svc = jobService.addService(auth.getName(), req);
            return ResponseEntity.ok(serviceMap(svc));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    @PutMapping("/api/provider/services/{id}")
    public ResponseEntity<?> updateService(@PathVariable Long id, @RequestBody Dto.ServiceRequest req, Authentication auth) {
        try { return ResponseEntity.ok(serviceMap(jobService.updateService(id, auth.getName(), req))); }
        catch (RuntimeException e) { return error(e.getMessage()); }
    }

    @DeleteMapping("/api/provider/services/{id}")
    public ResponseEntity<?> deleteService(@PathVariable Long id, Authentication auth) {
        try { jobService.deleteService(id, auth.getName()); return ResponseEntity.ok(new Dto.MessageResponse("Service removed.")); }
        catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ── JOBS ──────────────────────────────────────────────────
    @GetMapping("/api/provider/jobs")
    public ResponseEntity<?> browseJobs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            Authentication auth) {
        return ResponseEntity.ok(jobService.getOpenJobs(category, location)
                .stream().map(this::jobMap).collect(Collectors.toList()));
    }

    @PostMapping("/api/provider/jobs/{id}/respond")
    public ResponseEntity<?> respondToJob(@PathVariable Long id, @RequestBody Dto.JobResponseRequest req, Authentication auth) {
        try { return ResponseEntity.ok(responseMap(jobService.respondToJob(id, auth.getName(), req))); }
        catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ── RESPONSES ─────────────────────────────────────────────
    @GetMapping("/api/provider/responses")
    public ResponseEntity<?> getMyResponses(@RequestParam(required=false) String status, Authentication auth) {
        List<JobResponse> responses = jobService.getProviderResponses(auth.getName());
        if (status != null && !status.isBlank()) {
            try {
                JobResponse.ResponseStatus s = JobResponse.ResponseStatus.valueOf(status.toUpperCase());
                responses = responses.stream().filter(r -> r.getStatus() == s).collect(Collectors.toList());
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(responses.stream().map(this::responseMap).collect(Collectors.toList()));
    }

    @PutMapping("/api/provider/responses/{id}/withdraw")
    public ResponseEntity<?> withdrawResponse(@PathVariable Long id, Authentication auth) {
        try { return ResponseEntity.ok(responseMap(jobService.withdrawResponse(id, auth.getName()))); }
        catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ── BOOKINGS ──────────────────────────────────────────────
    @GetMapping("/api/provider/bookings")
    public ResponseEntity<?> getMyBookings(@RequestParam(required=false) String status, Authentication auth) {
        List<Booking> bookings = jobService.getProviderBookings(auth.getName());
        if (status != null && !status.isBlank()) {
            try {
                Booking.BookingStatus s = Booking.BookingStatus.valueOf(status.toUpperCase());
                bookings = bookings.stream().filter(b -> b.getStatus() == s).collect(Collectors.toList());
            } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(bookings.stream().map(this::bookingMap).collect(Collectors.toList()));
    }

    @PutMapping("/api/provider/bookings/{id}/accept")
    public ResponseEntity<?> acceptBooking(@PathVariable Long id, Authentication auth) {
        try { return ResponseEntity.ok(bookingMap(jobService.updateBookingStatus(id, auth.getName(), Booking.BookingStatus.ACCEPTED))); }
        catch (RuntimeException e) { return error(e.getMessage()); }
    }

    @PutMapping("/api/provider/bookings/{id}/start")
    public ResponseEntity<?> startBooking(@PathVariable Long id, Authentication auth) {
        try { return ResponseEntity.ok(bookingMap(jobService.updateBookingStatus(id, auth.getName(), Booking.BookingStatus.IN_PROGRESS))); }
        catch (RuntimeException e) { return error(e.getMessage()); }
    }

    @PutMapping("/api/provider/bookings/{id}/complete")
    public ResponseEntity<?> completeBooking(@PathVariable Long id, Authentication auth) {
        try { return ResponseEntity.ok(bookingMap(jobService.updateBookingStatus(id, auth.getName(), Booking.BookingStatus.COMPLETED))); }
        catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ── PROFILE ───────────────────────────────────────────────
    @PutMapping("/api/provider/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Dto.ProfileUpdateRequest req, Authentication auth) {
        try {
            User u = userService.updateProfile(auth.getName(), req);
            return ResponseEntity.ok(Map.of("name", u.getName(), "email", u.getEmail(),
                "phone", safe(u.getPhone()), "location", safe(u.getLocation()),
                "bio", safe(u.getBio()), "category", safe(u.getCategory())));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ── Serialisation ──────────────────────────────────────────
    private Map<String, Object> jobMap(Job j) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          j.getId());
        m.put("title",       j.getTitle());
        m.put("category",    safe(j.getCategory()));
        m.put("location",    safe(j.getLocation()));
        m.put("budget",      safe(j.getBudget()));
        m.put("duration",    safe(j.getDuration()));
        m.put("description", safe(j.getDescription()));
        m.put("status",      j.getStatus().name().toLowerCase());
        m.put("date",        j.getCreatedAt() != null ? j.getCreatedAt().toLocalDate().toString() : "");
        m.put("icon",        icon(j.getCategory()));
        if (j.getNeeder() != null) { m.put("neederName", j.getNeeder().getName()); m.put("neederId", j.getNeeder().getId()); }
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
            if (r.getJob().getNeeder() != null) { m.put("neederName", r.getJob().getNeeder().getName()); }
        }
        if (r.getProvider() != null) { m.put("providerName", r.getProvider().getName()); m.put("providerId", r.getProvider().getId()); }
        return m;
    }

    private Map<String, Object> bookingMap(Booking b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          b.getId());
        m.put("status",      b.getStatus().name().toLowerCase());
        m.put("agreedPrice", safe(b.getAgreedPrice()));
        m.put("notes",       safe(b.getNotes()));
        m.put("date",        b.getCreatedAt() != null ? b.getCreatedAt().toLocalDate().toString() : "");
        if (b.getNeeder()   != null) { m.put("neederName",   b.getNeeder().getName());   m.put("neederId",   b.getNeeder().getId()); }
        if (b.getProvider() != null) { m.put("providerName", b.getProvider().getName()); m.put("providerId", b.getProvider().getId()); }
        if (b.getJob()      != null) { m.put("title", b.getJob().getTitle());      m.put("icon", icon(b.getJob().getCategory())); }
        else if (b.getService() != null) { m.put("title", b.getService().getTitle()); m.put("icon", icon(b.getService().getCategory())); }
        return m;
    }

    private Map<String, Object> providerMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",           u.getId());
        m.put("name",         u.getName());
        m.put("category",     safe(u.getCategory()));
        m.put("location",     safe(u.getLocation()));
        m.put("rating",       u.getRating() != null ? u.getRating() : 0.0);
        m.put("jobsCompleted",u.getJobsCompleted() != null ? u.getJobsCompleted() : 0);
        m.put("availability", u.getAvailability() != null ? u.getAvailability().name().toLowerCase() : "available");
        m.put("icon",         icon(u.getCategory()));
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
