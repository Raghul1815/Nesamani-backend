package com.nesamani.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nesamani.dto.Dto;
import com.nesamani.model.Booking;
import com.nesamani.model.Job;
import com.nesamani.model.JobResponse;
import com.nesamani.model.User;
import com.nesamani.service.JobService;
import com.nesamani.service.UserService;

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
            String email  = auth.getName();
            User   needer = userService.findByEmail(email);

            List<Job>         jobs      = jobService.getNeederJobs(email);
            List<Booking>     bookings  = jobService.getNeederBookings(email);

            // Collect ALL responses for ALL of needer's jobs
            List<JobResponse> allResponses = new ArrayList<>();
            for (Job job : jobs) {
                try {
                    List<JobResponse> jobResps = jobService.getResponsesForJob(job.getId(), email);
                    allResponses.addAll(jobResps);
                } catch (Exception ignored) {}
            }

            long active    = jobs.stream().filter(j -> j.getStatus() == Job.JobStatus.OPEN || j.getStatus() == Job.JobStatus.IN_PROGRESS).count();
            long completed = jobs.stream().filter(j -> j.getStatus() == Job.JobStatus.COMPLETED).count();
            long pending   = allResponses.stream().filter(r -> r.getStatus() == JobResponse.ResponseStatus.PENDING).count();

            // Build notifications from real data
            List<Map<String, Object>> notifications = new ArrayList<>();
            int notifId = 1;
            for (JobResponse r : allResponses) {
                if (r.getStatus() == JobResponse.ResponseStatus.PENDING) {
                    Map<String, Object> n = new HashMap<>();
                    n.put("id",    notifId++);
                    n.put("icon",  "💬");
                    n.put("msg",   "<strong>" + r.getProvider().getName() + "</strong> responded to your job <strong>" + r.getJob().getTitle() + "</strong>");
                    n.put("time",  r.getRespondedAt() != null ? r.getRespondedAt().toLocalDate().toString() : "");
                    n.put("read",  false);
                    notifications.add(n);
                }
            }
            for (Booking b : bookings) {
                if (b.getStatus() == Booking.BookingStatus.COMPLETED) {
                    Map<String, Object> n = new HashMap<>();
                    n.put("id",   notifId++);
                    n.put("icon", "🎉");
                    n.put("msg",  "<strong>" + b.getProvider().getName() + "</strong> completed your booking.");
                    n.put("time", b.getUpdatedAt() != null ? b.getUpdatedAt().toLocalDate().toString() : "");
                    n.put("read", true);
                    notifications.add(n);
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("name",         needer.getName());
            data.put("email",        needer.getEmail());
            data.put("phone",        safe(needer.getPhone()));
            data.put("location",     safe(needer.getLocation()));
            data.put("totalJobs",    jobs.size());
            data.put("active",       active);
            data.put("completed",    completed);
            data.put("pendingResponses", pending);
            data.put("jobs",         jobs.stream().map(this::jobMap).collect(Collectors.toList()));
            data.put("responses",    allResponses.stream().map(this::responseMap).collect(Collectors.toList()));
            data.put("bookings",     bookings.stream().map(this::bookingMap).collect(Collectors.toList()));
            data.put("notifications",notifications);

            return ResponseEntity.ok(data);
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

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

    @PutMapping("/jobs/{id}/status")
    public ResponseEntity<?> updateJobStatus(@PathVariable Long id, @RequestParam String status, Authentication auth) {
        try {
            Job job = jobService.updateJobStatus(id, auth.getName(), status);
            return ResponseEntity.ok(jobMap(job));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable Long id, Authentication auth) {
        try {
            jobService.deleteJob(id, auth.getName());
            return ResponseEntity.ok(new Dto.MessageResponse("Job deleted."));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    @GetMapping("/jobs/{id}/responses")
    public ResponseEntity<?> getResponses(@PathVariable Long id, Authentication auth) {
        try {
            List<JobResponse> responses = jobService.getResponsesForJob(id, auth.getName());
            return ResponseEntity.ok(responses.stream().map(this::responseMap).collect(Collectors.toList()));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    @PutMapping("/responses/{id}/accept")
    public ResponseEntity<?> acceptResponse(@PathVariable Long id, Authentication auth) {
        try {
            Booking booking = jobService.acceptResponse(id, auth.getName());
            return ResponseEntity.ok(bookingMap(booking));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    @PutMapping("/responses/{id}/reject")
    public ResponseEntity<?> rejectResponse(@PathVariable Long id, Authentication auth) {
        try {
            JobResponse resp = jobService.rejectResponse(id, auth.getName());
            return ResponseEntity.ok(responseMap(resp));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    @PostMapping("/bookings")
    public ResponseEntity<?> bookService(@RequestBody Dto.BookingRequest req, Authentication auth) {
        if (req.getProviderId() == null) return error("Provider ID is required.");
        try {
            Booking booking = jobService.bookService(auth.getName(), req);
            return ResponseEntity.ok(bookingMap(booking));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

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

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Dto.ProfileUpdateRequest req, Authentication auth) {
        try {
            User u = userService.updateProfile(auth.getName(), req);
            return ResponseEntity.ok(Map.of("name", u.getName(), "email", u.getEmail(),
                "phone", safe(u.getPhone()), "location", safe(u.getLocation()), "bio", safe(u.getBio())));
        } catch (RuntimeException e) { return error(e.getMessage()); }
    }

    // ── Serialisation ──────────────────────────────────────────
    private Map<String,Object> jobMap(Job j) {
        Map<String,Object> m = new LinkedHashMap<>();
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
        m.put("neederName",  j.getNeeder() != null ? j.getNeeder().getName() : "");
        return m;
    }

    private Map<String,Object> responseMap(JobResponse r) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",           r.getId());
        m.put("status",       r.getStatus().name().toLowerCase());
        m.put("message",      safe(r.getMessage()));
        m.put("quotedPrice",  safe(r.getQuotedPrice()));
        m.put("date",         r.getRespondedAt() != null ? r.getRespondedAt().toLocalDate().toString() : "");
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
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",          b.getId());
        m.put("status",      b.getStatus().name().toLowerCase());
        m.put("agreedPrice", safe(b.getAgreedPrice()));
        m.put("notes",       safe(b.getNotes()));
        m.put("date",        b.getCreatedAt() != null ? b.getCreatedAt().toLocalDate().toString() : "");
        if (b.getProvider() != null) { m.put("providerName", b.getProvider().getName()); m.put("providerId", b.getProvider().getId()); }
        if (b.getJob()      != null) { m.put("title", b.getJob().getTitle());      m.put("icon", icon(b.getJob().getCategory())); }
        else if (b.getService() != null) { m.put("title", b.getService().getTitle()); m.put("icon", icon(b.getService().getCategory())); }
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

