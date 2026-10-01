package com.example.sitp.auth.captcha;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaptchaChallengeRepository extends JpaRepository<CaptchaChallenge, Long> {

    Optional<CaptchaChallenge> findByIdAndUsedFalse(Long id);
}
