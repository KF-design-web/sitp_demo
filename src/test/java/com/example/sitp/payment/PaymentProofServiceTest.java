package com.example.sitp.payment;

import com.example.sitp.access.exception.CourseAlreadyHeldException;
import com.example.sitp.access.model.RedeemableCode;
import com.example.sitp.access.model.RedeemableCodeStatus;
import com.example.sitp.access.model.Session;
import com.example.sitp.access.repository.RedeemableCodeRepository;
import com.example.sitp.access.repository.SessionRepository;
import com.example.sitp.course.exception.CourseNotFoundException;
import com.example.sitp.course.model.Audience;
import com.example.sitp.course.model.Course;
import com.example.sitp.course.repository.CourseRepository;
import com.example.sitp.payment.dto.SubmitProofRequest;
import com.example.sitp.payment.exception.ProofAlreadyDecidedException;
import com.example.sitp.payment.exception.ProofAlreadyPendingException;
import com.example.sitp.payment.exception.ProofNotFoundException;
import com.example.sitp.payment.exception.ProofNotPurchasableException;
import com.example.sitp.payment.exception.ProofSubmitForbiddenException;
import com.example.sitp.payment.model.PaymentProof;
import com.example.sitp.payment.model.PaymentProofStatus;
import com.example.sitp.payment.repository.PaymentProofRepository;
import com.example.sitp.payment.service.PaymentProofService;
import com.example.sitp.user.model.AccountType;
import com.example.sitp.user.model.Role;
import com.example.sitp.user.model.User;
import com.example.sitp.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PaymentProofServiceTest {

    @Autowired
    private PaymentProofService paymentProofService;

    @Autowired
    private PaymentProofRepository paymentProofRepository;

    @Autowired
    private RedeemableCodeRepository redeemableCodeRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    private void cleanTables() {
        paymentProofRepository.deleteAll();
        redeemableCodeRepository.deleteAll();
        sessionRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User seedUser(String username, Role role, AccountType accountType) {
        return userRepository.save(User.builder()
                .email(username + "@mail.com")
                .username(username)
                .passwordHash("irrelevant-for-this-test")
                .role(role)
                .accountType(accountType)
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .build());
    }

    private Course courseFor(Audience audience, String price) {
        return courseRepository.save(Course.builder()
                .title("Paid access course")
                .description("A course somebody wants to buy")
                .targetAudience(audience)
                .price(price == null ? null : new BigDecimal(price))
                .build());
    }

    private SubmitProofRequest proofRequest(Long courseId) {
        SubmitProofRequest request = new SubmitProofRequest();
        request.setCourseId(courseId);
        request.setAmount(new BigDecimal("49.90"));
        request.setPaymentMethod("MTN Mobile Money");
        request.setReceipt(new MockMultipartFile(
                "receipt", "receipt.png", "image/png", "receipt-bytes".getBytes()));
        return request;
    }

    private PaymentProof pendingProof(User buyer, Course course) {
        return paymentProofRepository.save(PaymentProof.builder()
                .uploader(buyer)
                .course(course)
                .fileUrl("receipts/seeded.png")
                .amount(new BigDecimal("49.90"))
                .paymentMethod("MTN Mobile Money")
                .status(PaymentProofStatus.PENDING)
                .build());
    }

    @Test
    void submitProof_outsiderForAnOutsiderCourse_createsThePendingFilingCard() {
        cleanTables();
        User buyer = seedUser("buyer.one", Role.TRAINEE, AccountType.OUTSIDER);
        Course course = courseFor(Audience.OUTSIDER, "49.90");

        var card = paymentProofService.submitProof(proofRequest(course.getId()), buyer.getId());

        assertThat(card.getStatus()).isEqualTo(PaymentProofStatus.PENDING);
        assertThat(card.getUploaderId()).isEqualTo(buyer.getId());
        assertThat(card.getUploaderUsername()).isEqualTo("buyer.one");
        assertThat(card.getCourseId()).isEqualTo(course.getId());
        assertThat(card.getAmount()).isEqualByComparingTo("49.90");
        assertThat(card.getPaymentMethod()).isEqualTo("MTN Mobile Money");
        assertThat(card.getFileUrl()).startsWith("receipts/");
        PaymentProof row = paymentProofRepository.findById(card.getId()).orElseThrow();
        assertThat(row.getStatus()).isEqualTo(PaymentProofStatus.PENDING);
        assertThat(row.getDecidedAt()).isNull();
    }

    @Test
    void submitProof_internCannotSubmitTheProof() {
        cleanTables();
        User intern = seedUser("intern.one", Role.TRAINEE, AccountType.INTERN);
        Course course = courseFor(Audience.OUTSIDER, "49.90");

        assertThatThrownBy(() -> paymentProofService.submitProof(proofRequest(course.getId()), intern.getId()))
                .isInstanceOf(ProofSubmitForbiddenException.class);
        assertThat(paymentProofRepository.findAll()).isEmpty();
    }

    @Test
    void submitProof_unknownCourse_throwsTheThrown404() {
        cleanTables();
        User buyer = seedUser("buyer.one", Role.TRAINEE, AccountType.OUTSIDER);

        assertThatThrownBy(() -> paymentProofService.submitProof(proofRequest(9999L), buyer.getId()))
                .isInstanceOf(CourseNotFoundException.class);
    }

    @Test
    void submitProof_internCourse_refusesTheSale() {
        cleanTables();
        User buyer = seedUser("buyer.one", Role.TRAINEE, AccountType.OUTSIDER);
        Course course = courseFor(Audience.INTERN, null);

        assertThatThrownBy(() -> paymentProofService.submitProof(proofRequest(course.getId()), buyer.getId()))
                .isInstanceOf(ProofNotPurchasableException.class);
        assertThat(paymentProofRepository.findAll()).isEmpty();
    }

    @Test
    void submitProof_secondProofForTheSameCourse_refusesTheDuplicate() {
        cleanTables();
        User buyer = seedUser("buyer.one", Role.TRAINEE, AccountType.OUTSIDER);
        Course course = courseFor(Audience.OUTSIDER, "49.90");
        paymentProofService.submitProof(proofRequest(course.getId()), buyer.getId());

        assertThatThrownBy(() -> paymentProofService.submitProof(proofRequest(course.getId()), buyer.getId()))
                .isInstanceOf(ProofAlreadyPendingException.class);
        assertThat(paymentProofRepository.findByUploaderId(buyer.getId())).hasSize(1);
    }

    @Test
    void pendingQueue_returnsOnlyThePendingRows() {
        cleanTables();
        User buyer = seedUser("buyer.one", Role.TRAINEE, AccountType.OUTSIDER);
        Course course = courseFor(Audience.OUTSIDER, "49.90");
        pendingProof(buyer, course);
        PaymentProof decided = pendingProof(buyer, course);
        decided.setStatus(PaymentProofStatus.APPROVED);
        paymentProofRepository.save(decided);

        var queue = paymentProofService.pendingQueue();

        assertThat(queue).hasSize(1);
        assertThat(queue.get(0).getStatus()).isEqualTo(PaymentProofStatus.PENDING);
    }

    @Test
    void approve_mintsClaimsAndLinksTheCodeInOneMove_TheLifetimeVerdict() {
        cleanTables();
        User buyer = seedUser("buyer.one", Role.TRAINEE, AccountType.OUTSIDER);
        Course course = courseFor(Audience.OUTSIDER, "49.90");
        PaymentProof proof = pendingProof(buyer, course);

        var card = paymentProofService.approve(proof.getId());

        assertThat(card.getStatus()).isEqualTo(PaymentProofStatus.APPROVED);
        assertThat(card.getDecidedAt()).isNotNull();
        PaymentProof row = paymentProofRepository.findById(proof.getId()).orElseThrow();
        assertThat(row.getStatus()).isEqualTo(PaymentProofStatus.APPROVED);
        assertThat(row.getDecidedAt()).isNotNull();
        var claimed = redeemableCodeRepository.findByClaimantIdAndCourseIdAndStatus(
                buyer.getId(), course.getId(), RedeemableCodeStatus.ACTIVE);
        assertThat(claimed).hasSize(1);
        RedeemableCode minted = claimed.get(0);
        assertThat(minted.getValidityDays()).isEqualTo(null);
        assertThat(minted.getRedeemedAt()).isNotNull();
        assertThat(row.getLinkedCode().getId()).isEqualTo(minted.getId());
    }

    @Test
    void approve_whenTheBuyerAlreadyHoldsTheCourse_refusesAndLeavesThePendingRowUntouched() {
        cleanTables();
        User buyer = seedUser("buyer.one", Role.TRAINEE, AccountType.OUTSIDER);
        Course course = courseFor(Audience.OUTSIDER, "49.90");
        redeemableCodeRepository.save(RedeemableCode.builder()
                .code("GIT-HELD0001")
                .course(course)
                .claimant(buyer)
                .status(RedeemableCodeStatus.ACTIVE)
                .build());
        PaymentProof proof = pendingProof(buyer, course);

        assertThatThrownBy(() -> paymentProofService.approve(proof.getId()))
                .isInstanceOf(CourseAlreadyHeldException.class);
        assertThat(paymentProofRepository.findById(proof.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentProofStatus.PENDING);
        assertThat(redeemableCodeRepository.findByClaimantIdAndCourseIdAndStatus(
                buyer.getId(), course.getId(), RedeemableCodeStatus.ACTIVE)).hasSize(1);
    }

    @Test
    void approve_unknownProof_throwsTheThrown404() {
        cleanTables();

        assertThatThrownBy(() -> paymentProofService.approve(9999L))
                .isInstanceOf(ProofNotFoundException.class);
    }

    @Test
    void approve_alreadyDecided_throwsTheAlreadyDecidedVerdict() {
        cleanTables();
        User buyer = seedUser("buyer.one", Role.TRAINEE, AccountType.OUTSIDER);
        Course course = courseFor(Audience.OUTSIDER, "49.90");
        PaymentProof proof = pendingProof(buyer, course);
        paymentProofService.approve(proof.getId());

        assertThatThrownBy(() -> paymentProofService.approve(proof.getId()))
                .isInstanceOf(ProofAlreadyDecidedException.class);
        assertThat(redeemableCodeRepository.findByClaimantIdAndCourseIdAndStatus(
                buyer.getId(), course.getId(), RedeemableCodeStatus.ACTIVE)).hasSize(1);
    }

    @Test
    void reject_flipsTheRowAndKillsTheUploaderSessions() {
        cleanTables();
        User buyer = seedUser("buyer.one", Role.TRAINEE, AccountType.OUTSIDER);
        Course course = courseFor(Audience.OUTSIDER, "49.90");
        sessionRepository.save(Session.builder()
                .user(buyer)
                .token("tok-buyer-1")
                .active(true)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build());
        sessionRepository.save(Session.builder()
                .user(buyer)
                .token("tok-buyer-2")
                .active(true)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build());
        PaymentProof proof = pendingProof(buyer, course);

        var card = paymentProofService.reject(proof.getId());

        assertThat(card.getStatus()).isEqualTo(PaymentProofStatus.REJECTED);
        assertThat(card.getDecidedAt()).isNotNull();
        sessionRepository.findAll().stream()
                .filter(s -> s.getUser().getId().equals(buyer.getId()))
                .forEach(s -> assertThat(s.isActive()).isFalse());
    }

    @Test
    void reject_twice_throwsTheAlreadyDecidedVerdict() {
        cleanTables();
        User buyer = seedUser("buyer.one", Role.TRAINEE, AccountType.OUTSIDER);
        Course course = courseFor(Audience.OUTSIDER, "49.90");
        PaymentProof proof = pendingProof(buyer, course);
        paymentProofService.reject(proof.getId());

        assertThatThrownBy(() -> paymentProofService.reject(proof.getId()))
                .isInstanceOf(ProofAlreadyDecidedException.class);
    }

    @Test
    void reject_unknownProof_throwsTheThrown404() {
        cleanTables();

        assertThatThrownBy(() -> paymentProofService.reject(9999L))
                .isInstanceOf(ProofNotFoundException.class);
    }
}
