package com.example.sitp.verification;

import com.example.sitp.auth.exception.PasswordMismatchException;
import com.example.sitp.user.model.User;
import com.example.sitp.user.repository.UserRepository;
import com.example.sitp.verification.exception.VerificationCodeInvalidException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class VerificationService {

    static final Duration CODE_LIFETIME = Duration.ofMinutes(15);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final VerificationCodeRepository codeRepository;
    private final UserRepository userRepository;
    private final SessionRepositoryPort sessionPort;
    private final EmailSender emailSender;
    private final PasswordEncoder passwordEncoder;
    private final String fromLabel;

    public VerificationService(VerificationCodeRepository codeRepository,
                               UserRepository userRepository,
                               SessionRepositoryPort sessionPort,
                               EmailSender emailSender,
                               PasswordEncoder passwordEncoder,
                               @Value("${app.mail.from}") String fromLabel) {
        this.codeRepository = codeRepository;
        this.userRepository = userRepository;
        this.sessionPort = sessionPort;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
        this.fromLabel = fromLabel;
    }


    @Transactional
    public void issueEmailVerification(User user) {
        String code = issue(user.getUsername(), VerificationPurpose.VERIFY_EMAIL);
        emailSender.send(user.getEmail(),
                "Your SITP verification code",
                "Hello " + user.getFirstName() + ",\n\n"
                        + "Your SITP verification code is: " + code + "\n"
                        + "It expires in 15 minutes.\n\n"
                        + fromLabel);
    }

    @Transactional
    public void verifyEmail(String username, String code) {
        VerificationCode row = consume(username, VerificationPurpose.VERIFY_EMAIL, code);

        User user = userRepository.findByUsername(username)
                .orElseThrow(VerificationCodeInvalidException::new);

        user.setEnabled(true);
        userRepository.save(user);
        row.setUsed(true);
        codeRepository.save(row);
    }


    @Transactional
    public void issuePasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String code = issue(user.getUsername(), VerificationPurpose.RESET_PASSWORD);
            emailSender.send(user.getEmail(),
                    "Your SITP password-reset code",
                    "Hello " + user.getFirstName() + ",\n\n"
                            + "Your SITP password-reset code is: " + code + "\n"
                            + "It expires in 15 minutes.\n\n"
                            + fromLabel);
        });
    }

    @Transactional
    public void resetPassword(String username, String code, String newPassword, String newPasswordConfirm) {
        if (!newPassword.equals(newPasswordConfirm)) {
            throw new PasswordMismatchException();
        }

        VerificationCode row = consume(username, VerificationPurpose.RESET_PASSWORD, code);

        User user = userRepository.findByUsername(username)
                .orElseThrow(VerificationCodeInvalidException::new);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.saveAndFlush(user);
        row.setUsed(true);
        codeRepository.save(row);

        sessionPort.invalidateByUserId(user.getId());
    }


    private String issue(String username, VerificationPurpose purpose) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        codeRepository.save(VerificationCode.builder()
                .username(username.toLowerCase())
                .purpose(purpose)
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(LocalDateTime.now().plus(CODE_LIFETIME))
                .build());
        return code;
    }

    private VerificationCode consume(String username, VerificationPurpose purpose, String code) {
        VerificationCode row = codeRepository
                .findFirstByUsernameIgnoreCaseAndPurposeAndUsedFalseOrderByCreatedAtDesc(username, purpose)
                .filter(r -> r.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(VerificationCodeInvalidException::new);

        if (!passwordEncoder.matches(code, row.getCodeHash())) {
            throw new VerificationCodeInvalidException();
        }
        return row;
    }
}
