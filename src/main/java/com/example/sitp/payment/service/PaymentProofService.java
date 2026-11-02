package com.example.sitp.payment.service;

import com.example.sitp.access.exception.CourseAlreadyHeldException;
import com.example.sitp.access.model.RedeemableCode;
import com.example.sitp.access.model.RedeemableCodeStatus;
import com.example.sitp.access.repository.RedeemableCodeRepository;
import com.example.sitp.access.repository.SessionRepository;
import com.example.sitp.access.service.RedeemableCodeService;
import com.example.sitp.course.exception.CourseNotFoundException;
import com.example.sitp.course.model.Audience;
import com.example.sitp.course.repository.CourseRepository;
import com.example.sitp.payment.dto.PaymentProofResponse;
import com.example.sitp.payment.dto.SubmitProofRequest;
import com.example.sitp.payment.exception.ProofAlreadyDecidedException;
import com.example.sitp.payment.exception.ProofAlreadyPendingException;
import com.example.sitp.payment.exception.ProofNotFoundException;
import com.example.sitp.payment.exception.ProofNotPurchasableException;
import com.example.sitp.payment.exception.ProofSubmitForbiddenException;
import com.example.sitp.payment.model.PaymentProof;
import com.example.sitp.payment.model.PaymentProofStatus;
import com.example.sitp.payment.repository.PaymentProofRepository;
import com.example.sitp.user.model.AccountType;
import com.example.sitp.user.model.Role;
import com.example.sitp.user.model.User;
import com.example.sitp.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentProofService {

    private final PaymentProofRepository proofRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ReceiptFileStore receiptFileStore;
    private final RedeemableCodeService redeemableCodeService;
    private final RedeemableCodeRepository codeRepository;
    private final SessionRepository sessionRepository;

    public PaymentProofService(PaymentProofRepository proofRepository,
                               CourseRepository courseRepository,
                               UserRepository userRepository,
                               ReceiptFileStore receiptFileStore,
                               RedeemableCodeService redeemableCodeService,
                               RedeemableCodeRepository codeRepository,
                               SessionRepository sessionRepository) {
        this.proofRepository = proofRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.receiptFileStore = receiptFileStore;
        this.redeemableCodeService = redeemableCodeService;
        this.codeRepository = codeRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public PaymentProofResponse submitProof(SubmitProofRequest request, Long uploaderId) {
        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated."));

        if (uploader.getRole() != Role.TRAINEE || uploader.getAccountType() != AccountType.OUTSIDER) {
            throw new ProofSubmitForbiddenException();
        }

        var course = courseRepository.findById(request.getCourseId())
                .orElseThrow(CourseNotFoundException::new);

        if (course.getTargetAudience() != Audience.OUTSIDER) {
            throw new ProofNotPurchasableException();
        }

        if (proofRepository.existsByUploaderIdAndCourseIdAndStatus(
                uploaderId, course.getId(), PaymentProofStatus.PENDING)) {
            throw new ProofAlreadyPendingException();
        }

        MultipartFile receipt = request.getReceipt();
        if (receipt == null || receipt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The receipt file is required.");
        }

        String fileUrl = receiptFileStore.save(receipt);

        PaymentProof proof = PaymentProof.builder()
                .uploader(uploader)
                .course(course)
                .fileUrl(fileUrl)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod().trim())
                .status(PaymentProofStatus.PENDING)
                .build();

        return PaymentProofResponse.fromEntity(proofRepository.save(proof));
    }

    @Transactional(readOnly = true)
    public List<PaymentProofResponse> pendingQueue() {
        return proofRepository.findByStatus(PaymentProofStatus.PENDING).stream()
                .map(PaymentProofResponse::fromEntity)
                .toList();
    }

    @Transactional
    public PaymentProofResponse approve(Long proofId) {
        PaymentProof proof = findDecidable(proofId);
        User buyer = proof.getUploader();

        if (!codeRepository.findByClaimantIdAndCourseIdAndStatus(
                buyer.getId(), proof.getCourse().getId(), RedeemableCodeStatus.ACTIVE).isEmpty()) {
            throw new CourseAlreadyHeldException();
        }

        RedeemableCode code = redeemableCodeService.generateCodes(proof.getCourse().getId(), 1).get(0);

        code.setClaimant(buyer);
        code.setRedeemedAt(LocalDateTime.now());
        codeRepository.save(code);

        proof.setLinkedCode(code);
        proof.setStatus(PaymentProofStatus.APPROVED);
        proof.setDecidedAt(LocalDateTime.now());

        return PaymentProofResponse.fromEntity(proofRepository.save(proof));
    }

    @Transactional
    public PaymentProofResponse reject(Long proofId) {
        PaymentProof proof = findDecidable(proofId);

        proof.setStatus(PaymentProofStatus.REJECTED);
        proof.setDecidedAt(LocalDateTime.now());
        PaymentProofResponse response = PaymentProofResponse.fromEntity(proofRepository.save(proof));

        sessionRepository.invalidateByUserId(proof.getUploader().getId());
        return response;
    }

    private PaymentProof findDecidable(Long proofId) {
        PaymentProof proof = proofRepository.findById(proofId)
                .orElseThrow(ProofNotFoundException::new);
        if (proof.getStatus() != PaymentProofStatus.PENDING) {
            throw new ProofAlreadyDecidedException();
        }
        return proof;
    }
}
