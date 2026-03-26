package com.nesamani.controller;

import com.nesamani.dto.Dto;
import com.nesamani.model.User;
import com.nesamani.security.JwtUtil;
import com.nesamani.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * POST /api/auth/register
 * POST /api/auth/login  →  { token, role:"needer"|"provider", name, userId, email, phone }
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private UserService userService;
    @Autowired private JwtUtil     jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Dto.RegisterRequest req) {
        try {
            User user = userService.register(req);
            return ResponseEntity.ok(new Dto.MessageResponse(
                "Account created! Welcome to Nesamani, " + user.getName() + "."));
        } catch (RuntimeException e) {
            return error(e.getMessage(), HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Dto.LoginRequest req) {
        if (req.getEmail() == null || req.getPassword() == null)
            return error("Email and password are required.", HttpStatus.BAD_REQUEST);
        try {
            User user = userService.authenticate(req.getEmail(), req.getPassword());
            // MUST be lowercase: "needer" or "provider" — auth.js redirectByRole() depends on this
            String role  = user.getRole().name().toLowerCase();
            String token = jwtUtil.generateToken(user.getEmail(), role);
            return ResponseEntity.ok(new Dto.AuthResponse(
                token, role, user.getName(), user.getId(),
                user.getEmail(), user.getPhone() != null ? user.getPhone() : ""));
        } catch (RuntimeException e) {
            return error(e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    private ResponseEntity<Dto.ErrorResponse> error(String msg, HttpStatus s) {
        return ResponseEntity.status(s).body(new Dto.ErrorResponse(msg, s.value()));
    }
}
