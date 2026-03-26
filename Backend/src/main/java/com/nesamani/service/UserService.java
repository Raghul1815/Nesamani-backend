package com.nesamani.service;

import com.nesamani.dto.Dto;
import com.nesamani.model.User;
import com.nesamani.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {

    @Autowired private UserRepository userRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    // ── Register ──────────────────────────────────────────────

    /**
     * Creates a new user.
     * Accepts role as "needer" or "provider" (lowercase from frontend).
     * Maps to User.Role.NEEDER or User.Role.PROVIDER.
     */
    public User register(Dto.RegisterRequest req) {
        if (req.getName() == null || req.getName().isBlank())
            throw new RuntimeException("Name is required.");
        if (req.getEmail() == null || req.getEmail().isBlank())
            throw new RuntimeException("Email is required.");
        if (req.getPassword() == null || req.getPassword().length() < 6)
            throw new RuntimeException("Password must be at least 6 characters.");

        String role = req.getRole() == null ? "" : req.getRole().trim().toLowerCase();
        if (!role.equals("needer") && !role.equals("provider"))
            throw new RuntimeException("Role must be 'needer' or 'provider'.");

        if (userRepo.existsByEmail(req.getEmail().trim().toLowerCase()))
            throw new RuntimeException("This email is already registered. Please log in.");

        User user = new User();
        user.setName(req.getName().trim());
        user.setEmail(req.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setPhone(req.getPhone());
        user.setRole(role.equals("needer") ? User.Role.NEEDER : User.Role.PROVIDER);
        user.setAvailability(User.Availability.AVAILABLE);
        user.setRating(0.0);
        user.setJobsCompleted(0);
        user.setIsActive(true);

        return userRepo.save(user);
    }

    // ── Authenticate ──────────────────────────────────────────

    public User authenticate(String email, String rawPassword) {
        User user = userRepo.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("No account found with that email."));

        if (!passwordEncoder.matches(rawPassword, user.getPassword()))
            throw new RuntimeException("Incorrect password. Please try again.");

        return user;
    }

    // ── Lookups ───────────────────────────────────────────────

    public User findByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));
    }

    public User findById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found."));
    }

    // ── Update profile ────────────────────────────────────────

    public User updateProfile(String email, Dto.ProfileUpdateRequest req) {
        User user = findByEmail(email);
        if (req.getName()     != null && !req.getName().isBlank()) user.setName(req.getName().trim());
        if (req.getPhone()    != null) user.setPhone(req.getPhone());
        if (req.getLocation() != null) user.setLocation(req.getLocation());
        if (req.getBio()      != null) user.setBio(req.getBio());
        if (req.getCategory() != null && user.getRole() == User.Role.PROVIDER) user.setCategory(req.getCategory());
        return userRepo.save(user);
    }

    // ── Browse providers ──────────────────────────────────────

    public List<User> getAllProviders() {
        return userRepo.findByRole(User.Role.PROVIDER);
    }

    public List<User> searchProviders(String category, String location) {
        String cat = (category != null && !category.isBlank()) ? category : null;
        String loc = (location != null && !location.isBlank()) ? location : null;
        if (cat == null && loc == null) return getAllProviders();
        return userRepo.searchProviders(cat, loc);
    }
}
