package com.example.sitp.payment.repository;

import com.example.sitp.payment.model.PaymentProof;
import com.example.sitp.payment.model.PaymentProofStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentProofRepository extends JpaRepository<PaymentProof, Long> {

    List<PaymentProof> findByStatus(PaymentProofStatus status);

    List<PaymentProof> findByUploaderId(Long uploaderId);

    boolean existsByUploaderIdAndCourseIdAndStatus(
            Long uploaderId, Long courseId, PaymentProofStatus status);
}
