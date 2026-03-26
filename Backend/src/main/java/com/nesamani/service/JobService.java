package com.nesamani.service;

import com.nesamani.dto.Dto;
import com.nesamani.model.*;
import com.nesamani.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class JobService {

    @Autowired private JobRepository         jobRepo;
    @Autowired private ServiceRepository     serviceRepo;
    @Autowired private JobResponseRepository responseRepo;
    @Autowired private BookingRepository     bookingRepo;
    @Autowired private UserRepository        userRepo;

    // ══════════════════════════════════════════════════════════
    //  JOBS  (posted by NEEDER)
    // ══════════════════════════════════════════════════════════

    public Job postJob(String neederEmail, Dto.JobRequest req) {
        User needer = getUser(neederEmail);
        requireRole(needer, User.Role.NEEDER, "Only needers can post jobs.");

        Job job = new Job();
        job.setNeeder(needer);
        job.setTitle(req.getTitle().trim());
        job.setCategory(req.getCategory());
        job.setLocation(req.getLocation().trim());
        job.setBudget(req.getBudget());
        job.setDuration(req.getDuration());
        job.setDescription(req.getDescription());
        job.setStatus(Job.JobStatus.OPEN);

        if (req.getDate() != null && !req.getDate().isBlank()) {
            try { job.setExpectedDate(LocalDate.parse(req.getDate())); } catch (Exception ignored) {}
        }
        return jobRepo.save(job);
    }

    public List<Job> getNeederJobs(String neederEmail) {
        return jobRepo.findByNeederOrderByCreatedAtDesc(getUser(neederEmail));
    }

    public Job updateJobStatus(Long jobId, String neederEmail, String status) {
        Job job = getJob(jobId);
        verifyJobOwner(job, neederEmail);
        job.setStatus(Job.JobStatus.valueOf(status.toUpperCase()));
        return jobRepo.save(job);
    }

    public void deleteJob(Long jobId, String neederEmail) {
        Job job = getJob(jobId);
        verifyJobOwner(job, neederEmail);
        jobRepo.delete(job);
    }

    public List<Job> getOpenJobs(String category, String location) {
        String cat = blank(category) ? null : category;
        String loc = blank(location) ? null : location;
        return jobRepo.searchOpenJobs(cat, loc);
    }

    // ══════════════════════════════════════════════════════════
    //  SERVICES  (uploaded by PROVIDER — Flow B)
    // ══════════════════════════════════════════════════════════

    public com.nesamani.model.Service addService(String providerEmail, Dto.ServiceRequest req) {
        User provider = getUser(providerEmail);
        requireRole(provider, User.Role.PROVIDER, "Only providers can add services.");

        com.nesamani.model.Service svc = new com.nesamani.model.Service();
        svc.setProvider(provider);
        svc.setTitle(req.getTitle().trim());
        svc.setCategory(req.getCategory());
        svc.setPrice(req.getPrice());
        svc.setLocation(req.getLocation());
        svc.setDescription(req.getDescription());
        svc.setIsAvailable(true);

        if (req.getPriceType() != null) {
            try {
                svc.setPriceType(com.nesamani.model.Service.PriceType.valueOf(req.getPriceType().toUpperCase()));
            } catch (Exception e) {
                svc.setPriceType(com.nesamani.model.Service.PriceType.NEGOTIABLE);
            }
        }
        return serviceRepo.save(svc);
    }

    public List<com.nesamani.model.Service> getProviderServices(String providerEmail) {
        return serviceRepo.findByProviderOrderByCreatedAtDesc(getUser(providerEmail));
    }

    public com.nesamani.model.Service updateService(Long serviceId, String providerEmail, Dto.ServiceRequest req) {
        com.nesamani.model.Service svc = getService(serviceId);
        verifyServiceOwner(svc, providerEmail);
        if (!blank(req.getTitle()))       svc.setTitle(req.getTitle().trim());
        if (!blank(req.getCategory()))    svc.setCategory(req.getCategory());
        if (!blank(req.getPrice()))       svc.setPrice(req.getPrice());
        if (!blank(req.getLocation()))    svc.setLocation(req.getLocation());
        if (!blank(req.getDescription())) svc.setDescription(req.getDescription());
        if (req.getIsAvailable() != null) svc.setIsAvailable(req.getIsAvailable());
        return serviceRepo.save(svc);
    }

    public void deleteService(Long serviceId, String providerEmail) {
        com.nesamani.model.Service svc = getService(serviceId);
        verifyServiceOwner(svc, providerEmail);
        svc.setIsAvailable(false);   // soft delete — just hide
        serviceRepo.save(svc);
    }

    public List<com.nesamani.model.Service> getAvailableServices(String category, String location) {
        String cat = blank(category) ? null : category;
        String loc = blank(location) ? null : location;
        return serviceRepo.searchServices(cat, loc);
    }

    // ══════════════════════════════════════════════════════════
    //  JOB RESPONSES  (Flow A — PROVIDER responds to job)
    // ══════════════════════════════════════════════════════════

    public JobResponse respondToJob(Long jobId, String providerEmail, Dto.JobResponseRequest req) {
        User provider = getUser(providerEmail);
        requireRole(provider, User.Role.PROVIDER, "Only providers can respond to jobs.");

        Job job = getJob(jobId);
        if (job.getStatus() != Job.JobStatus.OPEN)
            throw new RuntimeException("This job is no longer accepting responses.");
        if (responseRepo.existsByJobAndProvider(job, provider))
            throw new RuntimeException("You have already responded to this job.");

        JobResponse resp = new JobResponse(job, provider);
        resp.setMessage(req.getMessage());
        resp.setQuotedPrice(req.getQuotedPrice());
        return responseRepo.save(resp);
    }

    public List<JobResponse> getResponsesForJob(Long jobId, String neederEmail) {
        Job job = getJob(jobId);
        verifyJobOwner(job, neederEmail);
        return responseRepo.findByJobOrderByRespondedAtDesc(job);
    }

    public List<JobResponse> getProviderResponses(String providerEmail) {
        return responseRepo.findByProviderOrderByRespondedAtDesc(getUser(providerEmail));
    }

    /**
     * Needer accepts a response → creates a Booking, marks job IN_PROGRESS.
     */
    public Booking acceptResponse(Long responseId, String neederEmail) {
        JobResponse resp = getResponse(responseId);
        verifyJobOwner(resp.getJob(), neederEmail);

        resp.setStatus(JobResponse.ResponseStatus.ACCEPTED);
        responseRepo.save(resp);

        // Update job status
        Job job = resp.getJob();
        job.setStatus(Job.JobStatus.IN_PROGRESS);
        jobRepo.save(job);

        // Create booking (Flow A)
        Booking booking = new Booking();
        booking.setNeeder(getUser(neederEmail));
        booking.setProvider(resp.getProvider());
        booking.setJob(job);
        booking.setAgreedPrice(resp.getQuotedPrice());
        booking.setStatus(Booking.BookingStatus.ACCEPTED);
        return bookingRepo.save(booking);
    }

    public JobResponse rejectResponse(Long responseId, String neederEmail) {
        JobResponse resp = getResponse(responseId);
        verifyJobOwner(resp.getJob(), neederEmail);
        resp.setStatus(JobResponse.ResponseStatus.REJECTED);
        return responseRepo.save(resp);
    }

    public JobResponse withdrawResponse(Long responseId, String providerEmail) {
        JobResponse resp = getResponse(responseId);
        if (!resp.getProvider().getEmail().equals(providerEmail))
            throw new RuntimeException("You did not make this response.");
        resp.setStatus(JobResponse.ResponseStatus.WITHDRAWN);
        return responseRepo.save(resp);
    }

    // ══════════════════════════════════════════════════════════
    //  BOOKINGS  (Flow B — Needer books a service directly)
    // ══════════════════════════════════════════════════════════

    public Booking bookService(String neederEmail, Dto.BookingRequest req) {
        User needer = getUser(neederEmail);
        requireRole(needer, User.Role.NEEDER, "Only needers can create bookings.");

        User provider = userRepo.findById(req.getProviderId())
                .orElseThrow(() -> new RuntimeException("Provider not found."));

        Booking booking = new Booking();
        booking.setNeeder(needer);
        booking.setProvider(provider);
        booking.setNotes(req.getNotes());
        booking.setStatus(Booking.BookingStatus.PENDING);

        if (req.getServiceId() != null) {
            booking.setService(serviceRepo.findById(req.getServiceId()).orElse(null));
        }
        return bookingRepo.save(booking);
    }

    public List<Booking> getNeederBookings(String neederEmail) {
        return bookingRepo.findByNeederOrderByCreatedAtDesc(getUser(neederEmail));
    }

    public List<Booking> getProviderBookings(String providerEmail) {
        return bookingRepo.findByProviderOrderByCreatedAtDesc(getUser(providerEmail));
    }

    public Booking updateBookingStatus(Long bookingId, String userEmail, Booking.BookingStatus newStatus) {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found."));

        boolean isNeeder   = booking.getNeeder().getEmail().equals(userEmail);
        boolean isProvider = booking.getProvider().getEmail().equals(userEmail);

        if (!isNeeder && !isProvider)
            throw new RuntimeException("You are not part of this booking.");

        // Providers can accept/start/complete; needers can cancel
        booking.setStatus(newStatus);
        return bookingRepo.save(booking);
    }

    // ══════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════

    private User getUser(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));
    }

    private Job getJob(Long id) {
        return jobRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found."));
    }

    private com.nesamani.model.Service getService(Long id) {
        return serviceRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found."));
    }

    private JobResponse getResponse(Long id) {
        return responseRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Response not found."));
    }

    private void requireRole(User user, User.Role expected, String msg) {
        if (user.getRole() != expected) throw new RuntimeException(msg);
    }

    private void verifyJobOwner(Job job, String email) {
        if (!job.getNeeder().getEmail().equals(email))
            throw new RuntimeException("You do not own this job.");
    }

    private void verifyServiceOwner(com.nesamani.model.Service svc, String email) {
        if (!svc.getProvider().getEmail().equals(email))
            throw new RuntimeException("You do not own this service.");
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }
}
