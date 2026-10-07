package com.example.sitp.auth.captcha;

import com.example.sitp.auth.exception.InvalidCaptchaException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class CaptchaService {

    static final Duration CHALLENGE_LIFETIME = Duration.ofMinutes(10);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CaptchaChallengeRepository repository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public CaptchaService(CaptchaChallengeRepository repository,
                          org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public CaptchaChallenge issue() {
        int a = RANDOM.nextInt(2, 10);
        int b = RANDOM.nextInt(2, 10);
        CaptchaChallenge challenge = CaptchaChallenge.builder()
                .question(a + " + " + b + " = ?")
                .answerHash(passwordEncoder.encode(String.valueOf(a + b)))
                .expiresAt(LocalDateTime.now().plus(CHALLENGE_LIFETIME))
                .build();
        return repository.save(challenge);
    }

    public void verify(String id, String answer) {
        Long parsed;
        try {
            parsed = Long.parseLong(id == null ? "" : id.trim());
        } catch (NumberFormatException e) {
            throw new InvalidCaptchaException();
        }
        verify(parsed, answer);
    }

    public void verify(Long id, String answer) {
        CaptchaChallenge challenge = repository.findByIdAndUsedFalse(id)
                .filter(c -> c.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(InvalidCaptchaException::new);

        challenge.setUsed(true);
        repository.save(challenge);

        if (!passwordEncoder.matches(answer == null ? "" : answer.trim(), challenge.getAnswerHash())) {
            throw new InvalidCaptchaException();
        }
    }

    public Map<String, Object> toWire(CaptchaChallenge challenge) {
        return Map.of("id", challenge.getId(), "question", challenge.getQuestion());
    }
}
