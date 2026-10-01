package com.example.sitp.verification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findFirstByUsernameIgnoreCaseAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            String username, VerificationPurpose purpose);
}
