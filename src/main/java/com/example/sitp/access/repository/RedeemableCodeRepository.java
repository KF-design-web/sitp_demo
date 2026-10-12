package com.example.sitp.access.repository;

import com.example.sitp.access.model.RedeemableCode;
import com.example.sitp.access.model.RedeemableCodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RedeemableCodeRepository extends JpaRepository<RedeemableCode, Long> {

    Optional<RedeemableCode> findByCode(String code);

    Optional<RedeemableCode> findByClaimantIdAndCourseIdAndStatus(
            Long claimantId, Long courseId, RedeemableCodeStatus status);

    List<RedeemableCode> findByCourseId(Long courseId);

    List<RedeemableCode> findByClaimantId(Long claimantId);
}
