package com.yourapp.service;

import com.yourapp.dto.AuthResponse;
import com.yourapp.dto.ForgotPasswordRequest;
import com.yourapp.dto.LoginRequest;
import com.yourapp.dto.RegisterRequest;
import com.yourapp.dto.ResetPasswordRequest;
import com.yourapp.dto.VerifyOtpRequest;
import com.yourapp.model.User;
import com.yourapp.repository.UserRepository;
import com.yourapp.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .build();

        User saved = userRepository.save(user);

        // Generate token — if this fails, clean up the saved user so re-registration works
        String token;
        try {
            token = jwtUtil.generateToken(saved);
        } catch (Exception e) {
            log.error("REGISTER_TOKEN_FAIL userId={} email={} error={}",
                    saved.getId(), saved.getEmail(), e.getMessage());
            userRepository.deleteById(saved.getId());
            throw new RuntimeException("Account creation failed due to an internal error. Please try again.");
        }

        log.info("REGISTER_SUCCESS userId={} email={}", saved.getId(), saved.getEmail());

        return AuthResponse.builder()
                .token(token)
                .userId(saved.getId())
                .fullName(saved.getFullName())
                .email(saved.getEmail())
                .message("Registration successful")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            log.warn("LOGIN_FAIL email={} reason=bad_credentials", request.getEmail());
            throw e;
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user);

        log.info("LOGIN_SUCCESS userId={} email={}", user.getId(), user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .message("Login successful")
                .build();
    }

    // ------------------------------------------------------------------
    // Forgot Password — Step 1: send OTP
    // ------------------------------------------------------------------
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("No account found with that email"));

        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        user.setPasswordResetOtp(passwordEncoder.encode(otp)); // store hashed
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        String body = "Your SMART TODO password reset OTP is: " + otp +
                "\n\nThis code expires in 10 minutes." +
                "\nIf you did not request this, please ignore this email.";
        emailService.sendEmail(user.getEmail(), "SMART TODO — Password Reset OTP", body);
        log.info("OTP_SENT email={}", user.getEmail());
    }

    // ------------------------------------------------------------------
    // Forgot Password — Step 2: verify OTP
    // ------------------------------------------------------------------
    public void verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("No account found with that email"));

        if (user.getPasswordResetOtp() == null || user.getOtpExpiry() == null) {
            throw new IllegalArgumentException("No OTP requested. Please request a new one.");
        }
        if (LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }
        if (!passwordEncoder.matches(request.getOtp(), user.getPasswordResetOtp())) {
            throw new IllegalArgumentException("Invalid OTP. Please try again.");
        }
        // OTP is valid — leave it in place until the password is actually reset
        log.info("OTP_VERIFIED email={}", user.getEmail());
    }

    // ------------------------------------------------------------------
    // Forgot Password — Step 3: reset password
    // ------------------------------------------------------------------
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("No account found with that email"));

        if (user.getPasswordResetOtp() == null || user.getOtpExpiry() == null) {
            throw new IllegalArgumentException("No OTP requested. Please request a new one.");
        }
        if (LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }
        if (!passwordEncoder.matches(request.getOtp(), user.getPasswordResetOtp())) {
            throw new IllegalArgumentException("Invalid OTP.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);
        log.info("PASSWORD_RESET_SUCCESS email={}", user.getEmail());
    }
}